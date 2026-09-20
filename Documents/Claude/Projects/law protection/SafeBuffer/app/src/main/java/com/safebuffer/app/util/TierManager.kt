package com.safebuffer.app.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 사용자 티어(Free / Premium) 판별.
 *
 * Free    — 잠긴 증거 최대 3개, 롤링 버퍼 24시간, STT 사용 불가, 서버 타임스탬프 없음
 * Premium — 잠긴 증거 무제한,   롤링 버퍼 48시간, STT는 크레딧(시간)으로만, 서버 타임스탬프 포함
 *
 * ★ STT는 '월 무료 횟수' 없이 전적으로 크레딧(시간)으로 동작한다.
 *   정식 버전(5,500원) 구매 시 맛보기 1시간을 지급하고, 이후 5시간(12,900원) 크레딧을 산다.
 *   그래서 월 한도는 0으로 두어 항상 크레딧에서만 차감되게 한다.
 */
enum class Tier { FREE, PREMIUM }

@Singleton
class TierManager @Inject constructor(
    private val billingManager: BillingManager
) {
    companion object {
        const val FREE_MAX_LOCKED     = 3      // 무료: 잠긴 증거 3개까지
        const val FREE_BUFFER_HOURS   = 24L    // 롤링 버퍼 보관 시간
        const val PAID_BUFFER_HOURS   = 48L
        const val FREE_STT_MONTHLY    = 0      // 월 무료 횟수 없음 — STT는 크레딧 전용
        const val PREMIUM_STT_MONTHLY = 0      // 월 무료 횟수 없음 — STT는 크레딧 전용
    }

    fun currentTier(): Tier =
        if (billingManager.isPremium.value) Tier.PREMIUM else Tier.FREE

    fun isPremium(): Boolean = billingManager.isPremium.value

    fun bufferHours(): Long =
        if (isPremium()) PAID_BUFFER_HOURS else FREE_BUFFER_HOURS

    fun maxLockedEvidence(): Int =
        if (isPremium()) Int.MAX_VALUE else FREE_MAX_LOCKED

    fun maxSttMonthly(): Int =
        if (isPremium()) PREMIUM_STT_MONTHLY else FREE_STT_MONTHLY

    /** 서버 SHA-256 타임스탬프 발급 가능 여부 */
    fun canStamp(): Boolean = isPremium()
}
