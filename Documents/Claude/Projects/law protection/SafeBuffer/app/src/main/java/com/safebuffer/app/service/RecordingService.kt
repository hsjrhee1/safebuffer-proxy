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
 * 2. MediaRecorder로 AAC 44.1kHz/96kbps M4A 파일 녹음 (미지원 기기는 자동 폴백)
 * 3. CHUNK_DURATION_MS(10분)마다 자동으로 파일 회전 (stop → new file → start)
 * 4. 각 청크를 Room DB에 메타데이터 저장
 * 5. 서비스 종료 시 현재 청크 안전하게 마무리
 *
 * [배터리 최적화]
 * - AAC 96kbps: CPU 사용 거의 없음
 * - HDD 쓰기: 10분마다 소량 (약 1.2 MB/10분)
 * - 마이크 전력이 유일한 소모원
 */
@AndroidEntryPoint
class RecordingService : Service() {

    private val traceTag = "SafeBufferTrace"

    @Inject
    lateinit var repository: AudioChunkRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var mediaRecorder: MediaRecorder? = null
    private var currentChunkStartTime: Long = 0L
    private var currentFile: File? = null
    private var chunkRotationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var shouldRecord = false
    private var resetStopRequested = false
    private var resetStopReady = false

    /** MediaRecorder 콜백은 임의 스레드에서 오므로 Main 으로 넘기기 위한 핸들러 */
    private val mainHandler = Handler(Looper.getMainLooper())

    // 10분 청크 (밀리초) — MediaRecorder 안정성 최우선
    private val CHUNK_DURATION_MS = 10 * 60 * 1000L

    // ★ 헬스체크 주기. 10분을 한 번에 delay 하면 Doze 로 타이머가 밀렸을 때 회복이 늦고,
    //   그 사이 녹음이 죽어도 감지할 방법이 없다. 30초마다 깨어나 상태를 점검한다.
    private val HEALTH_CHECK_MS = 30_000L

    // 청크 재시작 최소 간격 — 마이크를 오래 뺏긴 상황에서 무한 재시작 루프 방지
    private val RESTART_COOLDOWN_MS = 15_000L
    private var lastRestartMs = 0L

    // ★ 저장(flush)이 겹쳐 들어오는 것을 막는다.
    //   미리듣기(ACTION_FLUSH)와 저장(ACTION_FLUSH_AND_LOCK)이 연달아 오거나,
    //   Activity 재생성으로 같은 인텐트가 두 번 전달되면, 각 요청이 현재 녹음 파일을
    //   저장소에 한 번씩 넣어 같은 구간이 두 개 행으로 복제됐다.
    //   (테스터가 본 "11초짜리 + 3분45초 앞부분 복제"의 원인)
    //   진행 중이면 두 번째 요청은 저장을 반복하지 않고 잠금 범위만 반영한다.
    @Volatile private var flushInProgress = false

    // ★ 같은 잠금 요청이 3초 안에 반복되면 무시 (Activity 재생성 등)
    private var lastLockFrom = 0L
    private var lastLockTo = 0L
    private var lastLockAtMs = 0L

    // MediaRecorder.stop() 은 기록된 데이터가 너무 적으면 예외를 던진다.
    // 기존 3초는 안드로이드가 정한 값이 아니라 임의로 잡은 과도한 값이었다.
    // 대부분 기기는 1초 미만이면 충분하므로 1.5초로 낮춘다.
    // (이 조건에 걸려도 이제는 버리지 않고 기다렸다가 저장한다)
    private val MIN_CHUNK_MS = 1_500L

    // 짧은 청크 대기-재시도 상한 (무한 재귀 방지)
    private val MAX_SHORT_CHUNK_RETRY = 3

    // ── 48시간 타임라인용 진폭 수집 ──────────────────────────────
    // 파일을 디코딩하지 않고 녹음 중에 모은다. getMaxAmplitude() 는 값만 읽으므로 거의 공짜.
    private val AMP_SAMPLE_MS = 10_000L          // 10초마다 한 점 (10분 청크 = 60점)
    private val currentAmplitudes = mutableListOf<Int>()
    private var ampSamplerJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "safebuffer_recording"
        const val ACTION_START          = "ACTION_START"
        const val ACTION_STOP           = "ACTION_STOP"
        const val ACTION_RESET_STOP     = "ACTION_RESET_STOP"
        const val ACTION_LOCK_RANGE     = "ACTION_LOCK_RANGE"
        const val ACTION_FLUSH          = "ACTION_FLUSH"           // STT용: 현재 청크 저장 후 새 청크 시작
        const val ACTION_FLUSH_AND_LOCK = "ACTION_FLUSH_AND_LOCK"  // 잠금용: 저장 시 isLocked=true

        const val EXTRA_LOCK_FROM_MS = "lock_from_ms"
        const val EXTRA_LOCK_TO_MS   = "lock_to_ms"
        const val EXTRA_REQUEST_ID   = "request_id"

        const val PREFS_NAME = "safebuffer_prefs"
        const val KEY_RECORDING_START_MS = "recording_start_ms"

        // ★ 한 번 성공한 (오디오 소스, 샘플레이트) 조합을 기억한다.
        //   매번 폴백을 처음부터 돌리면 청크마다 인코딩이 달라질 수 있고,
        //   그러면 재인코딩 없이 이어붙일 수 없다. 첫 성공 조합을 계속 쓴다.
        const val KEY_AUDIO_SOURCE = "audio_source"
        const val KEY_SAMPLE_RATE  = "sample_rate"

        @Volatile var isRunning = false
            private set

        /** 실제 MediaRecorder 녹음 상태를 UI에 즉시 전달한다. */
        val recordingState = kotlinx.coroutines.flow.MutableStateFlow(false)

