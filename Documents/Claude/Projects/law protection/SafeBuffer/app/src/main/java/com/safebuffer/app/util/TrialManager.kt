package com.safebuffer.app.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 3일 무료 체험 관리
 *
 * - 첫 실행 시 타임스탬프 저장
 * - 남은 일수 계산 (소수점 올림)
 * - 만료 여부 확인
 */
@Singleton
class TrialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "safebuffer_trial"
        private const val KEY_FIRST_LAUNCH = "first_launch_ms"
        private const val TRIAL_DAYS = 3L
        private val TRIAL_MS = TRIAL_DAYS * 24 * 60 * 60 * 1000L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 첫 실행 시 호출 — 이미 저장된 경우 무시 */
    fun initIfNeeded() {
        if (prefs.getLong(KEY_FIRST_LAUNCH, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
    }

    /** 체험 만료 여부 */
    fun isExpired(): Boolean {
        val first = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
        if (first == 0L) return false  // 초기화 전 — 만료 아님
        return System.currentTimeMillis() - first > TRIAL_MS
    }

    /** 남은 일수 (만료 시 0) */
    fun daysRemaining(): Int {
        val first = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
        if (first == 0L) return TRIAL_DAYS.toInt()
        val elapsed = System.currentTimeMillis() - first
        val remaining = TRIAL_MS - elapsed
        return if (remaining <= 0) 0 else ((remaining / (24 * 60 * 60 * 1000.0)) + 0.99).toInt()
            .coerceAtMost(TRIAL_DAYS.toInt())
    }

    /** 첫 실행일 ms */
    fun firstLaunchMs(): Long = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
}
