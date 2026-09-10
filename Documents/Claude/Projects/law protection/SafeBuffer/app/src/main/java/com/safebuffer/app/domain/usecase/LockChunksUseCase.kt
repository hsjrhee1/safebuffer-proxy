package com.safebuffer.app.domain.usecase

import com.safebuffer.app.data.repository.AudioChunkRepository
import javax.inject.Inject

/**
 * 유저가 "지금부터 N시간 전 구간 잠금" 버튼을 눌렀을 때 실행
 *
 * @param hoursBack 몇 시간 전부터 잠글지 (예: 2.5 = 2시간 30분 전 ~ 지금)
 */
class LockChunksUseCase @Inject constructor(
    private val repository: AudioChunkRepository
) {
    suspend operator fun invoke(hoursBack: Float): LockResult {
        val nowMs = System.currentTimeMillis()
        val fromMs = nowMs - (hoursBack * 60 * 60 * 1000).toLong()

        repository.lockRange(fromMs = fromMs, toMs = nowMs)

        // 원본 증명: SHA-256 해시 계산 + 서버 타임스탬프 발급
        // 실패해도 잠금은 유지되므로 예외를 삼킴
        try {
            repository.stampChunksInRange(fromMs = fromMs, toMs = nowMs)
        } catch (e: Exception) {
            android.util.Log.w("LockChunks", "stamp 실패: ${e.message}")
        }

        return LockResult(
            fromMs = fromMs,
            toMs = nowMs,
            hoursBack = hoursBack
        )
    }
}

data class LockResult(
    val fromMs: Long,
    val toMs: Long,
    val hoursBack: Float
)
