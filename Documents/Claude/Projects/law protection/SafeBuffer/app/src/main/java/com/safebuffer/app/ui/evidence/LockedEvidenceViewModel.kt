package com.safebuffer.app.ui.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safebuffer.app.data.local.ChunkDao
import com.safebuffer.app.data.local.ChunkEntity
import com.safebuffer.app.data.repository.WhisperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LockedEvidence(
    val chunk: ChunkEntity,
    val transcriptText: String,
    val physicalChunks: List<ChunkEntity> = listOf(chunk)
)

@HiltViewModel
class LockedEvidenceViewModel @Inject constructor(
    private val chunkDao: ChunkDao
) : ViewModel() {

    val evidenceList = combine(
        chunkDao.getLockedChunks(),
        chunkDao.getSavedRanges()
    ) { chunks, savedRanges ->
        val savedIds = savedRanges.map { it.saveGroupId }.toSet()
        val ranged = savedRanges.mapNotNull { range ->
            val group = chunks.filter {
                it.startTimeMs <= range.requestedEndMs && it.endTimeMs >= range.requestedStartMs
            }
            if (group.isEmpty()) return@mapNotNull null
            buildEvidence(
                group = group,
                logicalStartMs = range.requestedStartMs,
                logicalEndMs = range.requestedEndMs,
                saveGroupId = range.saveGroupId
            )
        }
        val legacy = chunks
            .groupBy { it.saveGroupId ?: it.id }
            .filterKeys { it !in savedIds }
            .values.map { group -> buildEvidence(group) }
        ranged + legacy
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private fun buildEvidence(
        group: List<ChunkEntity>,
        logicalStartMs: Long? = null,
        logicalEndMs: Long? = null,
        saveGroupId: Long? = null
    ): LockedEvidence {
            val ordered = group.sortedBy { it.startTimeMs }
            val first = ordered.first()
            val last = ordered.last()
            val transcriptSource = ordered.firstOrNull {
                !it.transcription.isNullOrBlank() &&
                    it.transcription != "[]" &&
                    it.transcription != WhisperRepository.MARKER_SILENT
            }?.transcription ?: ordered.firstOrNull {
                it.transcription == WhisperRepository.MARKER_SILENT
            }?.transcription
            val logicalChunk = last.copy(
                startTimeMs = logicalStartMs ?: first.startTimeMs,
                endTimeMs = logicalEndMs ?: last.endTimeMs,
                saveGroupId = saveGroupId ?: last.saveGroupId,
                transcription = transcriptSource
            )
            val text = ordered.mapNotNull { chunk ->
                chunk.transcription?.takeIf { it.isNotBlank() }?.let(::parseTranscription)
            }.joinToString("\n").ifBlank { "아직 대화 기록을 만들지 않았습니다." }
            android.util.Log.i("SaveFlow", buildString {
                append("SAVE_GROUP saveGroupId=${logicalChunk.saveGroupId} ")
                append("requestedStartMs=${logicalChunk.startTimeMs} ")
                append("requestedEndMs=${logicalChunk.endTimeMs}")
                ordered.forEachIndexed { index, chunk ->
                    append(" chunk[$index]:startTimeMs=${chunk.startTimeMs},")
                    append("endTimeMs=${chunk.endTimeMs},file=${chunk.filePath}")
                }
            })
            return LockedEvidence(logicalChunk, text, ordered)
    }

    fun delete(evidence: LockedEvidence) {
        viewModelScope.launch {
            val chunk = evidence.chunk
            android.util.Log.i("SaveFlow",
                "DELETE_EVIDENCE id=${chunk.id} file=${chunk.filePath} " +
                    "start=${chunk.startTimeMs} end=${chunk.endTimeMs}")
            val groupId = chunk.saveGroupId
            if (groupId == null) {
                deleteOrTransferChunk(chunk)
                return@launch
            }

            // 논리 범위를 먼저 제거한 뒤, 이 그룹이 실제로 소유 표시된 청크만 정리한다.
            // 다른 saved_range가 겹치면 파일을 보존하고 그 그룹으로 소유 표시를 넘긴다.
            chunkDao.deleteSavedRange(groupId)
            val owned = chunkDao.getChunksBySaveGroupId(groupId)
            owned.forEach { deleteOrTransferChunk(it) }
        }
    }

    private suspend fun deleteOrTransferChunk(chunk: ChunkEntity) {
        val remaining = chunkDao.getFirstSavedRangeOverlapping(
            chunkStartMs = chunk.startTimeMs,
            chunkEndMs = chunk.endTimeMs
        )
        if (remaining != null) {
            chunkDao.moveChunkToSaveGroup(chunk.id, remaining.saveGroupId)
            android.util.Log.i(
                "SaveFlow",
                "DELETE_EVIDENCE_KEEP_SHARED chunkId=${chunk.id} " +
                    "remainingSaveGroupId=${remaining.saveGroupId} file=${chunk.filePath}"
            )
            return
        }

        val fileDeleted = File(chunk.filePath).let { !it.exists() || it.delete() }
        chunkDao.delete(chunk)
        android.util.Log.i(
            "SaveFlow",
            "DELETE_EVIDENCE_DELETE_ORPHAN chunkId=${chunk.id} " +
                "fileDeleted=$fileDeleted file=${chunk.filePath}"
        )
    }

    private fun parseTranscription(json: String?): String {
        if (json.isNullOrBlank()) return "아직 대화 기록을 만들지 않았습니다."
        // ★ 무음으로 확인된 청크는 별도 표식으로 저장된다.
        //   이걸 JSON 으로 파싱하려 하면 예외가 나서 "(대화 내용 없음)"으로 잘못 표시됐다.
        if (json == WhisperRepository.MARKER_SILENT) return "말소리가 없는 구간입니다."
        return try {
            val arr = org.json.JSONArray(json)
            if (arr.length() == 0) return "인식된 대화 내용이 없습니다."
            val timeFmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREA)
            buildString {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val startMs = obj.getLong("absoluteStartMs")
                    val text = obj.getString("text")
                    appendLine("[${timeFmt.format(java.util.Date(startMs))}] $text")
                }
            }.trimEnd()
        } catch (e: Exception) {
            "대화 기록을 읽을 수 없습니다."
        }
    }
}
