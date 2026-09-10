package com.safebuffer.app.ui.transcript

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safebuffer.app.databinding.ActivityTranscriptBinding
import com.safebuffer.app.util.TrialManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 대화 타임라인 화면
 *
 * [사용 흐름]
 * 1. MainActivity에서 "대화 내용 확인" 버튼 → 이 화면 실행
 * 2. Whisper STT로 해당 구간 전사
 * 3. 타임라인 목록에서 탭으로 시작/끝 구간 선택 (빨간색 하이라이트)
 * 4. "이 구간 잠금" 버튼 → 해당 청크 영구 보존
 */
@AndroidEntryPoint
class TranscriptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTranscriptBinding
    private val viewModel: TranscriptViewModel by viewModels()
    private lateinit var adapter: TranscriptAdapter

    @javax.inject.Inject lateinit var trialManager: TrialManager

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranscriptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fromMs = intent.getLongExtra(EXTRA_FROM_MS, 0L)
        val toMs   = intent.getLongExtra(EXTRA_TO_MS, System.currentTimeMillis())

        setupRecyclerView()
        setupUI(fromMs, toMs)
        observeViewModel()

        // 체험 만료 확인 후 전사 시작
        if (trialManager.isExpired()) {
            showTrialExpiredDialog()
        } else {
            viewModel.transcribeRange(fromMs, toMs)
        }
    }

    private fun setupRecyclerView() {
        adapter = TranscriptAdapter { segmentId ->
            viewModel.onSegmentTapped(segmentId)
        }
        binding.rvTranscript.apply {
            layoutManager = LinearLayoutManager(this@TranscriptActivity)
            adapter = this@TranscriptActivity.adapter
        }
    }

    private fun setupUI(fromMs: Long, toMs: Long) {
        binding.tvTimeRange.text =
            "${timeFmt.format(Date(fromMs))}  →  ${timeFmt.format(Date(toMs))}"

        binding.tvInstruction.text =
            "탭으로 시작 지점을 선택하고,\n다시 탭으로 끝 지점을 선택하세요."

        // 잠금 버튼
        binding.btnLockSelected.setOnClickListener {
            showLockConfirmDialog()
        }
        binding.btnLockSelected.isEnabled = false

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is TranscriptViewModel.UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.tvLoadingMsg.visibility = View.VISIBLE
                        binding.tvLoadingMsg.text = "Whisper AI가 대화를 분석 중입니다...\n(1-2분 소요)"
                        binding.rvTranscript.visibility = View.GONE
                    }
                    is TranscriptViewModel.UiState.Done -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvLoadingMsg.visibility = View.GONE
                        binding.rvTranscript.visibility = View.VISIBLE
                    }
                    is TranscriptViewModel.UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvLoadingMsg.text = "오류: ${state.message}"
                        binding.tvLoadingMsg.visibility = View.VISIBLE
                    }
                    else -> {}
                }
            }
        }

        // 세그먼트 목록 업데이트
        lifecycleScope.launch {
            viewModel.segments.collect { segments ->
                adapter.submitList(segments.toList())

                // 선택 구간 요약 표시
                val selected = segments.filter { it.isSelected }
                if (selected.isNotEmpty()) {
                    val startTime = timeFmt.format(Date(selected.first().absoluteStartMs))
                    val endTime   = timeFmt.format(Date(selected.last().absoluteEndMs))
                    binding.tvSelectionSummary.text = "선택 구간: $startTime ~ $endTime"
                    binding.tvSelectionSummary.visibility = View.VISIBLE
                } else {
                    binding.tvSelectionSummary.visibility = View.GONE
                }
            }
        }

        // 잠금 버튼 활성화
        lifecycleScope.launch {
            viewModel.hasSelection.collect { has ->
                binding.btnLockSelected.isEnabled = has
            }
        }

        // 잠금 완료 이벤트
        lifecycleScope.launch {
            viewModel.lockEvent.collect { event ->
                val from = timeFmt.format(Date(event.fromMs))
                val to   = timeFmt.format(Date(event.toMs))
                MaterialAlertDialogBuilder(this@TranscriptActivity)
                    .setTitle("✅ 증거 저장 완료")
                    .setMessage("$from ~ $to 구간이\n48시간이 지나도 삭제되지 않도록 잠겼습니다.")
                    .setPositiveButton("확인") { _, _ -> finish() }
                    .show()
            }
        }
    }

    private fun showLockConfirmDialog() {
        val selected = viewModel.segments.value.filter { it.isSelected }
        if (selected.isEmpty()) return

        val from = timeFmt.format(Date(selected.first().absoluteStartMs))
        val to   = timeFmt.format(Date(selected.last().absoluteEndMs))
        val preview = selected.joinToString("\n") { "\"${it.text}\"" }
            .take(300)

        MaterialAlertDialogBuilder(this)
            .setTitle("🔒 이 구간을 증거로 저장할까요?")
            .setMessage("구간: $from ~ $to\n\n$preview")
            .setPositiveButton("저장") { _, _ ->
                viewModel.lockSelectedRange()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showTrialExpiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("체험 기간 종료")
            .setMessage("3일 무료 체험이 종료되었습니다.\n\n정식 버전으로 업그레이드하면 대화 내용 확인 및 증거 잠금 기능을 계속 사용할 수 있습니다.")
            .setPositiveButton("업그레이드") { _, _ ->
                // TODO: 인앱 결제 연동
                finish()
            }
            .setNegativeButton("닫기") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val EXTRA_FROM_MS = "extra_from_ms"
        private const val EXTRA_TO_MS   = "extra_to_ms"

        fun newIntent(context: Context, fromMs: Long, toMs: Long): Intent =
            Intent(context, TranscriptActivity::class.java).apply {
                putExtra(EXTRA_FROM_MS, fromMs)
                putExtra(EXTRA_TO_MS, toMs)
            }
    }
}
