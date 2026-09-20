package com.safebuffer.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.safebuffer.app.data.repository.AudioChunkRepository
import com.safebuffer.app.util.TierManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager 주기 작업 — 매 1시간마다 실행
 *
 * 티어별 롤링 버퍼 보관 시간:
 *   Free    → 24시간
 *   Pro     → 48시간
 *   Premium → 48시간
 *
 * 보관 기간 초과 & 잠금 안 된 청크를 파일 + DB에서 모두 삭제한다.
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AudioChunkRepository,
    private val tierManager: TierManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val retentionMs = TimeUnit.HOURS.toMillis(tierManager.bufferHours())
            val cutoff = System.currentTimeMillis() - retentionMs
            val deletedCount = repository.cleanupExpired(cutoff)
            android.util.Log.d(TAG,
                "삭제된 청크: $deletedCount 개 (보관: ${tierManager.bufferHours()}h)")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "청크 삭제 실패", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CleanupWorker"
        private const val WORK_NAME = "safebuffer_cleanup"

        /**
         * 앱 시작 시 1회 등록 (이미 등록된 경우 유지)
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
