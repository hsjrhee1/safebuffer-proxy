package com.safebuffer.app.domain.usecase

import com.safebuffer.app.data.repository.AudioChunkRepository
import com.safebuffer.app.util.TierManager
import javax.inject.Inject

/**
 * 유저가 "지금부터 N시간 전 구간 잠금" 버튼을 눌렀을 때 실행
 *
 * Free:    잠긴 증거 최대 3개 제한. 초과 시 FreeLimitException
 * Premium: 잠금 후 서버 SHA-256 타임스탬프 발급
 */
class LockChunksUseCase @Inject constructor(
    private val repository: AudioChunkRepository,
    private val tierManager: TierManager
) {
    sealed class LockError {
        data class FreeLimitReached(val max: Int) : LockError()
    }

    suspend operator fun invoke(hoursBack: Float): Result<LockResult> {
        val nowMs  = System.currentTimeMillis()
        val fromMs = nowMs - (hoursBack * 60 * 60 * 1000).toLong()

        // Free 잠금 한도 체크 — 잠금 전에 실제로 잠길 청크 수를 미리 계산
        val maxLocked = tierManager.maxLockedEvidence()
        if (maxLocked != Int.MAX_VALUE) {
            val currentLocked = repository.countLocked()
            val toBeLockedCount = repository.countUnlockedInRange(fromMs, nowMs)
            if (currentLocked + toBeLockedCount > maxLocked) {
                return Result.failure(
                    FreeLimitException(LockError.FreeLimitReached(maxLocked))
                )
            }
        }

        repository.lockRange(fromMs = fromMs, toMs = nowMs)

        // Premium: 서버 타임스탬프 발급 / Free: 로컬 SHA-256만
        if (tierManager.canStamp()) {
            try {
                repository.stampChunksInRange(fromMs = fromMs, toMs = nowMs)
            } catch (e: Exception) {
                android.util.Log.w("LockChunks", "stamp 실패: ${e.message}")
            }
        } else {
            try {
                repository.hashChunksInRange(fromMs = fromMs, toMs = nowMs)
            } catch (e: Exception) {
                android.util.Log.w("LockChunks", "hash 실패: ${e.message}")
            }
        }

        return Result.success(LockResult(fromMs = fromMs, toMs = nowMs, hoursBack = hoursBack))
    }

    /**
     * TranscriptActivity에서 ms 구간을 직접 지정해 잠글 때 사용.
     * Free 한도 체크 포함.
     */
    suspend fun invokeWithRange(fromMs: Long, toMs: Long): Result<LockResult> {
        val maxLocked = tierManager.maxLockedEvidence()
        if (maxLocked != Int.MAX_VALUE) {
            val currentLocked = repository.countLocked()
            val toBeLockedCount = repository.countUnlockedInRange(fromMs, toMs)
            if (currentLocked + toBeLockedCount > maxLocked) {
                return Result.failure(
                    FreeLimitException(LockError.FreeLimitReached(maxLocked))
                )
            }
        }

        repository.lockRange(fromMs = fromMs, toMs = toMs)

        if (tierManager.canStamp()) {
            try { repository.stampChunksInRange(fromMs, toMs) }
            catch (e: Exception) { android.util.Log.w("LockChunks", "stamp 실패: ${e.message}") }
        } else {
            try { repository.hashChunksInRange(fromMs, toMs) }
            catch (e: Exception) { android.util.Log.w("LockChunks", "hash 실패: ${e.message}") }
        }

        return Result.success(LockResult(fromMs = fromMs, toMs = toMs, hoursBack = 0f))
    }
}

data class LockResult(val fromMs: Long, val toMs: Long, val hoursBack: Float)

class FreeLimitException(val error: LockChunksUseCase.LockError) : Exception(
    when (error) {
        is LockChunksUseCase.LockError.FreeLimitReached ->
            "무료 버전은 최대 ${error.max}개의 증거만 잠글 수 있습니다."
    }
)
