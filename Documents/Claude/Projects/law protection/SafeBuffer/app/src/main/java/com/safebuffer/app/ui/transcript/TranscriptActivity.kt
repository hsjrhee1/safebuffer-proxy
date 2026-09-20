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
import com.safebuffer.app.ui.settings.SettingsActivity
import com.safebuffer.app.util.TierManager
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

    @javax.inject.Inject lateinit var tierManager: TierManager

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranscriptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ★ 상단 상태바 / 하단 제스처바에 헤더·저장 버튼이 가리지 않도록 인셋만큼 패딩
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.transcriptRoot) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        val fromMs = intent.getLongExtra(EXTRA_FROM_MS, 0L)
        val toMs   = intent.getLongExtra(EXTRA_TO_MS, System.currentTimeMillis())

        setupRecyclerView()
        setupUI(fromMs, toMs)
        observeViewModel()

        // ★ STT는 유료. 정식 버전을 사지 않았으면 전사하지 않고 구매를 안내한다.
        //   정식 버전이 있으면 크레딧(맛보기 1시간 포함)에서 차감하며 전사한다.
        //   (크레딧이 0이면 transcribeRange 가 QuotaExceeded 로 안내한다.)
        if (!tierManager.isPremium()) {
            showPurchaseNeededDialog()
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
                        // ★ 결과가 0건일 때 빈 화면만 보이면 녹음 실패인지 대화가 없었는지
                        //   사용자가 구분할 수 없다. 이유를 명시한다.
                        if (viewModel.segments.value.isEmpty()) {
                            binding.rvTranscript.visibility = View.GONE
                            binding.tvLoadingMsg.visibility = View.VISIBLE
                            binding.tvLoadingMsg.text =
                                "이 구간에서 인식된 대화가 없습니다.\n\n" +
                                "· 녹음이 아직 10분이 되지 않았다면 첫 저장 전일 수 있습니다\n" +
                                "· 주변이 조용했거나 소리가 작으면 인식되지 않습니다\n" +
                                "· 마이크가 다른 앱(통화 등)에 사용 중이었을 수 있습니다"
                        } else {
                            binding.tvLoadingMsg.visibility = View.GONE
                            binding.rvTranscript.visibility = View.VISIBLE
                        }
                    }
                    is TranscriptViewModel.UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvLoadingMsg.text = "오류: ${state.message}"
                        binding.tvLoadingMsg.visibility = View.VISIBLE
                    }
                    is TranscriptViewModel.UiState.QuotaExceeded -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvLoadingMsg.visibility = View.GONE
                        val msg = "AI 대화 기록에 쓸 시간이 부족합니다.\n" +
                            "남은 시간: ${state.purchasedRemaining}\n\n" +
                            "설정에서 'AI 대화 기록 5시간(12,900원)'을 구매하면 계속 사용할 수 있습니다."
                        MaterialAlertDialogBuilder(this@TranscriptActivity)
                            .setTitle("변환 시간이 부족합니다")
                            .setMessage(msg)
                            .setPositiveButton("설정으로") { _, _ ->
                                startActivity(Intent(
                                    this@TranscriptActivity, SettingsActivity::class.java
                                ))
                            }
                            .setNegativeButton("확인", null)
                            .show()
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
                    .setTitle("저장했습니다")
                    .setMessage("$from ~ $to 구간을 보관했습니다.\n48시간이 지나도 삭제되지 않습니다.")
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
            .setTitle("이 구간을 남겨둘까요?")
            .setMessage("구간: $from ~ $to\n\n$preview")
            .setPositiveButton("저장") { _, _ ->
                viewModel.lockSelectedRange()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showPurchaseNeededDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("정식 버전이 필요합니다")
            .setMessage(
                "AI 대화 기록은 정식 버전(5,500원, 한 번만) 기능입니다.\n\n" +
                "정식 버전을 구매하면 맛보기 1시간이 포함되고, 이후 'AI 대화 기록 5시간(12,900원)'을 추가로 구매할 수 있습니다."
            )
            .setPositiveButton("정식 버전 구매") { _, _ ->
                startActivity(Intent(
                    this@TranscriptActivity, SettingsActivity::class.java
                ))
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
