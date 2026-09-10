package com.safebuffer.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.safebuffer.app.data.repository.AudioChunkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager 주기 작업 — 매 1시간마다 실행
 *
 * 48시간(RETENTION_HOURS) 초과 & 잠금 안 된 청크를
 * 파일 + DB에서 모두 삭제한다.
 *
 * [등록 방법] — App 시작 시 한 번만 호출
 *   CleanupWorker.schedule(context)
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AudioChunkRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val cutoff = System.currentTimeMillis() - RETENTION_MS
            val deletedCount = repository.cleanupExpired(cutoff)
            android.util.Log.d("CleanupWorker", "삭제된 청크: $deletedCount 개")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("CleanupWorker", "청크 삭제 실패", e)
            Result.retry()
        }
    }

    companion object {
        // 48시간 보관
        private const val RETENTION_HOURS = 48L
        private val RETENTION_MS = TimeUnit.HOURS.toMillis(RETENTION_HOURS)

        private const val WORK_NAME = "safebuffer_cleanup"

        /**
         * 앱 시작 시 1회 등록 (이미 등록된 경우 무시)
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)  // 배터리 낮아도 실행
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // 이미 있으면 유지
                request
            )
        }
    }
}
