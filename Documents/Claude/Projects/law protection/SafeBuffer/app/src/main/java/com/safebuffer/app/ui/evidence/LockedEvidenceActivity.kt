package com.safebuffer.app.ui.evidence

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safebuffer.app.databinding.ActivityLockedEvidenceBinding
import com.safebuffer.app.ui.widget.WaveformView
import com.safebuffer.app.util.PdfReportGenerator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class LockedEvidenceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockedEvidenceBinding
    private val viewModel: LockedEvidenceViewModel by viewModels()
    private var mediaPlayer: MediaPlayer? = null

    private var playingPosition = -1   // 현재 재생/일시정지 중인 아이템
    private var isPaused = false       // 일시정지 상태

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer ?: return
            if (!mp.isPlaying || playingPosition < 0) return
            val dur = mp.duration.toFloat()
            if (dur > 0f) getWaveformView(playingPosition)?.setProgress(mp.currentPosition / dur)
            progressHandler.postDelayed(this, 80)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockedEvidenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val adapter = LockedEvidenceAdapter(
            onPlay = { evidence, position ->
                val mp = mediaPlayer

                when {
                    // ① 같은 아이템 → 재생 중이면 일시정지, 일시정지면 재개
                    position == playingPosition && mp != null -> {
                        if (mp.isPlaying) {
                            mp.pause()
                            isPaused = true
                            progressHandler.removeCallbacks(progressRunnable)
                            getPlayButton(position)?.text = "▶  계속"
                        } else {
                            mp.start()
                            isPaused = false
                            progressHandler.post(progressRunnable)
                            getPlayButton(position)?.text = "⏸  일시정지"
                        }
                    }

                    // ② 다른 아이템 → 기존 정지 후 새로 시작
                    else -> {
                        val file = File(evidence.chunk.filePath)
                        if (!file.exists()) {
                            Toast.makeText(this, "파일이 없습니다.", Toast.LENGTH_SHORT).show()
                            return@LockedEvidenceAdapter
                        }
                        // 기존 재생 중이면 버튼 초기화
                        if (playingPosition >= 0) {
                            getPlayButton(playingPosition)?.text = "▶  재생"
                            getWaveformView(playingPosition)?.setProgress(0f)
                        }
                        progressHandler.removeCallbacks(progressRunnable)
                        mp?.release()
                        mediaPlayer = null

                        // prepareAsync() — Main thread 블로킹 방지
                        val newMp = MediaPlayer()
                        newMp.setDataSource(file.absolutePath)
                        newMp.setOnPreparedListener { player ->
                            // position이 바뀌지 않은 경우만 재생 (사용자가 다른 버튼 누른 경우 skip)
                            if (playingPosition == position) {
                                player.start()
                                isPaused = false
                                getPlayButton(position)?.text = "⏸  일시정지"
                                progressHandler.post(progressRunnable)
                            } else {
                                player.release()
                                if (mediaPlayer == newMp) mediaPlayer = null
                            }
                        }
                        newMp.setOnCompletionListener {
                            getPlayButton(playingPosition)?.text = "▶  재생"
                            getWaveformView(playingPosition)?.setProgress(0f)
                            progressHandler.removeCallbacks(progressRunnable)
                            playingPosition = -1
                            isPaused = false
                        }
                        newMp.setOnErrorListener { _, _, _ ->
                            Toast.makeText(this, "재생 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            getPlayButton(position)?.text = "▶  재생"
                            progressHandler.removeCallbacks(progressRunnable)
                            playingPosition = -1; isPaused = false
                            true
                        }
                        newMp.prepareAsync()
                        mediaPlayer = newMp
                        playingPosition = position
                        isPaused = false
                        getPlayButton(position)?.text = "⏳  로딩..."
                    }
                }
            },
            onShareAudio = { evidence ->
                val file = File(evidence.chunk.filePath)
                if (!file.exists()) {
                    Toast.makeText(this, "녹음 파일이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@LockedEvidenceAdapter
                }
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "audio/mp4"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "SafeBuffer 녹음 파일")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "녹음 공유"
                ))
            },
            onExportPdf = { evidence ->
                lifecycleScope.launch {
                    Toast.makeText(this@LockedEvidenceActivity, "PDF 생성 중...", Toast.LENGTH_SHORT).show()
                    val file = withContext(Dispatchers.IO) {
                        PdfReportGenerator.generate(this@LockedEvidenceActivity, evidence)
                    }
                    val uri = FileProvider.getUriForFile(
                        this@LockedEvidenceActivity, "${packageName}.fileprovider", file
                    )
                    startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "SafeBuffer 증거 리포트")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "PDF 공유"
                    ))
                }
            },
            onDelete = { evidence ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("🗑 증거 삭제")
                    .setMessage("이 구간을 영구 삭제할까요? 복구할 수 없습니다.")
                    .setPositiveButton("삭제") { _, _ ->
                        if (playingPosition >= 0) {
                            mediaPlayer?.release(); mediaPlayer = null
                            progressHandler.removeCallbacks(progressRunnable)
                            playingPosition = -1; isPaused = false
                        }
                        viewModel.delete(evidence.chunk)
                        Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("취소", null).show()
            },
            onSeek = { position, fraction ->
                if (position == playingPosition) {
                    mediaPlayer?.let { mp ->
                        mp.seekTo((mp.duration * fraction).toInt())
                        getWaveformView(position)?.setProgress(fraction)
                    }
                }
            }
        )

        binding.rvEvidence.adapter = adapter

        lifecycleScope.launch {
            viewModel.evidenceList.collect { list ->
                adapter.submitList(list)
                binding.tvEmpty.visibility =
                    if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun getWaveformView(position: Int): WaveformView? =
        (binding.rvEvidence.findViewHolderForAdapterPosition(position) as? LockedEvidenceAdapter.VH)
            ?.binding?.waveformView

    private fun getPlayButton(position: Int): Button? =
        (binding.rvEvidence.findViewHolderForAdapterPosition(position) as? LockedEvidenceAdapter.VH)
            ?.binding?.btnPlay

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