        /** 서비스 인스턴스가 존재하는지 여부. STARTING 상태의 중복 시작 방지에 사용한다. */
        @Volatile var isServiceActive = false
            private set

        /** 초기화 전용 종료가 recorder 해제와 기존 DB 작업 정리까지 마친 횟수. */
        val resetStopCounter = kotlinx.coroutines.flow.MutableStateFlow(0L)

        /**
         * flush(현재 청크 즉시 저장) 가 실제로 끝난 시각.
         *
         * UI 가 고정 delay 로 추측하면 기기 속도에 따라 DB 반영 전에 조회하게 된다.
         * (MediaRecorder.stop() 은 기기·인코더에 따라 수백 ms ~ 수 초)
         * → "빠른 폰은 3분 구간이 보이고 느린 폰은 10분을 골라야 보이는" 증상의 원인.
         * 실제 완료 시각을 게시해 UI 가 정확히 기다릴 수 있게 한다.
         */
        // ★ 완료 신호는 시각이 아니라 단조 증가 카운터로 준다.
        //   시각(StateFlow<Long>)으로 하면 첫 저장 값이 남아 있어 두 번째 대기가
        //   옛 값/새 값 판정에서 꼬였다. 카운터는 "요청 전 값보다 커지면 완료"로
        //   명확히 구분된다.
        val flushCounter = kotlinx.coroutines.flow.MutableStateFlow(0L)

        private fun signalFlushDone() {
            flushCounter.value = flushCounter.value + 1
        }

        fun startIntent(context: Context) =
            Intent(context, RecordingService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, RecordingService::class.java).apply { action = ACTION_STOP }

        fun resetStopIntent(context: Context) =
            Intent(context, RecordingService::class.java).apply { action = ACTION_RESET_STOP }

