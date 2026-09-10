package com.safebuffer.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.*
import androidx.core.app.NotificationCompat
import com.safebuffer.app.R
import com.safebuffer.app.data.local.ChunkEntity
import com.safebuffer.app.data.repository.AudioChunkRepository
import com.safebuffer.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * SafeBuffer 핵심 서비스
 *
 * [동작 방식]
 * 1. startForeground()로 상단 알림 표시 (사용자가 알 수 있도록)
 * 2. MediaRecorder로 AAC 16kbps M4A 파일 녹음
 * 3. CHUNK_DURATION_MS(10분)마다 자동으로 파일 회전 (stop → new file → start)
 * 4. 각 청크를 Room DB에 메타데이터 저장
 * 5. 서비스 종료 시 현재 청크 안전하게 마무리
 *
 * [배터리 최적화]
 * - AAC 16kbps: CPU 사용 거의 없음
 * - HDD 쓰기: 10분마다 소량 (약 1.2 MB/10분)
 * - 마이크 전력이 유일한 소모원
 */
@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var repository: AudioChunkRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var mediaRecorder: MediaRecorder? = null
    private var currentChunkStartTime: Long = 0L
    private var currentFile: File? = null
    private var chunkRotationJob: Job? = null

    // 10분 청크 (밀리초)
    private val CHUNK_DURATION_MS = 10 * 60 * 1000L

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "safebuffer_recording"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_FLUSH = "ACTION_FLUSH"  // 현재 청크 즉시 저장

        // getRunningServices deprecated 대체용 플래그
        @Volatile var isRunning = false
            private set

        fun startIntent(context: Context) =
            Intent(context, RecordingService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, RecordingService::class.java).apply { action = ACTION_STOP }

        fun flushIntent(context: Context) =
            Intent(context, RecordingService::class.java).apply { action = ACTION_FLUSH }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
            ACTION_FLUSH -> rotateChunk()  // 현재 청크 즉시 저장 후 새 청크 시작
        }
        return START_STICKY // 시스템이 죽여도 자동 재시작
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecording()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ──────────────────────────────────────────────────────────
    // 녹음 시작
    // ──────────────────────────────────────────────────────────

    private fun startRecording() {
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())
        startNewChunk()
        scheduleChunkRotation()
    }

    /**
     * 새 청크 파일 생성 및 MediaRecorder 시작
     */
    private fun startNewChunk() {
        currentChunkStartTime = System.currentTimeMillis()
        val file = createChunkFile()
        currentFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(32_000)   // 32kbps — 음질 개선, 10분 = ~2.4 MB
            setAudioSamplingRate(16_000)       // 16kHz — 음성 최적
            setAudioChannels(1)                // 모노
            setOutputFile(file.absolutePath)

            prepare()
            start()
        }
    }

    /**
     * 10분마다 청크 회전: 현재 파일 저장 → 새 파일 시작
     */
    private fun scheduleChunkRotation() {
        chunkRotationJob?.cancel()
        chunkRotationJob = serviceScope.launch {
            while (isActive) {
                delay(CHUNK_DURATION_MS)
                rotateChunk()
            }
        }
    }

    private fun rotateChunk() {
        val endTime = System.currentTimeMillis()
        val file = currentFile ?: return  // 녹음 중이 아니면 무시
        // ★ startNewChunk()가 currentChunkStartTime을 덮어쓰기 전에 반드시 캡처
        val savedStartTime = currentChunkStartTime

        // 현재 MediaRecorder 마무리
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // 짧은 청크 등 예외 무시
        }
        mediaRecorder = null

        // DB에 청크 메타데이터 저장 (로컬 변수로 캡처한 값 사용)
        if (file.exists() && file.length() > 0) {
            serviceScope.launch {
                repository.saveChunk(
                    ChunkEntity(
                        startTimeMs = savedStartTime,
                        endTimeMs = endTime,
                        filePath = file.absolutePath
                    )
                )
            }
        }

        // 새 청크 즉시 시작
        startNewChunk()
    }

    // ──────────────────────────────────────────────────────────
    // 녹음 정지
    // ──────────────────────────────────────────────────────────

    private fun stopRecording() {
        isRunning = false
        chunkRotationJob?.cancel()

        val endTime = System.currentTimeMillis()
        val file = currentFile

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) { /* 무시 */ }
        mediaRecorder = null

        // 마지막 청크도 DB에 저장
        if (file != null && file.exists() && file.length() > 0) {
            serviceScope.launch {
                repository.saveChunk(
                    ChunkEntity(
                        startTimeMs = currentChunkStartTime,
                        endTimeMs = endTime,
                        filePath = file.absolutePath
                    )
                )
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ──────────────────────────────────────────────────────────
    // 파일 / 알림 헬퍼
    // ──────────────────────────────────────────────────────────

    /**
     * 앱 내부 저장소에 청크 파일 생성
     * 외부에서 접근 불가 (보안)
     */
    private fun createChunkFile(): File {
        val dir = File(filesDir, "chunks").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return File(dir, "chunk_$timestamp.m4a")
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔴 SafeBuffer 작동 중")
            .setContentText("48시간 후 자동 삭제됩니다. 사건 발생 시 앱을 열어 구간을 잠그세요.")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.ic_stop, "중지", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SafeBuffer 녹음",
            NotificationManager.IMPORTANCE_LOW   // IMPORTANCE_LOW = 소리 없음, 상단 고정
        ).apply {
            description = "백그라운드 안전 버퍼 녹음 상태"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
