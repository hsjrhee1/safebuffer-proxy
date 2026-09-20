package com.safebuffer.app.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * STT 사용 한도 관리 — 전적으로 '크레딧(시간)' 기반.
 *
 * · 무료 월 횟수 없음 (TierManager 의 월 한도는 0).
 * · 정식 버전(5,500원) 구매 시 맛보기 1시간을 1회 지급한다.
 * · 추가로 5시간(12,900원) 크레딧을 구매하면 시간이 더해진다.
 * · 전사 1회는 오디오 구간 길이(ms)만큼 크레딧에서 차감한다.
 *
 * 정식 버전을 사지 않은(=Premium 아님) 사용자는 크레딧이 0이라 STT를 쓸 수 없다.
 */
@Singleton
class SttQuotaManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tierManager: TierManager
) {
    companion object {
        private const val PREFS_NAME       = "safebuffer_stt_quota"
        private const val KEY_COUNT        = "stt_count"
        private const val KEY_YEAR         = "quota_year"
        private const val KEY_MONTH        = "quota_month"
        private const val KEY_PURCHASED_MS = "purchased_ms"
        private const val KEY_WELCOME_GRANTED = "welcome_granted"
        const val HOURS_PER_CREDIT_PACK    = 5      // 12,900원 = 5시간
        const val WELCOME_HOURS            = 1      // 정식 버전 구매 시 맛보기 1시간
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 정식 버전(Premium) 구매자에게 맛보기 시간을 딱 한 번 지급한다.
     * STT 가용 여부를 볼 때마다 호출해도 플래그로 1회만 지급된다.
     */
    private fun grantWelcomeIfNeeded() {
        if (!tierManager.isPremium()) return
        if (prefs.getBoolean(KEY_WELCOME_GRANTED, false)) return
        prefs.edit()
            .putBoolean(KEY_WELCOME_GRANTED, true)
            .putLong(KEY_PURCHASED_MS,
                prefs.getLong(KEY_PURCHASED_MS, 0L) + WELCOME_HOURS * 3_600_000L)
            .apply()
    }

    // ── 월 횟수 한도 ─────────────────────────────────────────

    /** 이번 달 남은 월 할당 횟수 */
    fun remaining(): Int {
        resetIfNewMonth()
        return (tierManager.maxSttMonthly() - prefs.getInt(KEY_COUNT, 0)).coerceAtLeast(0)
    }

    /** STT 사용 가능 여부 — 크레딧(맛보기 포함)이 남아있으면 true (월 할당은 0) */
    fun canUse(): Boolean {
        grantWelcomeIfNeeded()
        return remaining() > 0 || remainingPurchasedMs() > 0
    }

    /**
     * 전사 1회 소비 (월 한도 → 구매 크레딧 순서로 차감)
     * @param durationMs 오디오 구간 길이 — 구매 크레딧 사용 시에만 차감
     * @return false = 잔여 없음
     */
    fun consume(durationMs: Long = 0L): Boolean {
        resetIfNewMonth()
        val used  = prefs.getInt(KEY_COUNT, 0)
        val limit = tierManager.maxSttMonthly()

        return if (used < limit) {
            // 월 할당 차감
            prefs.edit().putInt(KEY_COUNT, used + 1).apply()
            true
        } else {
            // 구매 크레딧 차감
            consumePurchasedMs(durationMs)
        }
    }

    /**
     * 차감 취소 — 전사가 실패했을 때 되돌린다.
     *
     * consume() 은 남용 방지를 위해 요청 직전에 차감하지만, 서버 오류·타임아웃처럼
     * 사용자 잘못이 아닌 실패까지 차감하면 월 3회 한도에서 한 번의 콜드스타트로
     * 3분의 1이 사라진다. 실패 시 반드시 이 메서드로 복구한다.
     *
     * @param durationMs consume() 에 넘겼던 값과 동일해야 한다
     * @param usedMonthly consume() 이 월 할당을 썼는지 여부
     */
    fun refund(durationMs: Long, usedMonthly: Boolean) {
        if (usedMonthly) {
            val used = prefs.getInt(KEY_COUNT, 0)
            prefs.edit().putInt(KEY_COUNT, (used - 1).coerceAtLeast(0)).apply()
        } else {
            val current = prefs.getLong(KEY_PURCHASED_MS, 0L)
            prefs.edit().putLong(KEY_PURCHASED_MS, current + durationMs).apply()
        }
    }

    /** consume() 이 월 할당에서 차감할 상태인지 — refund 시 어느 쪽을 되돌릴지 판단용 */
    fun willUseMonthly(): Boolean {
        resetIfNewMonth()
        return prefs.getInt(KEY_COUNT, 0) < tierManager.maxSttMonthly()
    }

    fun usedThisMonth(): Int {
        resetIfNewMonth()
        return prefs.getInt(KEY_COUNT, 0)
    }

    fun monthlyLimit(): Int = tierManager.maxSttMonthly()

    // ── 구매 크레딧 ──────────────────────────────────────────

    fun remainingPurchasedMs(): Long {
        grantWelcomeIfNeeded()
        return prefs.getLong(KEY_PURCHASED_MS, 0L).coerceAtLeast(0L)
    }

    /** 남은 구매 크레딧을 "X시간 Y분" 형태로 반환 */
    fun remainingPurchasedText(): String {
        val ms = remainingPurchasedMs()
        if (ms <= 0L) return "없음"
        val totalMin = ms / 60_000L
        val hours    = totalMin / 60
        val minutes  = totalMin % 60
        return if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
    }

    /** stt_credit_10h 구매 완료 시 호출 */
    fun addPurchasedHours(hours: Int) {
        val current = prefs.getLong(KEY_PURCHASED_MS, 0L)
        prefs.edit().putLong(KEY_PURCHASED_MS, current + hours * 3_600_000L).apply()
    }

    private fun consumePurchasedMs(durationMs: Long): Boolean {
        val current = prefs.getLong(KEY_PURCHASED_MS, 0L)
        if (current <= 0L) return false
        // 잔여보다 요청이 커도 모두 소모 후 허용 (마지막 구간 활용)
        prefs.edit().putLong(KEY_PURCHASED_MS, (current - durationMs).coerceAtLeast(0L)).apply()
        return true
    }

    // ── 공통 ─────────────────────────────────────────────────

    private fun resetIfNewMonth() {
        val now   = Calendar.getInstance()
        val year  = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)

        if (year != prefs.getInt(KEY_YEAR, -1) || month != prefs.getInt(KEY_MONTH, -1)) {
            prefs.edit()
                .putInt(KEY_COUNT, 0)
                .putInt(KEY_YEAR,  year)
                .putInt(KEY_MONTH, month)
                .apply()
        }
    }
}
