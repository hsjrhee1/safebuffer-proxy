package com.safebuffer.app.ui.evidence

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
    private val onDelete: (LockedEvidence) -> Unit,
    private val onSeek: (Int, Float) -> Unit
) : ListAdapter<LockedEvidence, LockedEvidenceAdapter.VH>(DIFF) {

    private val fmt = SimpleDateFormat("MM/dd HH:mm:ss", Locale.KOREA)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    // 파형 캐시: filePath → 진폭 리스트
    private val waveformCache = mutableMapOf<String, List<Float>>()

    inner class VH(val binding: ItemLockedEvidenceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var loadJob: Job? = null

        fun bind(item: LockedEvidence) {
            val from = fmt.format(Date(item.chunk.startTimeMs))
            val to   = fmt.format(Date(item.chunk.endTimeMs))
            binding.tvTimeRange.text  = "$from → $to"
            binding.tvTranscript.text = item.transcriptText

            binding.btnPlay.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onPlay(item, pos)
            }
            binding.btnShareAudio.setOnClickListener { onShareAudio(item) }
            binding.btnExportPdf.setOnClickListener  { onExportPdf(item) }
            binding.btnDelete.setOnClickListener     { onDelete(item) }

            // 파형 초기화
            binding.waveformView.setProgress(0f)
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

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

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
