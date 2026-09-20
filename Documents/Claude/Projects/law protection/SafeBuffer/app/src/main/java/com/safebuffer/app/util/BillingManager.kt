package com.safebuffer.app.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Play 인앱 결제 관리자 (Billing Library 8.0.0)
 *
 * 상품 (모두 인앱 일회성):
 *   safebuffer_unlock — 정식 버전 영구 잠금해제 (₩5,500, 비소모성)
 *                       구매 시 Premium 활성화 + STT 맛보기 1시간 지급.
 *   stt_credit_10h    — AI 대화 기록 5시간 크레딧 (₩12,900, 소모성)
 *                       ※ 상품 ID는 레거시 이름 유지, 내용은 5시간.
 *
 * ★ 구독 모델(월/연)은 폐기했다. "한 번 결제하면 영구"가 확정 정책이다.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PRODUCT_UNLOCK          = "safebuffer_unlock"
        const val PRODUCT_STT_CREDIT_10H  = "stt_credit_10h"
        private const val TAG = "BillingManager"
        private const val PREFS_NAME    = "safebuffer_billing"
        private const val KEY_IS_PREMIUM = "is_premium"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ★ 마지막으로 확인된 구매 상태를 영속화해서 초기값으로 사용한다.
    //   WorkManager 가 백그라운드에서 프로세스를 깨울 때는 connect() 가 호출되지 않으므로
    //   메모리 기본값(false)만 쓰면 정식 사용자가 Free 로 오판정돼
    //   버퍼 보관 시간이 48h → 24h 로 줄어 녹음이 조기 삭제된다.
    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_IS_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium

    /** 구매 상태 변경 — 메모리 + 디스크 동시 반영 */
    private fun setPremium(value: Boolean) {
        _isPremium.value = value
        prefs.edit().putBoolean(KEY_IS_PREMIUM, value).apply()
    }

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing

    /** 일회성 상품 구매 완료 이벤트 — 상품 ID를 emit */
    private val _oneTimePurchaseEvent = MutableSharedFlow<String>()
    val oneTimePurchaseEvent: SharedFlow<String> = _oneTimePurchaseEvent

    private var unlockDetails: ProductDetails? = null
    private var sttCredit10hDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "사용자 구매 취소")
                _isPurchasing.value = false
            }
            else -> {
                Log.w(TAG, "결제 오류: ${billingResult.debugMessage}")
                _isPurchasing.value = false
            }
        }
    }

    val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() {
        if (billingClient.isReady) {
            scope.launch { queryExistingPurchases() }
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryInAppProducts()
                        queryExistingPurchases()
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing 연결 끊김")
            }
        })
    }

    // ── 상품 조회 ─────────────────────────────────────────────

    private suspend fun queryInAppProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_UNLOCK)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_STT_CREDIT_10H)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )).build()

        suspendCancellableCoroutine { cont ->
            val listener = ProductDetailsResponseListener { br: BillingResult, qr: QueryProductDetailsResult ->
                if (br.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (detail in qr.productDetailsList) {
                        when (detail.productId) {
                            PRODUCT_UNLOCK         -> unlockDetails = detail
                            PRODUCT_STT_CREDIT_10H -> sttCredit10hDetails = detail
                        }
                    }
                    Log.d(TAG, "상품 조회 완료: unlock=${unlockDetails != null}, credit=${sttCredit10hDetails != null}")
                }
                if (cont.isActive) cont.resume(Unit)
            }
            billingClient.queryProductDetailsAsync(params, listener)
        }
    }

    // ── 기존 구매 복원 ────────────────────────────────────────

    suspend fun queryExistingPurchases() {
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        suspendCancellableCoroutine { cont ->
            val listener = PurchasesResponseListener { br: BillingResult, purchases: List<Purchase> ->
                if (br.responseCode == BillingClient.BillingResponseCode.OK) {
                    // 정식 버전(unlock) 보유 여부로 Premium 판정
                    val hasUnlock = purchases.any { p ->
                        p.products.contains(PRODUCT_UNLOCK) &&
                        p.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    setPremium(hasUnlock)
                    Log.d(TAG, "구매 복원 — 정식 버전=$hasUnlock")
                    // 미완료(미승인/미소비) 구매 처리
                    purchases.forEach { handlePurchase(it) }
                }
                if (cont.isActive) cont.resume(Unit)
            }
            billingClient.queryPurchasesAsync(inappParams, listener)
        }
    }

    // ── 구매 플로우 ───────────────────────────────────────────

    /** 정식 버전(영구 잠금해제, ₩5,500) 구매 */
    fun launchUnlockPurchase(activity: Activity): Boolean {
        if (!billingClient.isReady) { connect(); return false }
        val details = unlockDetails ?: run { connect(); return false }

        // ★ Billing 8: 일회성 상품도 구매 옵션(offer)이 있어 offerToken 을 넣어야 결제가 실행된다.
        val offerToken = oneTimeOfferToken(details) ?: run { connect(); return false }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        _isPurchasing.value = true
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _isPurchasing.value = false
            return false
        }
        return true
    }

    /** AI 대화 기록 5시간 크레딧(₩12,900) 구매 — 정식 버전 구매자만 가능 */
    fun launchSttCreditPurchase(activity: Activity): Boolean {
        if (!isPremium.value) return false          // 정식 버전 구매자만 크레딧 구매 가능
        if (!billingClient.isReady) { connect(); return false }
        val details = sttCredit10hDetails ?: run { connect(); return false }

        val offerToken = oneTimeOfferToken(details) ?: run { connect(); return false }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        _isPurchasing.value = true
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _isPurchasing.value = false
            return false
        }
        return true
    }

    // ── 구매 처리 ─────────────────────────────────────────────

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        _isPurchasing.value = false

        val isUnlock    = purchase.products.contains(PRODUCT_UNLOCK)
        val isSttCredit = purchase.products.contains(PRODUCT_STT_CREDIT_10H)

        when {
            isUnlock -> {
                setPremium(true)
                Log.d(TAG, "정식 버전 구매 완료")
                // 비소모성 — acknowledge 만 (consume 하지 않는다: 소비하면 소유가 사라진다)
                if (!purchase.isAcknowledged) {
                    scope.launch { acknowledgePurchase(purchase) }
                }
                scope.launch { _oneTimePurchaseEvent.emit(PRODUCT_UNLOCK) }
            }
            isSttCredit -> {
                Log.d(TAG, "STT 크레딧 구매 완료")
                scope.launch {
                    _oneTimePurchaseEvent.emit(PRODUCT_STT_CREDIT_10H)
                    // 소모성 상품은 consume 해야 재구매 가능
                    consumePurchase(purchase)
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val listener = AcknowledgePurchaseResponseListener { br: BillingResult ->
            Log.d(TAG, "Acknowledge: ${br.responseCode}")
        }
        billingClient.acknowledgePurchase(ackParams, listener)
    }

    private fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val listener = ConsumeResponseListener { br: BillingResult, _ ->
            Log.d(TAG, "Consume: ${br.responseCode}")
        }
        billingClient.consumeAsync(consumeParams, listener)
    }

    // ── 가격 / 오퍼 토큰 ───────────────────────────────────────

    /**
     * Billing 8 일회성 상품의 구매 옵션(offer) 토큰.
     * 단일 "Buy" 옵션이라도 목록의 첫 오퍼 토큰을 써야 결제가 실행된다.
     */
    private fun oneTimeOfferToken(details: ProductDetails): String? =
        details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken

    private fun oneTimePrice(details: ProductDetails?): String? =
        details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            ?: details?.oneTimePurchaseOfferDetails?.formattedPrice

    fun getUnlockPrice(): String = oneTimePrice(unlockDetails) ?: "₩5,500"

    fun getSttCreditPrice(): String = oneTimePrice(sttCredit10hDetails) ?: "₩12,900"
}