        fun flushIntent(context: Context, requestId: Long = 0L) =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_FLUSH
                putExtra(EXTRA_REQUEST_ID, requestId)
            }

        /** 현재 녹음 청크를 처음부터 isLocked=true 로 저장 — 비동기 타이밍 문제 없음 */
        fun flushAndLockIntent(context: Context, fromMs: Long, toMs: Long, requestId: Long = 0L) =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_FLUSH_AND_LOCK
                putExtra(EXTRA_REQUEST_ID, requestId)
                putExtra(EXTRA_LOCK_FROM_MS, fromMs)
                putExtra(EXTRA_LOCK_TO_MS, toMs)
            }

        fun lockRangeIntent(context: Context, fromMs: Long, toMs: Long, requestId: Long) =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_LOCK_RANGE
                putExtra(EXTRA_REQUEST_ID, requestId)
                putExtra(EXTRA_LOCK_FROM_MS, fromMs)
                putExtra(EXTRA_LOCK_TO_MS, toMs)
            }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        traceState("5 onCreate")
        createNotificationChannel()
        // ★ onCreate에서 즉시 startForeground → 5초 타이머 해결
        // startForegroundService() 호출 후 5초 내에 startForeground()가 없으면
        // 시스템이 ForegroundServiceDidNotStartInTimeException으로 서비스를 강제 종료함.
        // onStartCommand보다 onCreate가 먼저 호출되므로 여기서 선제적으로 처리.
        //
        // ★ Android 14+ 에서는 foregroundServiceType="microphone" 서비스를
        //   RECORD_AUDIO 권한 없이 startForeground 하면 SecurityException 으로 즉시 죽는다.
        //   (부팅 재시작·작업목록 스와이프 재시작 경로에서 권한이 없을 수 있음)
        //   → 권한을 먼저 확인하고, 없으면 크래시 대신 조용히 종료한다.
        if (!hasMicPermission()) {
            android.util.Log.e("RecordingService", "RECORD_AUDIO 권한 없음 — 서비스 종료")
            setActuallyRecording(false)  // MainActivity가 녹음 중으로 오인하지 않도록
            isServiceActive = false
            notifyPermissionMissing()    // 증거 앱이므로 조용히 죽지 않고 알린다
            stopSelf()
            return
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // 제조사 정책·권한 변경 등으로 실패할 수 있다. 크래시 루프보다 종료가 낫다.
            android.util.Log.e("RecordingService", "startForeground 실패: ${e.message}")
            setActuallyRecording(false)
            isServiceActive = false
            stopSelf()
        }
    }

    /** 마이크 권한이 없어 녹음을 시작할 수 없음을 사용자에게 알린다 */
    private fun notifyPermissionMissing() {
        try {
            val msg = "마이크 권한이 없어 녹음을 시작할 수 없습니다. 앱을 열어 권한을 허용해주세요."
            val n = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⚠️ SafeBuffer 녹음 중지됨")
                .setContentText(msg)
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                .setSmallIcon(R.drawable.ic_mic)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID + 1, n)
        } catch (e: Exception) {
            android.util.Log.w("RecordingService", "권한 알림 실패: ${e.message}")
        }
    }

    /**
     * 녹음 중 PARTIAL_WAKE_LOCK 유지.
     *
     * 코루틴 delay() 는 CPU 가 잠들면 타이머가 멈춘다. 화면을 끈 채 Doze 에 들어가면
     * 10분 회전이 밀리거나 사실상 정지하고, 그 상태로 서비스가 죽으면
     * MP4 가 finalize 되지 않아 그때까지의 녹음 전체가 재생 불가 파일이 된다.
     * 실제로 오디오를 녹음 중인 포그라운드 서비스이므로 wakelock 보유는 정당하다.
     */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        try {
            val pm = getSystemService(PowerManager::class.java)
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "SafeBuffer::Recording"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            android.util.Log.i("RecordingService", "WakeLock 획득")
        } catch (e: Exception) {
            android.util.Log.w("RecordingService", "WakeLock 획득 실패: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            android.util.Log.w("RecordingService", "WakeLock 해제 실패: ${e.message}")
        }
        wakeLock = null
    }

    private fun hasMicPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        traceState("5 onStartCommand action=${intent?.action} startId=$startId")
        // onCreate()에서 stopSelf() 했더라도 onStartCommand는 전달될 수 있다.
        // 권한이 없으면 START_NOT_STICKY로 재시작 루프를 끊는다.
        if (!hasMicPermission()) {
            android.util.Log.e("RecordingService", "RECORD_AUDIO 권한 없음 — 명령 무시")
            setActuallyRecording(false)
            shouldRecord = false
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP  -> stopRecording()
            ACTION_RESET_STOP -> stopForReset()
            ACTION_LOCK_RANGE -> {
                val requestId = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
                val fromMs = intent.getLongExtra(EXTRA_LOCK_FROM_MS, 0L)
                val toMs = intent.getLongExtra(EXTRA_LOCK_TO_MS, 0L)
                android.util.Log.i("SaveFlow", "LOCK_ONLY_REQUEST requestId=$requestId from=$fromMs to=$toMs")
                lockOnly(fromMs, toMs, requestId)
            }
            ACTION_FLUSH -> {
                val requestId = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
                logFlush("FLUSH_REQUEST", requestId)
                rotateChunk(requestId)
            }
            ACTION_FLUSH_AND_LOCK -> {
                val requestId = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
                val fromMs = intent.getLongExtra(EXTRA_LOCK_FROM_MS, 0L)
                val toMs   = intent.getLongExtra(EXTRA_LOCK_TO_MS, 0L)
                logFlush("FLUSH_AND_LOCK_REQUEST from=$fromMs to=$toMs", requestId)
                rotateChunkLocked(fromMs, toMs, requestId)
            }
            null -> { if (!shouldRecord) startRecording() }
        }
        return START_STICKY // 시스템이 죽여도 자동 재시작
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 삼성 등 최근 앱에서 쓸어내도 5초 후 자동 재시작
        val restart = Intent(applicationContext, RecordingService::class.java).apply {
            action = ACTION_START
        }
        val pi = android.app.PendingIntent.getService(
            applicationContext, 1, restart,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(android.app.AlarmManager::class.java)
        am.set(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // stopRecording()이 마지막 청크를 코루틴으로 저장하므로
        // 잠깐 기다린 후 scope 취소 (유실 방지)
        stopRecording()
        releaseWakeLock()   // stopRecording 이 조기 return 하는 경우 대비
        isServiceActive = false
        if (resetStopRequested && resetStopReady) {
            resetStopCounter.value = resetStopCounter.value + 1
        }
        serviceScope.launch {
            kotlinx.coroutines.delay(2000)
        }.invokeOnCompletion { serviceScope.cancel() }
        super.onDestroy()
    }

    // ──────────────────────────────────────────────────────────
    // 녹음 시작
    // ──────────────────────────────────────────────────────────

    private fun startRecording() {
        traceState("6 startRecording enter")
        // ★ 중복 방지: 이미 mediaRecorder가 살아있으면 두 번 시작하지 않음
        // onTaskRemoved 재시작 또는 여러 곳에서 ACTION_START가 오는 경우 마이크 점유 충돌 방지
        if (shouldRecord) {
            android.util.Log.w("RecordingService", "startRecording 중복 호출 — 무시")
            return
        }
        // 프로세스 강제 종료/덮어설치로 이전 서비스의 stopRecording()이 호출되지 않은 경우
        // 과거 세션 시작값이 남을 수 있다. 새 recorder가 성공하기 전에는 유효하지 않다.
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .remove(KEY_RECORDING_START_MS)
            .commit()
        shouldRecord = true
        setActuallyRecording(false)
        acquireWakeLock()
        // startForeground는 onCreate()에서 이미 호출됨 (5초 타이머 선제 처리)

        // ★ try-catch 필수: MediaRecorder.prepare()/start() 실패 시 (마이크 선점, 삼성 HAL 등)
        // 예외가 없으면 서비스 자체가 크래시 → START_STICKY 재시작 루프 → 녹음 영구 불가
        try {
            startNewChunk()
            markRecordingStarted()
            notifyRecordingState(ok = true)
        } catch (e: Exception) {
            android.util.Log.e("RecordingService", "녹음 첫 시작 실패 — 1초 후 재시도: ${e.message}")
            // ★ Main thread에서 재시도 — MediaRecorder는 Looper 스레드 필수
            serviceScope.launch(Dispatchers.Main) {
                delay(1000)
                if (!shouldRecord) return@launch  // ★ 대기 중 서비스 중지됐으면 재시도 취소
                try {
                    startNewChunk()
                    markRecordingStarted()
                    android.util.Log.i("RecordingService", "녹음 재시도 성공")
                    notifyRecordingState(ok = true)
                } catch (e2: Exception) {
                    android.util.Log.e("RecordingService", "녹음 재시도도 실패: ${e2.message}")
                    setActuallyRecording(false)
                    notifyRecordingState(ok = false)
                }
            }
        }
        scheduleChunkRotation()
    }

    /** 실제 MediaRecorder.start()가 성공한 뒤에만 외부에 녹음 중으로 공개한다. */
    private fun markRecordingStarted() {
        // UI가 recordingState=true를 받자마자 이 값을 읽으므로 반드시 먼저 저장한다.
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putLong(KEY_RECORDING_START_MS, System.currentTimeMillis())
            .commit()
        setActuallyRecording(true)
    }

    private fun setActuallyRecording(recording: Boolean) {
        isRunning = recording
        recordingState.value = recording
        traceState("8 setActuallyRecording($recording)")
    }

    private fun traceState(message: String) {
        android.util.Log.i(traceTag,
            "$message shouldRecord=$shouldRecord isRunning=$isRunning " +
                "serviceActive=$isServiceActive recorder=${mediaRecorder != null}")
    }

    /**
     * 새 청크 파일 생성 및 MediaRecorder 시작
     * ★ 기존 mediaRecorder가 있으면 먼저 release — 마이크 점유 충돌 방지
     */
    private fun startNewChunk() {
        // 혹시 이전 recorder가 남아있으면 먼저 정리
        mediaRecorder?.let { old ->
            try { old.stop() } catch (_: Exception) {}
            try { old.release() } catch (_: Exception) {}
        }
        mediaRecorder = null

        currentChunkStartTime = System.currentTimeMillis()
        val file = createChunkFile()
        currentFile = file

        // ★ 샘플레이트 — 음질 우선.
        //   예전엔 16kHz 를 1순위로 둬서 대부분 기기가 16kHz(전화 통화 수준)로 녹음돼
        //   먹먹하고 답답했다. 44.1kHz 를 1순위로 올려 사람이 듣기 좋은 음질을 낸다.
        //   44.1/48kHz 는 사실상 모든 기기가 지원하고, 미지원 기기를 위해 낮은 값으로 폴백한다.
        val sampleRates = intArrayOf(44_100, 48_000, 22_050, 16_000)

        // ★ 오디오 소스도 기기별 차이가 크다.
        //   삼성 등은 AudioSource.MIC 에 공격적인 노이즈 억제·AGC 를 적용해서
        //   조금 떨어진 곳의 대화는 거의 뭉개버린다 → STT 가 아무것도 못 잡는다.
        //   VOICE_RECOGNITION 은 그 전처리를 끄고 음성 인식에 맞춰 캡처하므로
        //   증거 녹음·STT 용도에는 이쪽이 우선이다. 미지원 기기를 위해 MIC 로 폴백한다.
        val audioSources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )

        // (소스, 샘플레이트) 조합을 우선순위대로 평탄화 — 첫 성공에서 즉시 종료
        var combos = audioSources.flatMap { src -> sampleRates.map { rate -> src to rate } }

        // ★ 이 기기에서 이미 성공한 조합이 있으면 그것을 맨 앞에 둔다.
        //   청크마다 인코딩이 달라지면 나중에 구간을 하나로 이어붙일 수 없다.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedSource = prefs.getInt(KEY_AUDIO_SOURCE, -1)
        val savedRate   = prefs.getInt(KEY_SAMPLE_RATE, -1)
        // ★ 예전 버전에서 16kHz 로 성공한 기기는 그 값이 저장돼 있다.
        //   그대로 1순위로 쓰면 음질 개선이 적용되지 않으므로, 44.1kHz 이상만 재사용한다.
        //   더 낮은 값이 저장돼 있으면 무시하고 새 우선순위(44.1kHz 우선)로 다시 탐색한다.
        if (savedSource >= 0 && savedRate >= 44_100) {
            val saved = savedSource to savedRate
            combos = listOf(saved) + combos.filterNot { it == saved }
        }

        var lastError: Exception? = null

        for ((source, rate) in combos) {
            val recorder = newRecorder()
            try {
                recorder.apply {
                    setAudioSource(source)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    // 음질 우선: 44.1kHz 이상은 96kbps, 낮은 폴백 샘플레이트는 그에 맞춰 낮춘다
                    setAudioEncodingBitRate(if (rate >= 44_100) 96_000 else 48_000)
                    setAudioSamplingRate(rate)
                    setAudioChannels(1)                // 모노 (증거용 — 방향성 불필요, 용량 절반)
                    setOutputFile(file.absolutePath)

                    // ★ 통화 수신 등으로 마이크를 빼앗기면 MediaRecorder 는 조용히 죽는다.
                    //   객체는 null 이 아니므로 리스너가 없으면 아무도 눈치채지 못하고
                    //   "에러 없이 녹음만 안 되는" 상태가 된다.
                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("RecordingService",
                            "MediaRecorder 오류 what=$what extra=$extra — 청크 재시작")
                        mainHandler.post { if (shouldRecord) restartChunk() }
                    }
                    setOnInfoListener { _, what, extra ->
                        android.util.Log.w("RecordingService",
                            "MediaRecorder info what=$what extra=$extra")
                    }

                    android.util.Log.i(traceTag, "6 prepare source=$source rate=$rate")
                    prepare()
                    android.util.Log.i(traceTag, "6 prepare success source=$source rate=$rate")
                    android.util.Log.i(traceTag, "7 start source=$source rate=$rate")
                    start()
                    android.util.Log.i(traceTag, "7 start success source=$source rate=$rate")
                }
                mediaRecorder = recorder
                // 성공 조합 기억 — 이후 청크도 같은 인코딩으로 맞춘다
                if (savedSource != source || savedRate != rate) {
                    prefs.edit()
                        .putInt(KEY_AUDIO_SOURCE, source)
                        .putInt(KEY_SAMPLE_RATE, rate)
                        .apply()
                }
                startAmplitudeSampler()   // 타임라인용 진폭 수집 시작
                android.util.Log.i("RecordingService",
                    "녹음 시작 성공 — source=$source, ${rate}Hz")
                return
            } catch (e: Exception) {
                android.util.Log.e(traceTag,
                    "6/7 failed source=$source rate=$rate ${e.javaClass.simpleName}: ${e.message}", e)
                lastError = e
                // ★ 실패한 recorder는 반드시 release.
                //   이걸 빠뜨리면 마이크 핸들이 누수돼 이후 모든 녹음 시도가
                //   그 기기에서 영구히 실패한다. (기존 코드의 치명적 결함)
                try { recorder.reset() } catch (_: Exception) {}
                try { recorder.release() } catch (_: Exception) {}
                android.util.Log.w("RecordingService",
                    "source=$source ${rate}Hz 설정 실패: ${e.message}")
            }
        }

        // 모든 조합 실패 — 호출부가 재시도 로직을 돌리도록 예외를 올린다
        currentFile = null
        if (file.exists()) file.delete()
        throw lastError ?: IllegalStateException("MediaRecorder 시작 실패 (모든 조합)")
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
        else MediaRecorder()

    /**
     * 10분마다 청크 회전: 현재 파일 저장 → 새 파일 시작
     * ★ mediaRecorder가 null(청크 시작 실패 상태)이면 회전 대신 재시작 시도
     */
    private fun scheduleChunkRotation() {
        chunkRotationJob?.cancel()
        chunkRotationJob = serviceScope.launch {
            while (isActive) {
                delay(HEALTH_CHECK_MS)

                // ★★ 루프 전체를 try 로 감싼다.
                //   예외가 하나라도 밖으로 나가면 이 코루틴이 죽고 회전이 영구 정지된다.
                try {
                    if (!shouldRecord) continue

                    // A recorder can report an error while its object remains
                    // non-null. Treat the authoritative recording state as
                    // the health signal too, so cooldown cannot strand that
                    // failed instance until the 10-minute rotation boundary.
                    if (!isRunning) {
                        android.util.Log.w(
                            "RecordingService",
                            "shouldRecord=true but isRunning=false — recovery attempt"
                        )
                        withContext(Dispatchers.Main) { restartChunk() }
                        continue
                    }

                    // ① 녹음기가 없으면 재시작
                    if (mediaRecorder == null) {
                        android.util.Log.w("RecordingService", "mediaRecorder null 감지 — 재시작")
                        withContext(Dispatchers.Main) { startNewChunk() }
                        markRecordingStarted()
                        notifyRecordingState(ok = true)
                        continue
                    }

                    // ② 회전 판정은 delay 누적이 아니라 벽시계 기준으로 한다.
                    //    Doze 로 타이머가 밀려도 깨어나는 즉시 올바르게 회전된다.
                    //
                    // ★ 예전에 있던 '파일 크기 정체 감지'는 제거했다.
                    //   MediaRecorder(MPEG_4/AAC)는 데이터를 버퍼에 모았다 주기적으로만
                    //   디스크에 flush 하고, 조용한 구간은 거의 0바이트로 압축된다.
                    //   그래서 조용한 시간이 조금만 이어져도 file.length() 가 그대로여서
                    //   멀쩡히 녹음 중인데 "정체"로 오판 → 청크를 끊고 재시작하는 버그가 있었다.
                    //   (33초·17초로 잘게 쪼개지고 소리가 안 담기던 원인)
                    //   진짜 마이크 탈취는 setOnErrorListener 가 잡는다.
                    if (System.currentTimeMillis() - currentChunkStartTime >= CHUNK_DURATION_MS) {
                        withContext(Dispatchers.Main) { rotateChunk() }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RecordingService",
                        "회전 루프 오류(계속 진행): ${e.message}", e)
                }
            }
        }
    }

    /**
     * 현재 청크를 저장 가능한 상태면 저장하고 새 청크를 시작한다.
     * 정체(마이크 탈취) 감지 시 호출.
     */
    private fun restartChunk() {
        if (!shouldRecord) return
        setActuallyRecording(false)
        // ★ 재시작 폭주 방지.
        //   통화처럼 마이크를 오래 점유하는 상황에서는 새 recorder 도 곧바로 에러가 나므로
        //   가드가 없으면 에러 리스너 → 재시작 → 에러 리스너 … 무한 루프가 된다.
        val now0 = System.currentTimeMillis()
        if (now0 - lastRestartMs < RESTART_COOLDOWN_MS) {
            val remainingMs = RESTART_COOLDOWN_MS - (now0 - lastRestartMs)
            android.util.Log.w(
                "RecordingService",
                "재시작 쿨다운 중 — health check에서 재시도 예정 (${remainingMs}ms 남음)"
            )
            notifyRecordingState(ok = false)
            return
        }
        lastRestartMs = now0

        val endTime = System.currentTimeMillis()
        val file = currentFile
        val savedStartTime = currentChunkStartTime

        var stopSucceeded = false
        try {
            mediaRecorder?.apply { stop(); stopSucceeded = true; release() }
        } catch (e: Exception) {
            android.util.Log.w("RecordingService", "정체 청크 stop 실패: ${e.message}")
            try { mediaRecorder?.release() } catch (_: Exception) {}
        }
        mediaRecorder = null
        currentFile = null

        if (stopSucceeded && file != null && file.exists() && file.length() > 1024L) {
            saveChunkSafely(ChunkEntity(
                startTimeMs = savedStartTime,
                endTimeMs   = endTime,
                filePath    = file.absolutePath,
                amplitudes  = takeAmplitudes()
            ))
        } else if (file != null && file.exists()) {
            file.delete()
        }

        try {
            startNewChunk()
            markRecordingStarted()
            notifyRecordingState(ok = true)
        } catch (e: Exception) {
            android.util.Log.e("RecordingService", "정체 후 재시작 실패: ${e.message}")
            notifyRecordingState(ok = false)
        }
    }

    /**
     * DB 저장 — 실패해도 절대 앱을 죽이지 않는다.
     *
     * serviceScope 는 SupervisorJob 이라 자식 실패가 형제에게 전파되지는 않지만,
     * CoroutineExceptionHandler 가 없어 미처리 예외는 기본 핸들러로 가 앱이 크래시한다.
     */
    /**
     * 녹음 중 진폭 수집 — 48시간 타임라인을 그리기 위한 데이터.
     *
     * getMaxAmplitude() 는 "마지막 호출 이후의 최댓값"을 돌려준다.
     * 10초마다 한 번씩 읽어 그 구간에 소리가 있었는지만 기록한다.
     * 파일 디코딩이 없어 배터리 부담이 사실상 없다.
     */
    private fun startAmplitudeSampler() {
        ampSamplerJob?.cancel()
        currentAmplitudes.clear()
        ampSamplerJob = serviceScope.launch {
            while (isActive) {
                delay(AMP_SAMPLE_MS)
                try {
                    val raw = mediaRecorder?.maxAmplitude ?: continue
                    // 0~32767 → 0~100. 말소리 구간이 눈에 띄도록 제곱근 스케일을 쓴다.
                    val norm = (kotlin.math.sqrt(raw.coerceAtLeast(0) / 32767.0) * 100).toInt()
                    currentAmplitudes.add(norm.coerceIn(0, 100))
                } catch (e: Exception) {
                    // recorder 가 정리되는 중이면 무시
                }
            }
        }
    }

    /** 수집한 진폭을 문자열로 — 저장 후 비운다 */
    private fun takeAmplitudes(): String? {
        if (currentAmplitudes.isEmpty()) return null
        val s = currentAmplitudes.joinToString(",")
        currentAmplitudes.clear()
        return s
    }

    private fun saveChunkSafely(chunk: ChunkEntity) {
        serviceScope.launch {
            try {
                repository.saveChunk(chunk)
            } catch (e: Exception) {
                android.util.Log.e("RecordingService", "청크 DB 저장 실패: ${e.message}", e)
            } finally {
                // ★ DB 반영이 끝난 뒤에 알린다 — UI 가 이 시점을 기다린다.
                //   성공/실패 무관: 실패해도 UI 가 더 기다릴 이유는 없다.
                signalFlushDone()
            }
        }
    }

    /** 미리듣기·STT용: 현재 청크를 저장소에 넣고 새 청크 시작 (잠금 없음) */
    private fun rotateChunk(requestId: Long = 0L) = flushCurrent(null, null, requestId = requestId)

    /**
     * 저장용: 현재 청크를 저장소에 넣고, 지정 구간을 잠근다.
     *
     * ★ 잠금은 별도의 저장이 아니다. 저장소는 하나뿐이고, 잠금은 그 저장소의
     *   행에 붙는 플래그일 뿐이다. 그래서 예전처럼 "현재 청크를 isLocked=true 로
     *   따로 한 번 더 저장"하지 않는다. 현재 청크도 일반 버퍼 청크로 딱 한 번만
     *   저장하고, lockRange 가 그 청크(및 겹치는 옛 청크)를 잠근다.
     */
    private fun rotateChunkLocked(fromMs: Long, toMs: Long, requestId: Long = 0L) {
        // ★ 중복 잠금 요청 차단 — Activity 재생성 등으로 같은 요청이 연달아 올 때
        val now = System.currentTimeMillis()
        if (fromMs == lastLockFrom && toMs == lastLockTo && now - lastLockAtMs < 3_000L) {
            android.util.Log.w("RecordingService", "중복 잠금 요청 무시")
            signalFlushDone()
            return
        }
        lastLockFrom = fromMs; lastLockTo = toMs; lastLockAtMs = now
        android.util.Log.i("SB_SAVE",
            "잠금 저장 요청: from=$fromMs to=$toMs (범위 ${(toMs - fromMs) / 1000}초)")
        flushCurrent(fromMs, toMs, requestId = requestId)
    }

    /**
     * 현재 녹음 청크를 저장소(단일 audio_chunks 테이블)에 한 번 저장하고 새 청크를 시작한다.
     * lockFrom/lockTo 가 주어지면 저장 후 그 구간을 잠근다(플래그).
     *
     * ★ flushInProgress 로 중복 저장을 막는다.
     *   저장이 이미 진행 중인데 또 요청이 오면(미리듣기→저장 연타, 인텐트 이중 전달),
     *   현재 파일을 두 번 저장해 같은 구간이 복제됐다. 두 번째 요청은 저장을 반복하지
     *   않고 잠금 범위만 반영한다.
     */
    private fun flushCurrent(lockFrom: Long?, lockTo: Long?, retry: Int = 0, requestId: Long = 0L) {
        logFlush("FLUSH_START retry=$retry", requestId)
        if (retry == 0) {
            if (flushInProgress) {
                android.util.Log.w("SB_SAVE", "이미 저장 진행 중 — 중복 저장 대신 잠금만 적용")
                lockOnly(lockFrom, lockTo)
                return
            }
            flushInProgress = true
        }

        val endTime = System.currentTimeMillis()
        val file = currentFile
        if (file == null) {              // 녹음 중이 아니면 잠금만 적용
            android.util.Log.w("SB_SAVE", "현재 녹음 파일 없음 — 잠금만 적용")
            flushInProgress = false
            lockOnly(lockFrom, lockTo)
            return
        }
        // ★ startNewChunk()가 currentChunkStartTime을 덮어쓰기 전에 반드시 캡처
        val savedStartTime = currentChunkStartTime

        // MediaRecorder는 너무 짧으면 stop() 이 예외를 던진다.
        // 증거 앱이라 최근 몇 초를 버릴 수 없으므로 안전 길이가 될 때까지 기다렸다가 저장한다.
        val chunkDurationMs = endTime - savedStartTime
        if (chunkDurationMs < MIN_CHUNK_MS) {
            if (retry >= MAX_SHORT_CHUNK_RETRY) {
                android.util.Log.w("RecordingService",
                    "짧은 청크 재시도 한계 — 저장 없이 잠금만 (${chunkDurationMs}ms)")
                flushInProgress = false
                lockOnly(lockFrom, lockTo)
                return
            }
            val wait = MIN_CHUNK_MS - chunkDurationMs + 200L
            android.util.Log.w("RecordingService",
                "청크가 짧아 ${wait}ms 대기 후 저장 (${chunkDurationMs}ms, retry=$retry)")
            serviceScope.launch(Dispatchers.Main) {
                delay(wait)
                if (shouldRecord) flushCurrent(lockFrom, lockTo, retry + 1, requestId)
                else { flushInProgress = false; signalFlushDone() }
            }
            return
        }

        // 현재 MediaRecorder 마무리
        var stopSucceeded = false
        try {
            android.util.Log.i("SaveFlow", "MEDIARECORDER_STOP requestId=$requestId file=${file.absolutePath}")
            mediaRecorder?.apply {
                stop()
                stopSucceeded = true  // stop() 성공 = 파일 완결됨 (release 실패와 무관)
                release()
            }
            android.util.Log.i("SaveFlow", "MEDIARECORDER_STOP_SUCCESS requestId=$requestId")
        } catch (e: Exception) {
            android.util.Log.e("SaveFlow", "MEDIARECORDER_STOP_FAILED requestId=$requestId ${e.message}", e)
            android.util.Log.w("RecordingService", "MediaRecorder stop 실패: ${e.message}")
            try { mediaRecorder?.release() } catch (_: Exception) {}
        }
        mediaRecorder = null
        setActuallyRecording(false)

        // stop 성공 + 파일에 실제 데이터가 있을 때만 저장소에 넣는다 (딱 한 번)
        if (stopSucceeded && file.exists() && file.length() > 1024L) {
            android.util.Log.i("SB_SAVE",
                "청크 저장: ${chunkDurationMs / 1000}초, ${file.length()}bytes" +
                if (lockFrom != null) " + 범위잠금" else "")
            val amps = takeAmplitudes()
            serviceScope.launch {
                try {
                    // 저장소는 하나 — 현재 청크도 일반 버퍼 청크로 한 번만 저장한다.
                    val insertedId = repository.saveChunk(ChunkEntity(
                        startTimeMs = savedStartTime,
                        endTimeMs   = endTime,
                        filePath    = file.absolutePath,
                        amplitudes  = amps
                    ))
                    android.util.Log.i("SaveFlow",
                        "DB_CHUNK_INSERT requestId=$requestId id=$insertedId start=$savedStartTime " +
                            "end=$endTime file=${file.absolutePath}")
                    // 잠금은 저장소에 붙는 플래그 — 방금 저장한 청크와 겹치는 옛 청크를 모두 잠근다
                    if (lockFrom != null && lockTo != null) {
                        repository.lockRange(lockFrom, lockTo, requestId.takeIf { it != 0L })
                        val locked = repository.getAllLockedOnce()
                            .filter { it.startTimeMs <= lockTo && it.endTimeMs >= lockFrom }
                        android.util.Log.i("SaveFlow",
                            "LOCK_RANGE requestId=$requestId from=$lockFrom to=$lockTo " +
                                "count=${locked.size} ids=${locked.map { it.id }}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RecordingService", "저장/잠금 실패: ${e.message}", e)
                } finally {
                    signalFlushDone()
                }
            }
        } else {
            android.util.Log.w("SB_SAVE",
                "청크 저장 건너뜀: stopSucceeded=$stopSucceeded, size=${if (file.exists()) file.length() else -1}" +
                if (lockFrom != null) " → 범위잠금만" else "")
            if (file.exists()) {
                file.delete()  // 빈/불완전 파일 정리
            }
            // 저장할 게 없어도 잠금 요청이면 기존 청크 범위 잠금은 한다
            lockOnly(lockFrom, lockTo)
        }

        // 새 청크 시작 — 실패 시 1분 후 재시도 (scheduleChunkRotation도 10분 후 재시도)
        try {
            android.util.Log.i("SaveFlow", "MEDIARECORDER_RESTART requestId=$requestId")
            startNewChunk()
            markRecordingStarted()
            android.util.Log.i("SaveFlow", "MEDIARECORDER_RESTART_SUCCESS requestId=$requestId")
        } catch (e: Exception) {
            android.util.Log.e("SaveFlow", "MEDIARECORDER_RESTART_FAILED requestId=$requestId ${e.message}", e)
            android.util.Log.e("RecordingService", "새 청크 시작 실패: ${e.message}")
            currentFile = null
            mediaRecorder = null
            // ★ 10분 루프를 기다리지 않고 1분 후 즉시 재시도 (Main thread — MediaRecorder 요구사항)
            notifyRecordingState(ok = false)
            serviceScope.launch(Dispatchers.Main) {
                delay(60_000L)
                if (shouldRecord && mediaRecorder == null) {
                    try {
                        startNewChunk()
                        markRecordingStarted()
                        android.util.Log.i("RecordingService", "1분 후 청크 재시작 성공")
                        notifyRecordingState(ok = true)
                    } catch (e2: Exception) {
                        android.util.Log.e("RecordingService", "1분 후 재시작도 실패: ${e2.message}")
                    }
                }
            }
        } finally {
            // 여기까지 오면 현재 청크는 저장소에 넣었고 새 청크로 넘어갔다.
            // 이후 요청은 새 청크를 대상으로 하므로 잠금을 푼다.
            flushInProgress = false
        }
    }

    private fun logFlush(event: String, requestId: Long) {
        android.util.Log.i("SaveFlow",
            "$event requestId=$requestId currentFile=${currentFile?.absolutePath} " +
                "chunkStart=$currentChunkStartTime shouldRecord=$shouldRecord isRunning=$isRunning " +
                "serviceActive=$isServiceActive")
    }

    /** 저장 없이 구간 잠금만 적용하고 완료를 통지한다 */
    private fun lockOnly(lockFrom: Long?, lockTo: Long?, requestId: Long = 0L) {
        if (lockFrom == null || lockTo == null) {
            signalFlushDone()
            return
        }
        serviceScope.launch {
            try {
                repository.lockRange(lockFrom, lockTo, requestId.takeIf { it != 0L })
                android.util.Log.i("SB_SAVE", "범위 잠금 완료: from=$lockFrom to=$lockTo")
                val locked = repository.getAllLockedOnce()
                    .filter { it.saveGroupId == requestId }
                android.util.Log.i("SaveFlow",
                    "LOCK_RANGE requestId=$requestId from=$lockFrom to=$lockTo " +
                        "count=${locked.size} ids=${locked.map { it.id }}")
            }
            catch (e: Exception) { android.util.Log.e("SB_SAVE", "범위 잠금 실패: ${e.message}") }
            finally { signalFlushDone() }
        }
    }

    // ──────────────────────────────────────────────────────────
    // 녹음 정지
    // ──────────────────────────────────────────────────────────

    private fun stopRecording() {
        if (!shouldRecord && mediaRecorder == null && currentFile == null) {
            // ★ onDestroy()에서 이중 호출 방지 — stopSelf() → onDestroy() → stopRecording() 재진입
            android.util.Log.d("RecordingService", "stopRecording 중복 호출 — 무시")
            return
        }
        shouldRecord = false
        setActuallyRecording(false)
        releaseWakeLock()
        ampSamplerJob?.cancel()
        // 녹음 시작 시각 초기화
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .remove(KEY_RECORDING_START_MS)
            .apply()
        chunkRotationJob?.cancel()

        val endTime = System.currentTimeMillis()
        val file = currentFile
        currentFile = null  // ★ 즉시 null화 → onDestroy() 이중 호출 시 중복 DB 저장 방지

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) { /* 무시 */ }
        mediaRecorder = null

        // 마지막 청크도 DB에 저장
        if (file != null && file.exists() && file.length() > 0) {
            saveChunkSafely(
                ChunkEntity(
                    startTimeMs = currentChunkStartTime,
                    endTimeMs = endTime,
                    filePath = file.absolutePath,
                    amplitudes = takeAmplitudes()
                )
            )
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * 전체 초기화 전용 종료. 현재 청크를 DB에 저장하지 않고 recorder와 파일만 정리한다.
     * 이미 시작된 DB 작업도 취소 후 완료될 때까지 기다려 resetAll() 뒤 재삽입되는 race를 막는다.
     */
    private fun stopForReset() {
        if (resetStopRequested) return
        resetStopRequested = true
        shouldRecord = false
        setActuallyRecording(false)
        chunkRotationJob?.cancel()
        ampSamplerJob?.cancel()
        releaseWakeLock()

        try { mediaRecorder?.stop() } catch (_: Exception) {}
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null
        currentFile?.let { if (it.exists()) it.delete() }
        currentFile = null
        currentAmplitudes.clear()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().remove(KEY_RECORDING_START_MS).apply()

        val pendingJobs = serviceScope.coroutineContext[Job]
            ?.children
            ?.filter { it.isActive }
            ?.toList()
            .orEmpty()
        pendingJobs.forEach { it.cancel() }
        serviceScope.launch {
            pendingJobs.joinAll()
            withContext(Dispatchers.Main) {
                resetStopReady = true
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
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
        // ★ 초 단위 타임스탬프만 쓰면 같은 초에 두 번 호출될 때(연속 flush 등)
        //   파일명이 겹쳐 앞 청크가 덮어써진다 → 밀리초까지 사용하고, 그래도 겹치면 접미사를 붙인다.
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
            .format(Date())
        var file = File(dir, "chunk_$timestamp.m4a")
        var seq = 1
        while (file.exists()) {
            file = File(dir, "chunk_${timestamp}_$seq.m4a")
            seq++
        }
        return file
    }

    private fun buildNotification(failed: Boolean = false): Notification {
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

        val title = if (failed) "⚠️ SafeBuffer 녹음 실패"
                    else        "🔴 SafeBuffer 녹음 중"
        val text  = if (failed)
            "마이크를 사용할 수 없습니다. 다른 앱이 마이크를 쓰고 있는지 확인 후 앱을 다시 열어주세요."
        else
            "대화 자동저장 기간은 48시간 입니다. 필요시 대화내용을 저장하세요."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.ic_stop, "중지", stopIntent)
            .build()
    }

    /**
     * 실제 녹음 상태를 알림에 반영한다.
     *
     * 증거 앱에서 "녹음 중"이라고 표시해 놓고 실제로는 녹음이 안 되는 것은
     * 가장 위험한 실패 방식이므로, 실패하면 반드시 사용자에게 보인다.
     */
    private fun notifyRecordingState(ok: Boolean) {
        try {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(failed = !ok))
        } catch (e: Exception) {
            android.util.Log.w("RecordingService", "알림 갱신 실패: ${e.message}")
        }
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
