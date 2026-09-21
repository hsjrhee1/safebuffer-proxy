package com.safebuffer.app.ui.evidence

import android.content.Context
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
import com.safebuffer.app.data.local.ChunkEntity
import com.safebuffer.app.ui.widget.WaveformView
import com.safebuffer.app.util.DocxReportGenerator
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
    private var playbackChunks: List<ChunkEntity> = emptyList()
    private var playbackChunkIndex = 0
    private var pendingSeekMs = 0
    private var playbackLogicalStartMs = 0L
    private var playbackLogicalEndMs = 0L

    private var evidenceAdapter: LockedEvidenceAdapter? = null

    // ★ 값이 바뀔 때마다 어댑터에 그대로 전달 — 뷰 재활용 시 버튼 텍스트가
    //   실제 재생 상태와 어긋나지 않도록 단일 출처를 유지한다.
    private var playingPosition = -1   // 현재 재생/일시정지 중인 아이템
        set(value) { field = value; evidenceAdapter?.playingPosition = value }

    private var isPaused = false       // 일시정지 상태
        set(value) { field = value; evidenceAdapter?.isPaused = value }

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer ?: return
            if (!mp.isPlaying || playingPosition < 0) return
            val playingAtMs = playbackChunks[playbackChunkIndex].startTimeMs + mp.currentPosition
            if (playingAtMs >= playbackLogicalEndMs) {
                finishLogicalPlayback(playingPosition)
                return
            }
            val total = playbackLogicalEndMs - playbackLogicalStartMs
            if (total > 0L) getWaveformView(playingPosition)?.setProgress(
                ((playingAtMs - playbackLogicalStartMs).toFloat() / total).coerceIn(0f, 1f)
            )
            // Poll normally at 80 ms, but schedule the terminal check at the
            // logical boundary instead of allowing a full extra poll interval.
            val untilLogicalEndMs = (playbackLogicalEndMs - playingAtMs).coerceAtLeast(1L)
            progressHandler.postDelayed(this, minOf(80L, untilLogicalEndMs))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockedEvidenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ★ 상단 상태바에 헤더가 가리지 않게, 하단 제스처바에 마지막 항목이 가리지 않게
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.evidenceRoot) { _, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.evidenceRoot.setPadding(0, bars.top, 0, 0)
            binding.rvEvidence.setPadding(
                binding.rvEvidence.paddingLeft, binding.rvEvidence.paddingTop,
                binding.rvEvidence.paddingRight, bars.bottom + 12
            )
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        // ★ 전체를 하나의 문서로 — 청크별 PDF 는 대화의 일부만 담긴다
        binding.btnExportAll.setOnClickListener {
            val items = viewModel.evidenceList.value
            if (items.isEmpty()) {
                Toast.makeText(this, "보관된 녹음이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // ★ 하나도 변환되지 않았으면 백지 문서가 나온다 — 만들지 않는다
            val anyTranscript = items.any { ev ->
                ev.chunk.transcription?.let {
                    it.isNotBlank() && it != "[]" && it != "__SILENT__"
                } ?: false
            }
            if (!anyTranscript) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("대화 기록이 없습니다")
                    .setMessage(
                        "문서로 내보내려면 먼저 대화 기록을 만들어야 합니다.\n\n" +
                        "각 녹음의 [대화 기록 만들기] 버튼을 눌러 주세요."
                    )
                    .setPositiveButton("확인", null)
                    .show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                Toast.makeText(
                    this@LockedEvidenceActivity,
                    "전체 문서 만드는 중... (${items.size}건)", Toast.LENGTH_SHORT
                ).show()
                try {
                    val file = withContext(Dispatchers.IO) {
                        PdfReportGenerator.generateAll(this@LockedEvidenceActivity, items)
                    }
                    val uri = FileProvider.getUriForFile(
                        this@LockedEvidenceActivity, "${packageName}.fileprovider", file
                    )
                    startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "SafeBuffer 녹음 기록 전체")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "전체 PDF 공유"
                    ))
                } catch (e: Exception) {
                    android.util.Log.e("LockedEvidence", "전체 PDF 실패: ${e.message}", e)
                    Toast.makeText(
                        this@LockedEvidenceActivity,
                        "전체 PDF 생성 실패: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

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
                        val playable = evidence.physicalChunks.sortedBy { it.startTimeMs }
                            .filter { File(it.filePath).exists() }
                        if (playable.isEmpty()) {
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
                        playbackChunks = playable
                        playbackChunkIndex = 0
                        playbackLogicalStartMs = evidence.chunk.startTimeMs
                        playbackLogicalEndMs = evidence.chunk.endTimeMs
                        pendingSeekMs = (playbackLogicalStartMs - playable.first().startTimeMs)
                            .coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                        android.util.Log.i("SaveFlow",
                            "PLAY_GROUP saveGroupId=${evidence.chunk.saveGroupId} " +
                                "logicalStartMs=$playbackLogicalStartMs " +
                                "logicalEndMs=$playbackLogicalEndMs " +
                                "firstChunkStartMs=${playable.first().startTimeMs} " +
                                "initialOffsetMs=$pendingSeekMs")
                        playingPosition = position
                        isPaused = false
                        getPlayButton(position)?.text = "⏳  로딩..."
                        playPhysicalChunk(position, 0)
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
                    try {
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
                    } catch (e: Exception) {
                        android.util.Log.e("LockedEvidence", "PDF 생성 실패: ${e.message}", e)
                        Toast.makeText(
                            this@LockedEvidenceActivity,
                            "PDF 생성 실패: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onExportDocx = { evidence ->
                lifecycleScope.launch {
                    Toast.makeText(this@LockedEvidenceActivity, "Word 문서 생성 중...", Toast.LENGTH_SHORT).show()
                    try {
                        val file = withContext(Dispatchers.IO) {
                            DocxReportGenerator.generate(this@LockedEvidenceActivity, evidence)
                        }
                        val uri = FileProvider.getUriForFile(
                            this@LockedEvidenceActivity, "${packageName}.fileprovider", file
                        )
                        startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "SafeBuffer 증거 리포트")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, "Word 공유"
                        ))
                    } catch (e: Exception) {
                        android.util.Log.e("LockedEvidence", "Word 생성 실패: ${e.message}", e)
                        Toast.makeText(
                            this@LockedEvidenceActivity,
                            "Word 문서 생성 실패: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onMakeTranscript = { evidence ->
                // ★ 이 구간만 대화 기록으로 변환 — 홈으로 나가 구간을 다시 고를 필요가 없다
                startActivity(
                    com.safebuffer.app.ui.transcript.TranscriptActivity.newIntent(
                        this,
                        evidence.chunk.startTimeMs,
                        evidence.chunk.endTimeMs
                    )
                )
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
                        viewModel.delete(evidence)
                        Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("취소", null).show()
            },
            onSeek = { position, fraction ->
                if (position == playingPosition) {
                    val total = (playbackLogicalEndMs - playbackLogicalStartMs).coerceAtLeast(0L)
                    val targetAbsolute = playbackLogicalStartMs +
                        (total * fraction.coerceIn(0f, 1f)).toLong()
                    val index = playbackChunks.indexOfFirst {
                        targetAbsolute >= it.startTimeMs && targetAbsolute <= it.endTimeMs
                    }.takeIf { it >= 0 } ?: playbackChunks.lastIndex
                    playbackChunkIndex = index
                    pendingSeekMs = (targetAbsolute - playbackChunks[index].startTimeMs)
                        .coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    playPhysicalChunk(position, index)
                    getWaveformView(position)?.setProgress(fraction)
                }
            }
        )

        evidenceAdapter = adapter
        // 프로퍼티 setter 가 어댑터에 값을 전달할 수 있도록 현재 상태를 한 번 동기화
        adapter.playingPosition = playingPosition
        adapter.isPaused = isPaused

        binding.rvEvidence.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvEvidence.adapter = adapter

        // ★ 무료와 유료를 섞지 않는다. 화면마다 보여주는 것이 다르다.
        //   JUST_SAVED — 방금 저장한 구간 (저장 직후 확인용)
        //   AUDIO      — 녹음 확인하기 (무료). 아직 글로 옮기지 않은 녹음
        //   TRANSCRIPT — AI 대화기록 보관함 (유료). 글로 옮긴 것
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_AUDIO
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lastFrom = prefs.getLong(KEY_LAST_FROM, 0L)
        val lastTo   = prefs.getLong(KEY_LAST_TO, 0L)
        val hasLast  = lastFrom > 0L && lastTo > 0L

        when (mode) {
            MODE_JUST_SAVED -> {
                binding.tvTitle.text = "방금 저장한 녹음"
                binding.tvEmpty.text = "저장된 구간을 찾을 수 없습니다."
                binding.btnExportAll.visibility = android.view.View.GONE
            }
            MODE_TRANSCRIPT -> {
                binding.tvTitle.text = "AI 대화기록 보관함"
                binding.tvEmpty.text =
                    "아직 글로 옮긴 기록이 없습니다.\n'녹음 확인하기'에서 만들어 보세요."
            }
            else -> {
                binding.tvTitle.text = "녹음 확인하기"
                binding.tvEmpty.text =
                    "저장한 녹음이 없습니다.\n홈에서 구간을 골라 저장해 보세요."
                binding.btnExportAll.visibility = android.view.View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.evidenceList.collect { all ->
                fun inLast(e: LockedEvidence) =
                    hasLast && e.chunk.startTimeMs <= lastTo && e.chunk.endTimeMs >= lastFrom
                fun hasText(e: LockedEvidence) = e.chunk.transcription?.let {
                    it.isNotBlank() && it != "[]" && it != "__SILENT__"
                } ?: false

                val list = when (mode) {
                    // 저장 직후 확인용 — 방금 저장한 구간만
                    MODE_JUST_SAVED -> all.filter { inLast(it) }
                    // 대화기록 보관함 — 글로 옮긴 것만 (전체 저장소의 부분집합)
                    MODE_TRANSCRIPT -> all.filter { hasText(it) }
                    // 녹음 확인하기 — 저장한 녹음 전부.
                    // ★ 저장소는 하나다. 대화기록을 만들어도 녹음 자체가 여기서 사라지면 안 된다.
                    //   (예전엔 글로 옮긴 것을 뺐더니, 대화기록을 만든 순간 녹음이 사라진 것처럼
                    //    보였다.) 대화기록이 생긴 것은 보관함에 '추가로' 나타날 뿐, 이 목록에는 계속 남는다.
                    else            -> all
                }

                adapter.submitList(list)
                binding.tvEmpty.visibility =
                    if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    companion object {
        private const val EXTRA_MODE = "extra_mode"
        private const val MODE_AUDIO      = "audio"
        private const val MODE_TRANSCRIPT = "transcript"
        private const val MODE_JUST_SAVED = "just_saved"

        const val PREFS_NAME    = "safebuffer_last_save"
        const val KEY_LAST_FROM = "last_from_ms"
        const val KEY_LAST_TO   = "last_to_ms"

        /** 녹음 확인하기 (무료) */
        fun audioIntent(context: Context): Intent =
            Intent(context, LockedEvidenceActivity::class.java)
                .putExtra(EXTRA_MODE, MODE_AUDIO)

        /** AI 대화기록 보관함 (유료) */
        fun transcriptIntent(context: Context): Intent =
            Intent(context, LockedEvidenceActivity::class.java)
                .putExtra(EXTRA_MODE, MODE_TRANSCRIPT)

        /** 방금 저장한 녹음 */
        fun justSavedIntent(context: Context): Intent =
            Intent(context, LockedEvidenceActivity::class.java)
                .putExtra(EXTRA_MODE, MODE_JUST_SAVED)
    }

    private fun getWaveformView(position: Int): WaveformView? =
        (binding.rvEvidence.findViewHolderForAdapterPosition(position) as? LockedEvidenceAdapter.VH)
            ?.binding?.waveformView

    private fun getPlayButton(position: Int): Button? =
        (binding.rvEvidence.findViewHolderForAdapterPosition(position) as? LockedEvidenceAdapter.VH)
            ?.binding?.btnPlay

    /** 한 논리 저장 그룹의 물리 파일을 기존 MediaPlayer로 시간순 재생한다. */
    private fun playPhysicalChunk(position: Int, index: Int) {
        if (position != playingPosition || index !in playbackChunks.indices) {
            finishLogicalPlayback(position)
            return
        }
        playbackChunkIndex = index
        val file = File(playbackChunks[index].filePath)
        if (!file.exists()) {
            playPhysicalChunk(position, index + 1)
            return
        }
        val player = MediaPlayer()
        try {
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener {
                if (position != playingPosition || mediaPlayer !== player) {
                    player.release()
                    return@setOnPreparedListener
                }
                if (pendingSeekMs > 0) {
                    try { player.seekTo(pendingSeekMs) } catch (_: Exception) {}
                    pendingSeekMs = 0
                }
                player.start()
                isPaused = false
                getPlayButton(position)?.text = "⏸  일시정지"
                progressHandler.removeCallbacks(progressRunnable)
                progressHandler.post(progressRunnable)
            }
            player.setOnCompletionListener {
                if (mediaPlayer === player) mediaPlayer = null
                player.release()
                playPhysicalChunk(position, index + 1)
            }
            player.setOnErrorListener { _, _, _ ->
                if (mediaPlayer === player) mediaPlayer = null
                try { player.release() } catch (_: Exception) {}
                playPhysicalChunk(position, index + 1)
                true
            }
            mediaPlayer = player
            player.prepareAsync()
        } catch (_: Exception) {
            try { player.release() } catch (_: Exception) {}
            if (mediaPlayer === player) mediaPlayer = null
            playPhysicalChunk(position, index + 1)
        }
    }

    private fun finishLogicalPlayback(position: Int) {
        if (position != playingPosition) return

        // A physical chunk may extend beyond the user's logical saved range.
        // Detach callbacks before stopping so the old player cannot advance to
        // another physical chunk or mutate the UI after playback has finished.
        progressHandler.removeCallbacks(progressRunnable)
        val finishedPlayer = mediaPlayer
        mediaPlayer = null
        if (finishedPlayer != null) {
            try { finishedPlayer.setOnPreparedListener(null) } catch (_: Exception) {}
            try { finishedPlayer.setOnCompletionListener(null) } catch (_: Exception) {}
            try { finishedPlayer.setOnErrorListener(null) } catch (_: Exception) {}
            try { if (finishedPlayer.isPlaying) finishedPlayer.stop() } catch (_: Exception) {}
            try { finishedPlayer.release() } catch (_: Exception) {}
        }

        getPlayButton(position)?.text = "▶  재생"
        getWaveformView(position)?.setProgress(0f)
        playbackChunks = emptyList()
        playbackChunkIndex = 0
        pendingSeekMs = 0
        playbackLogicalStartMs = 0L
        playbackLogicalEndMs = 0L
        playingPosition = -1
        isPaused = false
    }

    override fun onPause() {
        super.onPause()
        // 앱이 백그라운드로 가거나 전화가 올 때 재생 일시정지
        val mp = mediaPlayer
        if (mp != null && mp.isPlaying) {
            mp.pause()
            isPaused = true
            progressHandler.removeCallbacks(progressRunnable)
            getPlayButton(playingPosition)?.text = "▶  계속"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
