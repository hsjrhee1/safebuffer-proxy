package com.safebuffer.app.ui

import android.Manifest
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safebuffer.app.R
import com.safebuffer.app.databinding.ActivityMainBinding
import com.safebuffer.app.service.RecordingService
import com.safebuffer.app.ui.evidence.LockedEvidenceActivity
import com.safebuffer.app.ui.settings.SettingsActivity
import com.safebuffer.app.ui.transcript.TranscriptActivity
import com.safebuffer.app.worker.CleanupWorker
import com.safebuffer.app.util.TrialManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var trialManager: TrialManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) startSafeBuffer()
        else showPermissionDeniedDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        trialManager.initIfNeeded()
        CleanupWorker.schedule(this)
        setupUI()
        observeViewModel()
        warmUpServer()
        // 서비스가 이미 실행 중이면 UI 동기화 (재부팅 후 자동시작 케이스)
        if (isRecordingServiceRunning()) {
            viewModel.setRecordingState(true)
        }
        // 앱 시작 시 자동 녹음
        if (!viewModel.isRecording.value) {
            checkPermissionsAndStart()
        }
    }

    private fun setupUI() {
        // 시작/중지
        binding.btnToggleRecording.setOnClickListener {
            if (viewModel.isRecording.value) stopSafeBuffer()
            else checkPermissionsAndStart()
        }

        // ★ 대화 내용 확인 → 현재 청크 즉시 저장 후 TranscriptActivity
        binding.btnViewTranscript.setOnClickListener {
            // 현재 녹음 중인 청크를 DB에 즉시 저장
            startService(RecordingService.flushIntent(this))
            // DB 저장 완료 대기 후 화면 전환 (1.5초)
            binding.btnViewTranscript.isEnabled = false
            binding.btnViewTranscript.text = "저장 중..."
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1500)
                showHoursPickerDialog()
                binding.btnViewTranscript.isEnabled = true
                binding.btnViewTranscript.text = "🎙 대화 내용 확인"
            }
        }

        // 잠긴 증거 목록
        binding.tvLockedCount.setOnClickListener {
            startActivity(Intent(this, LockedEvidenceActivity::class.java))
        }

        // 설정
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 빠른 잠금 슬라이더
        binding.sliderHoursBack.apply {
            valueFrom = 0.5f
            valueTo = 48f
            stepSize = 0.5f
            value = 2f
            addOnChangeListener { _, value, _ ->
                binding.tvSliderLabel.text = formatHours(value)
            }
        }
        binding.tvSliderLabel.text = formatHours(2f)

        binding.btnLock.setOnClickListener {
            val hoursBack = binding.sliderHoursBack.value
            MaterialAlertDialogBuilder(this)
                .setTitle("🔒 빠른 잠금")
                .setMessage("지금부터 ${formatHours(hoursBack)} 전까지를 저장할까요?")
                .setPositiveButton("저장") { _, _ -> viewModel.lockRange(hoursBack) }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun showHoursPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_time_range, null)
        val dialog = Dialog(this, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog)
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // 날짜 오프셋 (0=오늘, 1=어제, 2=그제)
        var dayOffset = 0

        // 기본 시작/종료: 1시간 전 ~ 지금
        val cal = java.util.Calendar.getInstance()
        var endHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        var endMin = cal.get(java.util.Calendar.MINUTE)
        cal.add(java.util.Calendar.HOUR_OF_DAY, -1)
        var startHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        var startMin = cal.get(java.util.Calendar.MINUTE)

        val btnStart = dialogView.findViewById<Button>(R.id.btnStartTime)
        val btnEnd = dialogView.findViewById<Button>(R.id.btnEndTime)
        val tvSummary = dialogView.findViewById<TextView>(R.id.tvRangeSummary)

        fun fmt(h: Int, m: Int) = "%02d:%02d".format(h, m)

        fun updateSummary() {
            val diffMin = endHour * 60 + endMin - (startHour * 60 + startMin)
            val label = when {
                diffMin <= 0 -> "⚠️ 종료 시간을 시작 이후로 설정하세요"
                diffMin < 60 -> "${diffMin}분 구간"
                diffMin % 60 == 0 -> "${diffMin / 60}시간 구간"
                else -> "${diffMin / 60}시간 ${diffMin % 60}분 구간"
            }
            tvSummary.text = label
        }

        btnStart.text = fmt(startHour, startMin)
        btnEnd.text = fmt(endHour, endMin)
        updateSummary()

        btnStart.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                startHour = h; startMin = m
                btnStart.text = fmt(h, m)
                updateSummary()
            }, startHour, startMin, true).show()
        }

        btnEnd.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                endHour = h; endMin = m
                btnEnd.text = fmt(h, m)
                updateSummary()
            }, endHour, endMin, true).show()
        }

        // 날짜 칩
        val chipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupDate)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            dayOffset = when (checkedIds.firstOrNull()) {
                R.id.chipYesterday -> 1
                R.id.chipDayBefore -> 2
                else -> 0
            }
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val baseCal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -dayOffset)
                set(java.util.Calendar.HOUR_OF_DAY, startHour)
                set(java.util.Calendar.MINUTE, startMin)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val fromMs = baseCal.timeInMillis
            baseCal.set(java.util.Calendar.HOUR_OF_DAY, endHour)
            baseCal.set(java.util.Calendar.MINUTE, endMin)
            val toMs = baseCal.timeInMillis

            if (toMs <= fromMs) {
                android.widget.Toast.makeText(this, "종료 시간이 시작 시간보다 늦어야 합니다.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            startActivity(TranscriptActivity.newIntent(this, fromMs, toMs))
        }

        dialog.show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isRecording.collect { isRecording ->
                updateRecordingUI(isRecording)
            }
        }

        lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is UiEvent.LockSuccess -> {
                        Toast.makeText(this@MainActivity, "구간이 잠겼습니다.", Toast.LENGTH_SHORT).show()
                    }
                    is UiEvent.Error -> {
                        Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.allChunks.collect { chunks ->
                // 잠기지 않은 롤링 버퍼만 실제 타임스탬프로 계산
                val buffer = chunks.filter { !it.isLocked }
                val text = if (buffer.isEmpty()) {
                    "보호 중: 0분"
                } else {
                    val oldestMs = buffer.minOf { it.startTimeMs }
                    val newestMs = buffer.maxOf { it.endTimeMs }
                    val totalMin = ((newestMs - oldestMs) / 60_000L).toInt().coerceAtLeast(0)
                    when {
                        totalMin == 0 -> "보호 중: 1분 미만"
                        totalMin < 60 -> "보호 중: ${totalMin}분"
                        else -> "보호 중: ${totalMin / 60}시간 ${totalMin % 60}분"
                    }
                }
                binding.tvBufferStatus.text = text
            }
        }

        lifecycleScope.launch {
            viewModel.lockedChunks.collect { locked ->
                binding.tvLockedCount.text = "🔒 잠긴 증거: ${locked.size}개 구간"
            }
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        if (isRecording) {
            // 버튼
            binding.btnToggleRecording.text = "⏹  녹음 중지"
            binding.btnToggleRecording.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0x33_33_33_FF.toInt())

            // 상태 카드
            binding.tvRecordingStatus.text = "🔴  녹음 중"
            binding.tvRecordingStatus.setTextColor(android.graphics.Color.parseColor("#FF4444"))

            // 맥박 애니메이션
            binding.viewRecordingDot.setBackgroundResource(R.drawable.bg_recording_circle)
            binding.viewPulseRing.visibility = View.VISIBLE
            val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.viewPulseRing.startAnimation(pulseAnim)
            binding.ivMicIcon.clearColorFilter()

            // 패널 표시
            binding.btnViewTranscript.visibility = View.VISIBLE
            binding.lockPanel.visibility = View.VISIBLE
        } else {
            // 버튼
            binding.btnToggleRecording.text = "▶  SafeBuffer 시작"
            binding.btnToggleRecording.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1A2A4A"))

            // 상태 카드
            binding.tvRecordingStatus.text = "대기 중"
            binding.tvRecordingStatus.setTextColor(android.graphics.Color.parseColor("#666688"))

            // 인디케이터 초기화
            binding.viewRecordingDot.setBackgroundResource(R.drawable.bg_idle_circle)
            binding.viewPulseRing.clearAnimation()
            binding.viewPulseRing.visibility = View.GONE
            binding.ivMicIcon.setColorFilter(android.graphics.Color.parseColor("#AAAAAA"))

            // 녹음 중지 후에도 기존 버퍼 있으면 대화 확인 가능
            binding.btnViewTranscript.visibility = View.VISIBLE
            binding.lockPanel.visibility = View.GONE
        }
    }

    private fun checkPermissionsAndStart() {
        val perms = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (perms.isEmpty()) startSafeBuffer()
        else permissionLauncher.launch(perms.toTypedArray())
    }

    private fun startSafeBuffer() {
        ContextCompat.startForegroundService(this, RecordingService.startIntent(this))
        viewModel.setRecordingState(true)
    }

    private fun stopSafeBuffer() {
        startService(RecordingService.stopIntent(this))
        viewModel.setRecordingState(false)
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("권한 필요")
            .setMessage("마이크 권한이 필요합니다.")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun isRecordingServiceRunning(): Boolean {
        // getRunningServices deprecated API 26+, RecordingService static 플래그로 대체
        return com.safebuffer.app.service.RecordingService.isRunning
    }

    private fun warmUpServer() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                java.net.URL("https://safebuffer-proxy.onrender.com/").readText()
            } catch (e: Exception) { /* 무시 */ }
        }
    }

    private fun formatHours(hours: Float): String {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        return when {
            h == 0 -> "${m}분 전"
            m == 0 -> "${h}시간 전"
            else -> "${h}시간 ${m}분 전"
        }
    }
}
