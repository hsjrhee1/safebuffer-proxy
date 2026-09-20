package com.safebuffer.app.ui.evidence

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safebuffer.app.data.repository.WhisperRepository
import com.safebuffer.app.databinding.ItemLockedEvidenceBinding
import com.safebuffer.app.util.AudioWaveformLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class LockedEvidenceAdapter(
    private val onPlay: (LockedEvidence, Int) -> Unit,
    private val onShareAudio: (LockedEvidence) -> Unit,
    private val onExportPdf: (LockedEvidence) -> Unit,
    private val onExportDocx: (LockedEvidence) -> Unit,
    private val onMakeTranscript: (LockedEvidence) -> Unit,
    private val onDelete: (LockedEvidence) -> Unit,
    private val onSeek: (Int, Float) -> Unit
) : ListAdapter<LockedEvidence, LockedEvidenceAdapter.VH>(DIFF) {

    private val dateFmt  = SimpleDateFormat("M월 d일 (E)", Locale.KOREA)
    private val timeFmt  = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    private val dayKeyFmt = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    // 파형 캐시: filePath → 진폭 리스트
    private val waveformCache = mutableMapOf<String, List<Float>>()

    // ★ Activity 가 관리하는 재생 상태 미러 — 뷰 재활용 시 버튼 텍스트 복원에 사용.
    //   이 값이 없으면 재활용된 ViewHolder 가 이전 항목의 "⏸ 일시정지" 텍스트를 그대로 물려받는다.
    var playingPosition: Int = -1
    var isPaused: Boolean = false

    // 대화 기록을 펼쳐 놓은 항목 (chunk id).
    // 목록에서는 4줄만 보이는데 전체를 볼 방법이 없어 "글이 나오다 마는" 상태였다.
    private val expandedIds = mutableSetOf<Long>()

    inner class VH(val binding: ItemLockedEvidenceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var loadJob: Job? = null

        fun bind(item: LockedEvidence, position: Int) {
            // 재활용 대비 — 항상 현재 재생 상태에 맞는 텍스트로 초기화
            binding.btnPlay.text = when {
                position != playingPosition -> "▶  재생"
                isPaused                    -> "▶  계속"
                else                        -> "⏸  일시정지"
            }

            val start = Date(item.chunk.startTimeMs)
            val end   = Date(item.chunk.endTimeMs)
            val sameDay = dayKeyFmt.format(start) == dayKeyFmt.format(end)
            // 날짜는 한 번만, 시간은 크게 — 같은 날이면 시각만, 날을 넘기면 끝에 날짜를 덧붙인다
            binding.tvDate.text = dateFmt.format(start)
            binding.tvTimeRange.text = if (sameDay)
                "${timeFmt.format(start)}  →  ${timeFmt.format(end)}"
            else
                "${timeFmt.format(start)}  →  ${dateFmt.format(end)} ${timeFmt.format(end)}"
            val durationSec = (item.chunk.endTimeMs - item.chunk.startTimeMs) / 1000
            val durationStr = if (durationSec >= 60) "${durationSec/60}분 ${durationSec%60}초" else "${durationSec}초"
            binding.tvDuration.text = durationStr
            // ★ 목록에서는 4줄만 보여주되, 탭하면 전체를 펼칠 수 있게 한다.
            val chunkId = item.chunk.id
            val expanded = expandedIds.contains(chunkId)
            binding.tvTranscript.maxLines = if (expanded) Int.MAX_VALUE else 4
            binding.tvTranscript.ellipsize =
                if (expanded) null else android.text.TextUtils.TruncateAt.END

            // 4줄을 넘는 경우에만 안내를 붙인다 — 짧은 글에 '더 보기'가 뜨면 혼란스럽다
            val lineCount = item.transcriptText.count { it == '\n' } + 1
            val needsExpand = lineCount > 4 || item.transcriptText.length > 120
            binding.tvTranscript.text = when {
                !needsExpand -> item.transcriptText
                expanded     -> item.transcriptText + "\n\n▲ 접기"
                else         -> item.transcriptText
            }

            binding.tvTranscript.setOnClickListener {
                if (!needsExpand) return@setOnClickListener
                if (expandedIds.contains(chunkId)) expandedIds.remove(chunkId)
                else expandedIds.add(chunkId)
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos)
            }

            // ★ 대화 기록 유무에 따라 내보내기 버튼이 달라진다.
            //   기록이 없어도 해시·시각이 담긴 '녹음 증명서'는 쓸모가 있으므로 남긴다.
            //   Word 는 대화 기록을 담는 문서라 기록이 있을 때만 의미가 있다.
            val hasTranscript = item.chunk.transcription?.let {
                it.isNotBlank() && it != "[]" && it != WhisperRepository.MARKER_SILENT
            } ?: false

            // ★ 종류를 한눈에 — '녹음 저장'만 한 것과 'AI 대화 기록'까지 만든 것을 구분
            if (hasTranscript) {
                binding.tvKindBadge.text = "📝  대화 기록 있음"
                binding.tvKindBadge.setTextColor(android.graphics.Color.parseColor("#22C55E"))
                binding.tvKindBadge.setBackgroundColor(
                    android.graphics.Color.parseColor("#14321F"))
            } else {
                binding.tvKindBadge.text = "🎧  녹음만"
                binding.tvKindBadge.setTextColor(android.graphics.Color.parseColor("#8C9AC0"))
                binding.tvKindBadge.setBackgroundColor(
                    android.graphics.Color.parseColor("#1E2438"))
            }

            // ★ 대화 기록이 없으면 문서를 내보내지 않는다.
            //   해시와 시각만 담긴 문서는 사실상 백지여서, 눌러본 사용자에게는
            //   기능이 고장 난 것으로만 보인다. 대신 기록을 만드는 길만 남긴다.
            if (hasTranscript) {
                binding.btnExportPdf.visibility = android.view.View.VISIBLE
                binding.btnExportPdf.text = "📄 PDF"
                binding.btnExportPdf.setOnClickListener { onExportPdf(item) }

                binding.btnExportDocx.text = "📝 Word"
                binding.btnExportDocx.setOnClickListener { onExportDocx(item) }
            } else {
                binding.btnExportPdf.visibility = android.view.View.GONE

                binding.btnExportDocx.text = "✨ 대화 기록"
                binding.btnExportDocx.setOnClickListener { onMakeTranscript(item) }
            }

            // SHA-256 무결성 배지 — 서버 타임스탬프 있으면 더 강조
            binding.tvHashBadge.visibility = if (item.chunk.fileHash != null) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            if (item.chunk.serverTimestamp != null) {
                binding.tvHashBadge.text = "✓ SHA-256 + 서버 타임스탬프 — 원본 무결성 보증"
                binding.tvHashBadge.setTextColor(android.graphics.Color.parseColor("#818CF8"))
            } else {
                binding.tvHashBadge.text = "✓ SHA-256 무결성 확인됨"
                binding.tvHashBadge.setTextColor(android.graphics.Color.parseColor("#22C55E"))
            }

            binding.btnPlay.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onPlay(item, pos)
            }
            binding.btnShareAudio.setOnClickListener  { onShareAudio(item) }
            binding.btnDelete.setOnClickListener      { onDelete(item) }
            // btnExportPdf / btnExportDocx 의 동작은 대화 기록 유무에 따라 위에서 설정한다

            // 파형 초기화 — 재생 중인 항목이 아니면 0으로
            if (position != playingPosition) binding.waveformView.setProgress(0f)
            binding.waveformView.setOnSeekListener { frac ->
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onSeek(pos, frac)
            }

            // 파형 로드 (캐시 우선)
            loadJob?.cancel()
            val cached = waveformCache[item.chunk.filePath]
            if (cached != null) {
                binding.waveformView.setAmplitudes(cached)
            } else {
                binding.waveformView.setAmplitudes(emptyList())
                loadJob = scope.launch {
                    val amps = withContext(Dispatchers.IO) {
                        AudioWaveformLoader.load(item.chunk.filePath)
                    }
                    waveformCache[item.chunk.filePath] = amps
                    binding.waveformView.setAmplitudes(amps)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemLockedEvidenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position), position)

    override fun onViewRecycled(holder: VH) {
        holder.loadJob?.cancel()
        holder.loadJob = null
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        scope.cancel()
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<LockedEvidence>() {
            override fun areItemsTheSame(a: LockedEvidence, b: LockedEvidence) =
                a.chunk.id == b.chunk.id
            override fun areContentsTheSame(a: LockedEvidence, b: LockedEvidence) = a == b
        }
    }
}
