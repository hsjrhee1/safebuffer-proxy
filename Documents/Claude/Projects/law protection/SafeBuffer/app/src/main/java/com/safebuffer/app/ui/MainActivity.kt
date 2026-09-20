package com.safebuffer.app.ui

import android.Manifest
import android.app.Dialog
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.safebuffer.app.R
import com.safebuffer.app.databinding.ActivityMainBinding
import com.safebuffer.app.service.RecordingService
import com.safebuffer.app.ui.evidence.LockedEvidenceActivity
import com.safebuffer.app.ui.settings.SettingsActivity
import com.safebuffer.app.ui.transcript.TranscriptActivity
import com.safebuffer.app.util.BillingManager
import com.safebuffer.app.util.TrialManager
import com.safebuffer.app.worker.CleanupWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val traceTag = "SafeBufferTrace"

    // ── 상수 ────────────────────────────────────────────────────────────────

    companion object {
        // tvBufferStatus 텍스트 상태값 — updateRecordingUI / allChunks 컬렉터가 함께 관리
        const val TEXT_WAITING  = "녹음 대기 중"
        const val TEXT_STARTING = "녹음 시작 중..."

        // 버튼 / 인디케이터 색상 (ARGB int)
        private const val COLOR_BTN_RECORDING = 0x33333333.toInt() // 녹음 중 — 어두운 회색
        private const val COLOR_BTN_IDLE      = 0xFF1A2A4A.toInt() // 대기 중 — 진한 남색
        private const val COLOR_STATUS_ACTIVE = 0xFFFF4444.toInt() // 녹음 중 상태 텍스트 — 빨간색
        private const val COLOR_STATUS_IDLE   = 0xFF666688.toInt() // 대기 중 상태 텍스트 — 회색
        private const val COLOR_MIC_IDLE      = 0xFFAAAAAA.toInt() // 마이크 아이콘 비활성 — 연회색
    }

    // ── 필드 ────────────────────────────────────────────────────────────────

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // 첫 청크 저장(10분) 전까지 경과 시간을 표시하는 타이머
    private var elapsedTimerJob: Job? = null
    private var serviceStartRequested = false
    private var saveRequestSequence = 0L

    // ── 선택 구간 ────────────────────────────────────────────────────────────
    // ★ 구간은 화면의 선택 비율(selNear/selFar)로만 들고 있고, 실제 시각은
    //   저장·미리듣기 '그 순간'에 항상 현재 시각 기준으로 다시 계산한다.
    //   (예전엔 구간을 조정할 때 계산한 시각을 고정으로 들고 있어서, 몇 분/몇 시간 뒤
    //    같은 구간으로 다시 저장하면 옛날 시각이 그대로 쓰여 이전 녹음을 덮어썼다.)
    private var spanMinutes = 60        // 파형에 담는 범위 (버튼)

    // 미리듣기 진행 표시용 — 재생 중인 청크의 시작 시각과 진행 타이머
    private var previewChunkStartMs = 0L
    private var previewTickJob: Job? = null
    // 미리듣기로 재생할 실제 구간 (선택 구간). 파일은 이보다 길 수 있으므로
    // 첫 파일은 이 시작점으로 건너뛰고, 이 끝점에 닿으면 멈춘다.
    private var previewFromMs = 0L
    private var previewToMs = 0L
    private var previewSnapshotReady = false

    // ── 구간 미리듣기 ────────────────────────────────────────────────────────
    // 선택 구간은 여러 청크에 걸쳐 있으므로 순서대로 이어서 재생한다.
    private var previewPlayer: android.media.MediaPlayer? = null
    /** 재생할 청크 — 파일과 그 청크의 시작 시각 (파형 진행 표시에 필요) */
    private var previewQueue: List<Pair<java.io.File, Long>> = emptyList()
    private var previewIndex = 0

    @Inject lateinit var trialManager: TrialManager
    @Inject lateinit var billingManager: BillingManager

    // ── 권한 요청 ────────────────────────────────────────────────────────────

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) startSafeBuffer()
        else showPermissionDeniedDialog()
    }

    // ── 생명주기 ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상태바 영역까지 앱이 그리도록 설정 (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바 높이만큼 최상단 패딩 적용 → SafeBuffer 타이틀이 시계와 겹치지 않음
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight   = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(top = statusBarHeight, bottom = navBarHeight)
            insets
        }

        trialManager.initIfNeeded()
        billingManager.connect()
        CleanupWorker.schedule(this)
        showBuildInfo()

        // 서비스가 이미 돌고 있을 때만 미리 true — 권한만 보고 낙관적으로 켜면
        // 실제로 녹음이 시작되지 못한 경우에도 "녹음 중"으로 표시돼 실패를 숨긴다.
        if (RecordingService.isRunning) {
            viewModel.setRecordingState(true)
        }

        setupUI()
        observeViewModel()
        warmUpServer()

        // 불완전한 reset 잔존 레코드 정리 (백그라운드)
        lifecycleScope.launch(Dispatchers.IO) { viewModel.cleanupOrphans() }

        // ★ 순서가 중요하다.
        //   배터리 최적화 다이얼로그를 먼저 띄우면 MainActivity 가 백그라운드로 밀리고,
        //   Android 12+ 는 백그라운드에서 포그라운드 서비스 시작을 막는다.
        //   그러면 녹음이 시작되지 못한 채 조용히 실패한다.
        //   → 녹음을 먼저 시작하고, 배터리 설정은 그 뒤에 요청한다.
        if (!RecordingService.isServiceActive) {
            checkPermissionsAndStart()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPreview()   // 화면을 벗어나면 미리듣기를 계속 재생하지 않는다
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.i(traceTag,
            "9 onResume before active=${RecordingService.isServiceActive} " +
                "running=${RecordingService.isRunning} flow=${RecordingService.recordingState.value} " +
                "vm=${viewModel.isRecording.value} requested=$serviceStartRequested")
        if (RecordingService.isServiceActive) serviceStartRequested = false
        // 실제 녹음 UI의 단일 출처는 RecordingService.recordingState collector다.
        // 여기서 별도로 ViewModel에 쓰면 앱/서비스가 동시에 시작되는 순간의 false가
        // MediaRecorder.start() 성공 직후의 true를 덮을 수 있다.
        android.util.Log.i(traceTag,
            "9 onResume after vm=${viewModel.isRecording.value} requested=$serviceStartRequested")
        if (!RecordingService.isServiceActive && !serviceStartRequested) {
            checkPermissionsAndStart()
        }
        // 다른 화면에서 잠금 후 돌아왔을 때 카운트 즉시 갱신
        lifecycleScope.launch {
            binding.tvLockedCountBadge.text = "${viewModel.getLockedCount()}개 ›"
            binding.tvTranscriptCountBadge.text = "${viewModel.getTranscriptCount()}개 ›"
        }
        // 백그라운드 복귀 시 타이머 재시작 (녹음 중인 경우)
        if (viewModel.isRecording.value) {
            startElapsedTimer()
        }
        refreshTimeline()   // 새 녹음이 쌓였을 수 있다
        applyRange()        // 시간이 흘렀으니 구간 시각 라벨도 현재 기준으로 갱신
    }

    // ── UI 설정 ──────────────────────────────────────────────────────────────

    private fun setupUI() {
        // 녹음 시작/중지 토글
        binding.btnToggleRecording.setOnClickListener {
            android.util.Log.i(traceTag,
                "1 button click vm=${viewModel.isRecording.value} " +
                    "active=${RecordingService.isServiceActive} running=${RecordingService.isRunning} " +
                    "requested=$serviceStartRequested")
            if (viewModel.isRecording.value) stopSafeBuffer()
            else checkPermissionsAndStart()
        }

        // AI 대화 기록 만들기 — 구간을 골라 글로 옮긴다
        binding.btnViewTranscript.setOnClickListener {
            if (binding.btnViewTranscript.tag == "loading") return@setOnClickListener
            binding.btnViewTranscript.tag = "loading"
            Toast.makeText(this, "녹음 저장 중... 잠시 기다려주세요", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                awaitFlushCompleted { RecordingService.flushIntent(this@MainActivity) }
                binding.btnViewTranscript.tag = null
                showHoursPickerDialog()
            }
        }

        // 보관된 대화 기록 — 만들어둔 것 확인 / PDF·Word 내보내기
        binding.cardTranscriptArchive.setOnClickListener {
            startActivity(LockedEvidenceActivity.transcriptIntent(this))
        }
        binding.btnTranscriptArchive.setOnClickListener {
            binding.btnViewTranscript.performClick()   // '만들기 ›' 배지도 카드와 같은 동작
        }

        // 녹음 확인하기 (무료) — 저장한 녹음
        binding.tvLockedCount.setOnClickListener {
            startActivity(LockedEvidenceActivity.audioIntent(this))
        }

        // 설정 화면으로 이동
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 버튼 = 파형에 담을 범위. 파형 위에서 실제 구간을 자른다.
        binding.btnRange1h.setOnClickListener   { selectSpan(60) }
        binding.btnRange1d.setOnClickListener   { selectSpan(24 * 60) }
        binding.btnRangeAll.setOnClickListener  { selectSpan(48 * 60) }

        binding.timelineView.onRangeChanged = { _, _ ->
            previewSnapshotReady = false
            applyRange()
            if (previewPlayer != null) stopPreview()
        }

        selectSpan(60)   // 기본: 최근 1시간

        // 빠른 잠금 실행
        // ★ 새 방식: 잠금 범위를 서비스에 전달 → 현재 청크를 isLocked=true 로 직접 저장
        //           딜레이 불필요 — 비동기 타이밍 문제 원천 차단
        // 저장 전에 귀로 확인
        binding.btnPreview.setOnClickListener { togglePreview() }
        binding.tvPreviewStatus.setOnClickListener { stopPreview() }

        binding.btnLock.setOnClickListener {
            stopPreview()   // 저장 다이얼로그를 띄우기 전에 재생 정리
            val (fromMs, toMs) = if (previewSnapshotReady) {
                previewFromMs to previewToMs
            } else selectedRangeMs()
            val lengthMin = ((toMs - fromMs) / 60_000f)
            if (lengthMin < 1f) {
                Toast.makeText(this, "구간이 너무 짧습니다. 손잡이를 벌려 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val timeFmt = SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA)
            val fromTime = timeFmt.format(Date(fromMs))
            val toTime   = timeFmt.format(Date(toMs))
            MaterialAlertDialogBuilder(this)
                .setTitle("이 구간을 남겨둘까요?")
                .setMessage(
                    "$fromTime  ~  $toTime\n" +
                    "${formatDuration(lengthMin)} 분량\n\n" +
                    "이 구간은 48시간이 지나도 삭제되지 않습니다."
                )
                .setPositiveButton("저장") { _, _ ->
                    val requestId = nextSaveRequestId()
                    android.util.Log.i("SaveFlow", "SAVE_CLICK requestId=$requestId from=$fromMs to=$toMs")
                    // ★ 저장(현재 청크 저장 + 범위 잠금)은 전부 서비스가 한 곳에서 처리한다.
                    //   UI 는 요청만 보내고, 완료를 기다린 뒤 결과 화면으로 간다.
                    lifecycleScope.launch {
                        awaitFlushCompleted {
                            if (previewSnapshotReady) {
                                RecordingService.lockRangeIntent(this@MainActivity, fromMs, toMs, requestId)
                            } else {
                                RecordingService.flushAndLockIntent(this@MainActivity, fromMs, toMs, requestId)
                            }
                        }
                        previewSnapshotReady = false
                        rememberLastSaved(fromMs, toMs)
                        startActivity(LockedEvidenceActivity.justSavedIntent(this@MainActivity))
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    // ── ViewModel 관찰 ───────────────────────────────────────────────────────

    private fun observeViewModel() {
        // 서비스 요청이 아니라 실제 MediaRecorder.start() 성공 여부만 UI에 표시한다.
        lifecycleScope.launch {
            RecordingService.recordingState.collect { isRecording ->
                android.util.Log.i(traceTag,
                    "8 recordingState=$isRecording active=${RecordingService.isServiceActive} " +
                        "running=${RecordingService.isRunning}")
                if (isRecording) serviceStartRequested = false
                viewModel.setRecordingState(isRecording)
            }
        }
        // 녹음 상태 변화 → 버튼/인디케이터/상태 텍스트 전체 갱신
        lifecycleScope.launch {
            viewModel.isRecording.collect { isRecording ->
                android.util.Log.i(traceTag,
                    "10 viewModel recording=$isRecording active=${RecordingService.isServiceActive} " +
                        "running=${RecordingService.isRunning} flow=${RecordingService.recordingState.value}")
                updateRecordingUI(isRecording)
                if (isRecording) startElapsedTimer() else elapsedTimerJob?.cancel()
            }
        }

        // UI 이벤트 (잠금 성공, 무료 한도 초과, 에러)
        lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    // LockSuccess 는 더 이상 쓰지 않는다 — 저장/이동은 저장 버튼이 직접 처리
                    is UiEvent.LockSuccess -> { }
                    is UiEvent.FreeLimitReached -> {
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("저장 한도에 도달했습니다")
                            .setMessage(event.message)
                            .setPositiveButton("설정 열기") { _, _ ->
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            }
                            .setNegativeButton("닫기", null)
                            .show()
                    }
                    is UiEvent.Error -> {
                        Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // 청크 목록 변화 → 버퍼 시간 텍스트 갱신
        lifecycleScope.launch {
            viewModel.allChunks
                .collect { chunks ->
                    val buffer = chunks.filter { !it.isLocked }
                    if (buffer.isNotEmpty()) {
                        val oldestMs = buffer.minOf { it.startTimeMs }
                        // newestMs를 현재 시간으로 고정 → 버튼 누를 때 시간이 갑자기 바뀌지 않음
                        val totalMin = ((System.currentTimeMillis() - oldestMs) / 60_000L)
                            .toInt().coerceAtLeast(0)
                        binding.tvBufferStatus.text = when {
                            totalMin == 0 -> "1분 미만 녹음됨"
                            totalMin < 60 -> "${totalMin}분 녹음됨"
                            else          -> "${totalMin / 60}시간 ${totalMin % 60}분 녹음됨"
                        }
                    } else {
                        // 버퍼가 비었을 때: 타이머 표시 중이 아니면 기본값으로 복원
                        // (잠금 후 "1분 미만 녹음됨"이 남아있는 문제 수정)
                        if (!isPlaceholderText(binding.tvBufferStatus.text.toString())) {
                            binding.tvBufferStatus.text =
                                if (viewModel.isRecording.value) TEXT_STARTING else TEXT_WAITING
                        }
                    }
                }
        }

        // 배지 갱신 — 각 카드가 실제로 보여주는 목록과 같은 기준으로 센다.
        // 전체 잠금 수를 그대로 쓰면 "22개"인데 들어가면 비어 있는 상황이 된다.
        lifecycleScope.launch {
            viewModel.lockedChunks.collect { locked ->
                fun hasText(c: com.safebuffer.app.data.local.ChunkEntity) =
                    c.transcription?.let {
                        it.isNotBlank() && it != "[]" && it != "__SILENT__"
                    } ?: false
                // 녹음 확인하기 = 저장한 녹음 전부, 보관함 = 그중 대화기록이 있는 것
                binding.tvTranscriptCountBadge.text = "${locked.count { hasText(it) }}개 ›"
            }
        }
        lifecycleScope.launch {
            viewModel.logicalSavedCount.collect { count ->
                android.util.Log.i("SaveFlow", "EVIDENCE_COUNT_CHANGED logicalCount=$count")
                binding.tvLockedCountBadge.text = "${count}개 ›"
            }
        }
    }

    // ── UI 업데이트 ──────────────────────────────────────────────────────────

    /**
     * 녹음 상태에 따라 버튼·인디케이터·상태 텍스트를 갱신한다.
     *
     * tvBufferStatus 관리 원칙:
     * - isPlaceholderText() 상태(실제 청크 시간 미표시)일 때만 이 함수가 직접 설정한다.
     * - 버퍼에 청크가 있으면 allChunks 컬렉터가 "N분 녹음됨" 형태로 덮어쓴다.
     * - 타이머 텍스트("N초 녹음 중...")도 placeholder로 간주해 청크 텍스트에 자동 교체된다.
     */
    private fun updateRecordingUI(isRecording: Boolean) {
        android.util.Log.i(traceTag,
            "11 updateRecordingUI($isRecording) beforeStatus=${binding.tvRecordingStatus.text} " +
                "active=${RecordingService.isServiceActive} running=${RecordingService.isRunning}")
        val bufferText    = binding.tvBufferStatus.text.toString()
        if (isPlaceholderText(bufferText)) {
            binding.tvBufferStatus.text = if (isRecording) TEXT_STARTING else TEXT_WAITING
        }

        if (isRecording) {
            binding.btnToggleRecording.text = "⏹  중지"
            binding.btnToggleRecording.backgroundTintList =
                ColorStateList.valueOf(COLOR_BTN_RECORDING)

            binding.tvRecordingStatus.text = "녹음 중"
            binding.tvRecordingStatus.setTextColor(COLOR_STATUS_ACTIVE)

            binding.viewRecordingDot.setBackgroundResource(R.drawable.bg_recording_circle)
            binding.viewPulseRing.visibility = View.VISIBLE
            binding.viewPulseRing.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
            binding.ivMicIcon.clearColorFilter()

            binding.btnViewTranscript.visibility = View.VISIBLE
            binding.lockPanel.visibility = View.VISIBLE
        } else {
            binding.btnToggleRecording.text = "▶  녹음 시작"
            binding.btnToggleRecording.backgroundTintList =
                ColorStateList.valueOf(COLOR_BTN_IDLE)

            binding.tvRecordingStatus.text = "준비됨"
            binding.tvRecordingStatus.setTextColor(COLOR_STATUS_IDLE)

            binding.viewRecordingDot.setBackgroundResource(R.drawable.bg_idle_circle)
            binding.viewPulseRing.clearAnimation()
            binding.viewPulseRing.visibility = View.GONE
            binding.ivMicIcon.setColorFilter(COLOR_MIC_IDLE)

            // 녹음 중지 후에도 기존 버퍼 잠금/조회 가능
            binding.btnViewTranscript.visibility = View.VISIBLE
            binding.lockPanel.visibility = View.VISIBLE
        }
    }

    // ── 다이얼로그 ────────────────────────────────────────────────────────────

    private fun showHoursPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_time_range, null)
        val dialog = Dialog(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog)
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // 날짜 오프셋 (0=오늘, 1=어제, 2=그제)
        var dayOffset = 0

        // 기본 시간 범위: 1시간 전 ~ 지금
        val cal = Calendar.getInstance()
        var endHour   = cal.get(Calendar.HOUR_OF_DAY)
        var endMin    = cal.get(Calendar.MINUTE)
        cal.add(Calendar.HOUR_OF_DAY, -1)
        var startHour = cal.get(Calendar.HOUR_OF_DAY)
        var startMin  = cal.get(Calendar.MINUTE)

        val btnStart  = dialogView.findViewById<Button>(R.id.btnStartTime)
        val btnEnd    = dialogView.findViewById<Button>(R.id.btnEndTime)
        val tvSummary = dialogView.findViewById<TextView>(R.id.tvRangeSummary)

        fun fmt(h: Int, m: Int) = "%02d:%02d".format(h, m)

        fun updateSummary() {
            val rawDiff = endHour * 60 + endMin - (startHour * 60 + startMin)
            // 자정을 넘기는 경우 (예: 23:00 → 01:00) 자동으로 다음날로 처리
            val diffMin = if (rawDiff <= 0) rawDiff + 1440 else rawDiff
            val crossMidnight = rawDiff <= 0
            val durationText = when {
                diffMin < 60      -> "${diffMin}분 구간"
                diffMin % 60 == 0 -> "${diffMin / 60}시간 구간"
                else              -> "${diffMin / 60}시간 ${diffMin % 60}분 구간"
            }
            tvSummary.text = if (crossMidnight) "🌙 $durationText (자정 넘어 종료)" else durationText
        }

        btnStart.text = fmt(startHour, startMin)
        btnEnd.text   = fmt(endHour, endMin)
        updateSummary()

        btnStart.setOnClickListener {
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(startHour).setMinute(startMin)
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .setTitleText("시작 시간")
                .build().also { picker ->
                    picker.addOnPositiveButtonClickListener {
                        startHour = picker.hour; startMin = picker.minute
                        btnStart.text = fmt(startHour, startMin)
                        updateSummary()
                    }
                    picker.show(supportFragmentManager, "picker_start")
                }
        }

        btnEnd.setOnClickListener {
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(endHour).setMinute(endMin)
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .setTitleText("종료 시간")
                .build().also { picker ->
                    picker.addOnPositiveButtonClickListener {
                        endHour = picker.hour; endMin = picker.minute
                        btnEnd.text = fmt(endHour, endMin)
                        updateSummary()
                    }
                    picker.show(supportFragmentManager, "picker_end")
                }
        }

        dialogView.findViewById<ChipGroup>(R.id.chipGroupDate)
            .setOnCheckedStateChangeListener { _, checkedIds ->
                dayOffset = when (checkedIds.firstOrNull()) {
                    R.id.chipYesterday -> 1
                    R.id.chipDayBefore -> 2
                    else               -> 0
                }
            }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val baseCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -dayOffset)
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMin)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val fromMs = baseCal.timeInMillis
            baseCal.set(Calendar.HOUR_OF_DAY, endHour)
            baseCal.set(Calendar.MINUTE, endMin)
            var toMs = baseCal.timeInMillis

            // 자정 넘기: 종료가 시작보다 이른 경우 다음 날로 처리
            if (toMs <= fromMs) toMs += 24 * 60 * 60 * 1000L

            dialog.dismiss()
            startActivity(TranscriptActivity.newIntent(this, fromMs, toMs))
        }

        dialog.show()
    }

    /**
     * 현재 녹음 청크를 즉시 저장(flush)하고, **DB 반영이 끝날 때까지** 기다린다.
     *
     * 서비스는 완료 시 [RecordingService.flushCounter] 를 1 올린다.
     * 요청 시각 이후의 신호가 올 때까지 기다리되, 서비스가 죽어 있거나 응답이 없는
     * 경우를 대비해 상한(8초)을 둔다. 상한에 걸려도 진행은 한다 — 기다리다 멈추는 것보다
     * 일부 구간이 빠지더라도 화면을 보여주는 편이 낫다.
     */
    /** 마지막 저장 구간 기록 — '방금 저장한 녹음' 화면이 이 구간만 보여준다 */
    private fun rememberLastSaved(fromMs: Long, toMs: Long) {
        getSharedPreferences(LockedEvidenceActivity.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putLong(LockedEvidenceActivity.KEY_LAST_FROM, fromMs)
            .putLong(LockedEvidenceActivity.KEY_LAST_TO, toMs)
            .apply()
    }

    private suspend fun awaitFlushCompleted(intentProvider: () -> Intent) {
        // ★ 요청 전 카운터를 먼저 읽어 기준선을 잡는다.
        //   완료는 "이 값보다 카운터가 커졌을 때". 첫/두 번째 저장이 확실히 구분된다.
        val baseline = RecordingService.flushCounter.value
        try {
            startService(intentProvider())
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "flush 요청 실패: ${e.message}")
            return
        }
        val done = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
            RecordingService.flushCounter.first { it > baseline }
        }
        if (done == null) {
            android.util.Log.w("MainActivity", "flush 완료 신호 타임아웃 — 그대로 진행")
        }
    }

    /**
     * 홈 화면 하단에 버전과 빌드 번호를 표시한다.
     *
     * versionName 은 모든 빌드가 "1.0.0"이라 어느 빌드가 설치됐는지 구분할 수 없었다.
     * 테스터가 버그를 알릴 때도 빌드 번호를 바로 읽어줄 수 있어야 한다.
     */
    private fun showBuildInfo() {
        binding.tvBuildInfo.text = try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION") info.versionCode.toLong()
            }
            "버전 ${info.versionName}  ·  빌드 $code"
        } catch (e: Exception) {
            "버전 정보 없음"
        }
    }

    // ── 녹음 제어 ────────────────────────────────────────────────────────────

    private fun checkPermissionsAndStart() {
        val missing = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        android.util.Log.i(traceTag, "2 checkPermissionsAndStart missing=$missing")
        if (missing.isEmpty()) startSafeBuffer()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startSafeBuffer() {
        android.util.Log.i(traceTag,
            "3 startSafeBuffer active=${RecordingService.isServiceActive} " +
                "running=${RecordingService.isRunning} requested=$serviceStartRequested")
        if (RecordingService.isServiceActive || serviceStartRequested) {
            android.util.Log.w(traceTag, "3 startSafeBuffer ignored by duplicate guard")
            return
        }
        serviceStartRequested = true
        try {
            android.util.Log.i(traceTag, "4 calling startForegroundService")
            ContextCompat.startForegroundService(this, RecordingService.startIntent(this))
            android.util.Log.i(traceTag, "4 startForegroundService returned")
        } catch (e: Exception) {
            // Android 12+ 는 앱이 백그라운드면 포그라운드 서비스 시작을 거부한다.
            // 폴백인 startService 도 같은 이유로 막히므로, 실패를 삼키지 않고 알린다.
            android.util.Log.e("MainActivity", "녹음 서비스 시작 실패: ${e.message}", e)
            try {
                android.util.Log.i(traceTag, "4b calling startService fallback")
                startService(RecordingService.startIntent(this))
            } catch (e2: Exception) {
                serviceStartRequested = false
                android.util.Log.e("MainActivity", "폴백도 실패: ${e2.message}", e2)
                viewModel.setRecordingState(false)
                MaterialAlertDialogBuilder(this)
                    .setTitle("녹음을 시작하지 못했습니다")
                    .setMessage(
                        "화면을 켠 상태에서 앱을 다시 열어 주세요.\n\n" +
                        "계속 실패하면 설정에서 마이크 권한과 배터리 최적화 제외를 확인해 주세요."
                    )
                    .setPositiveButton("다시 시도") { _, _ -> checkPermissionsAndStart() }
                    .setNegativeButton("닫기", null)
                    .show()
                return
            }
        }

        // ★ 녹음이 시작된 뒤에 배터리 최적화 제외를 요청한다.
        //   먼저 띄우면 시스템 다이얼로그가 앱을 백그라운드로 밀어 서비스 시작이 막힌다.
        binding.root.postDelayed({
            if (!isFinishing) requestBatteryOptimizationExemption()
        }, 1500)
    }

    private fun stopSafeBuffer() {
        serviceStartRequested = false
        startService(RecordingService.stopIntent(this))
        viewModel.setRecordingState(false)
    }

    // ── 유틸리티 ─────────────────────────────────────────────────────────────

    /** 마이크 권한이 이미 부여되어 있으면 true */
    private fun hasRecordingPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * RecordingService의 정적 플래그로 서비스 실행 여부 확인.
     * deprecated된 getRunningServices() 대신 사용.
     */
    private fun isRecordingServiceRunning(): Boolean =
        RecordingService.isServiceActive

    /**
     * 서버 콜드 스타트 방지용 ping.
     * Render.com 무료 인스턴스는 15분 idle 후 슬립 → 첫 STT 요청이 오래 걸림.
     */
    private fun warmUpServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                java.net.URL("https://safebuffer-proxy-1.onrender.com/").readText()
            } catch (_: Exception) { /* 실패해도 무시 — STT 동작에는 영향 없음 */ }
        }
    }

    /**
     * 배터리 최적화 제외 요청.
     * 삼성/Xiaomi 등은 백그라운드 서비스를 공격적으로 종료함.
     * 이미 제외돼 있으면 아무것도 하지 않음 (매번 팝업 없음).
     */
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) return  // 이미 제외됨
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (e: Exception) {
            // 일부 기기에서 해당 인텐트 미지원 → 무시
            android.util.Log.w("MainActivity", "배터리 최적화 제외 요청 불가: ${e.message}")
        }
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("권한 필요")
            .setMessage("마이크 권한이 필요합니다.")
            .setPositiveButton("확인", null)
            .show()
    }

    /**
     * 실제 청크 시간이 아직 표시되지 않은 "플레이스홀더" 상태인지 확인.
     * - TEXT_WAITING / TEXT_STARTING / "" : 초기값
     * - "N초 녹음 중..." / "N분 녹음 중..." : 타이머 경과 시간 (청크 미저장 중)
     * allChunks가 실제 청크 텍스트를 표시하면 이 함수가 false를 반환해 덮어쓰기 허용.
     */
    private fun isPlaceholderText(text: String): Boolean =
        text == TEXT_WAITING || text == TEXT_STARTING || text == "" ||
        text.endsWith("초 녹음 중...") || text.endsWith("분 녹음 중...")

    /**
     * 녹음 경과 시간을 1초마다 표시.
     * RecordingService가 SharedPreferences에 저장한 시작 시각 기준으로 계산
     * → 앱 재시작 후에도 정확한 경과 시간 표시.
     * allChunks 컬렉터가 실제 청크 텍스트를 표시하면 자동으로 교체됨.
     */
    private fun startElapsedTimer() {
        elapsedTimerJob?.cancel()
        // SharedPreferences에서 녹음 시작 시각 읽기 (없으면 현재 시각)
        val startMs = getSharedPreferences(
            com.safebuffer.app.service.RecordingService.PREFS_NAME, MODE_PRIVATE
        ).getLong(
            com.safebuffer.app.service.RecordingService.KEY_RECORDING_START_MS,
            System.currentTimeMillis()
        )
        elapsedTimerJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                // 청크 기반 텍스트가 이미 표시 중이면 덮어쓰지 않음
                if (isPlaceholderText(binding.tvBufferStatus.text.toString())) {
                    val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).toInt()
                    val min = elapsedSec / 60
                    val sec = elapsedSec % 60
                    binding.tvBufferStatus.text =
                        if (min == 0) "${sec}초 녹음 중..." else "${min}분 녹음 중..."
                }
            }
        }
    }

    /**
     * 슬라이더 레이블 갱신 — 굵은 기간 텍스트 + 실제 HH:mm 시각 표시
     * @param minutes 슬라이더 값 (분 단위)
     */
    /**
     * 구간 길이 선택.
     *
     * 버튼은 "얼마나 긴 구간인지"만 정한다. 48시간 중 어디인지는 아래 스크롤바가 정한다.
     * 길이를 바꾸면 위치는 가장 최근(지금)으로 되돌린다.
     */
    /**
     * 파형에 담을 범위를 정한다.
     * 왼쪽 끝이 항상 '지금'이고 오른쪽 끝이 spanMinutes 만큼 과거다.
     */
    private fun selectSpan(spanMinutes: Int) {
        this.spanMinutes = spanMinutes

        val on  = android.content.res.ColorStateList.valueOf(0xFF6366F1.toInt())
        val off = android.content.res.ColorStateList.valueOf(0xFF22223A.toInt())
        binding.btnRange1h.backgroundTintList  = if (spanMinutes == 60) on else off
        binding.btnRange1d.backgroundTintList  = if (spanMinutes == 24 * 60) on else off
        binding.btnRangeAll.backgroundTintList = if (spanMinutes == 48 * 60) on else off

        // 왼쪽 끝이 과거 — 표시 범위 라벨은 왼쪽에 둔다 (오른쪽은 항상 "지금")
        binding.tvTimelineStart.text = when (spanMinutes) {
            60      -> "1시간 전"
            24 * 60 -> "24시간 전"
            else    -> "48시간 전"
        }

        binding.timelineView.setSelection(0f, 1f)   // 처음엔 범위 전체 선택
        refreshTimeline()
        applyRange()
        if (previewPlayer != null) stopPreview()
    }

    /** 파형 선택 → 라벨 갱신 (실제 시각은 selectedRangeMs() 가 매번 계산) */
    private fun applyRange() {
        val (fromMs, toMs) = selectedRangeMs()
        val hm = SimpleDateFormat("M/d HH:mm", Locale.KOREA)
        val lengthMin = ((toMs - fromMs) / 60_000f)
        binding.tvSliderLabel.text = formatDuration(lengthMin) + " 구간"
        binding.tvSliderTime.text  =
            "${hm.format(Date(fromMs))}  ~  ${hm.format(Date(toMs))}"
    }

    /**
     * 파형 데이터 채우기.
     * 녹음 중 저장해둔 진폭 요약을 시간축에 배분한다. 파일 디코딩은 하지 않는다.
     */
    private fun refreshTimeline() {
        val buckets = binding.timelineView.bucketCount()
        val now = System.currentTimeMillis()
        val spanMs = spanMinutes * 60_000L
        val oldest = now - spanMs

        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                val sum = IntArray(buckets); val cnt = IntArray(buckets)
                viewModel.chunksInRange(oldest, now).forEach { chunk ->
                    val amps = chunk.amplitudes?.split(',')
                        ?.mapNotNull { it.trim().toIntOrNull() } ?: return@forEach
                    if (amps.isEmpty()) return@forEach
                    val dur = (chunk.endTimeMs - chunk.startTimeMs).coerceAtLeast(1L)
                    amps.forEachIndexed { i, v ->
                        val t = chunk.startTimeMs + dur * i / amps.size
                        // 왼쪽=과거(oldest), 오른쪽=지금
                        val idx = (((t - oldest).toDouble() / spanMs) * buckets).toInt()
                        if (idx in 0 until buckets) { sum[idx] += v; cnt[idx]++ }
                    }
                }
                IntArray(buckets) { i -> if (cnt[i] > 0) sum[i] / cnt[i] else -1 }
            }
            binding.timelineView.setBuckets(data)
        }
    }

    /**
     * 선택 구간의 실제 시각. 화면의 선택 비율을 '지금' 기준으로 매번 새로 환산한다.
     * 왼쪽(0)=과거(now-span), 오른쪽(1)=지금.
     *   near(왼쪽 손잡이)=구간 시작(과거), far(오른쪽 손잡이)=구간 끝(지금 쪽).
     * ★ 호출 시점의 현재 시각을 쓰므로, 시간이 지난 뒤 같은 구간으로 저장해도
     *   그때의 최신 구간이 잡힌다 (옛 시각 고정으로 인한 덮어쓰기 방지).
     */
    private fun selectedRangeMs(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val spanMs = spanMinutes * 60_000L
        val fromMs = now - ((1f - binding.timelineView.selectionNear()) * spanMs).toLong()
        val toMs   = now - ((1f - binding.timelineView.selectionFar())  * spanMs).toLong()
        return fromMs to toMs
    }

    /**
     * 선택 구간 미리듣기.
     *
     * 저장하기 전에 "이 구간이 맞는지" 귀로 확인할 수 있어야 한다.
     * 구간은 보통 여러 청크에 걸쳐 있으므로 시간순으로 이어서 재생한다.
     * 지금 녹음 중인 부분까지 들으려면 먼저 flush 해서 DB에 반영해야 한다.
     */
    private fun togglePreview() {
        val mp = previewPlayer
        if (mp != null) {
            if (mp.isPlaying) {
                mp.pause()
                binding.btnPreview.text = "▶  이어서 듣기"
            } else {
                mp.start()
                binding.btnPreview.text = "⏸  일시정지"
            }
            return
        }

        val (fromMs, toMs) = selectedRangeMs()
        val requestId = nextSaveRequestId()
        android.util.Log.i("SaveFlow", "PREVIEW_CLICK requestId=$requestId from=$fromMs to=$toMs")
        previewFromMs = fromMs
        previewToMs = toMs
        binding.btnPreview.isEnabled = false
        binding.btnPreview.text = "준비 중..."

        lifecycleScope.launch {
            // 지금 녹음 중인 부분까지 포함하려면 먼저 저장해야 한다
            awaitFlushCompleted { RecordingService.flushIntent(this@MainActivity, requestId) }
            previewSnapshotReady = true

            val files = withContext(Dispatchers.IO) {
                viewModel.chunksInRange(fromMs, toMs)
                    .map { java.io.File(it.filePath) to it.startTimeMs }
                    .filter { it.first.exists() && it.first.length() > 1024L }
            }

            binding.btnPreview.isEnabled = true
            if (files.isEmpty()) {
                binding.btnPreview.text = "▶  이 구간 들어보기"
                Toast.makeText(
                    this@MainActivity,
                    "이 구간에 녹음이 없습니다. 구간을 넓혀 보세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            previewQueue = files
            previewIndex = 0
            playPreviewAt(0)
        }
    }

    private fun playPreviewAt(index: Int) {
        if (index >= previewQueue.size) { stopPreview(); return }
        previewIndex = index
        try {
            previewPlayer?.release()
            val (file, startMs) = previewQueue[index]
            previewChunkStartMs = startMs
            val mp = android.media.MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener {
                // 첫 파일은 선택 구간 시작점부터 — 파일이 구간보다 먼저 시작하면 그만큼 건너뛴다
                if (index == 0) {
                    val offset = previewFromMs - startMs
                    if (offset > 0) {
                        try { it.seekTo(offset.toInt()) } catch (_: Exception) {}
                    }
                }
                it.start(); startPreviewTicker()
            }
            mp.setOnCompletionListener { playPreviewAt(previewIndex + 1) }
            mp.setOnErrorListener { _, _, _ -> playPreviewAt(previewIndex + 1); true }
            mp.prepareAsync()
            previewPlayer = mp

            binding.btnPreview.text = "⏸  일시정지"
            binding.tvPreviewStatus.visibility = View.VISIBLE
            binding.tvPreviewStatus.text =
                "듣는 중  ${index + 1} / ${previewQueue.size}    ·  탭하면 멈춥니다"
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "미리듣기 실패: ${e.message}")
            stopPreview()
        }
    }

    /**
     * 재생 중 파형 위에 진행 위치를 표시한다.
     * 지금 듣고 있는 지점이 어디인지 보여서, 무음을 지나치는지 바로 알 수 있다.
     */
    private fun startPreviewTicker() {
        previewTickJob?.cancel()
        previewTickJob = lifecycleScope.launch {
            val spanMs = spanMinutes * 60_000L
            while (isActive) {
                val mp = previewPlayer ?: break
                val playingAt = try {
                    if (mp.isPlaying) previewChunkStartMs + mp.currentPosition else null
                } catch (e: Exception) { null }
                if (playingAt != null) {
                    // 선택 구간 끝(지금 쪽)에 닿으면 멈춘다
                    if (playingAt >= previewToMs) { stopPreview(); break }
                    // 파형 축: 왼쪽(0)=과거, 오른쪽(1)=지금.
                    //   재생은 오래된 소리부터(왼쪽) 시작해 지금(오른쪽)으로 흐른다.
                    //   ago(0~1)는 지금으로부터의 거리이므로 화면 위치는 (1 - ago).
                    val ago = (System.currentTimeMillis() - playingAt).toFloat() / spanMs
                    binding.timelineView.setPlayhead(1f - ago)
                }
                delay(200)
            }
        }
    }

    private fun stopPreview() {
        previewTickJob?.cancel(); previewTickJob = null
        binding.timelineView.setPlayhead(-1f)
        try { previewPlayer?.release() } catch (_: Exception) {}
        previewPlayer = null
        previewQueue = emptyList()
        previewIndex = 0
        binding.btnPreview.text = "▶  이 구간 들어보기"
        binding.tvPreviewStatus.visibility = View.GONE
    }

    private fun nextSaveRequestId(): Long {
        saveRequestSequence = maxOf(saveRequestSequence + 1, System.currentTimeMillis())
        return saveRequestSequence
    }


    /** 분 수 → 한국어 기간 ("30분", "2시간", "1시간 30분") */
    private fun formatDuration(minutes: Float): String {
        val totalMin = minutes.toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h == 0 -> "${m}분"
            m == 0 -> "${h}시간"
            else   -> "${h}시간 ${m}분"
        }
    }
}
