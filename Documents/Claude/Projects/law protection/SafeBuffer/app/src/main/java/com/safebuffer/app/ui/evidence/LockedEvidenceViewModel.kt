package com.safebuffer.app.ui.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safebuffer.app.data.local.ChunkDao
import com.safebuffer.app.data.local.ChunkEntity
import com.safebuffer.app.data.model.TranscriptSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LockedEvidence(
    val chunk: ChunkEntity,
    val transcriptText: String
)

@HiltViewModel
class LockedEvidenceViewModel @Inject constructor(
    private val chunkDao: ChunkDao
) : ViewModel() {

    private val gson = Gson()

    val evidenceList = chunkDao.getLockedChunks().map { chunks ->
        chunks.map { chunk ->
            val text = parseTranscription(chunk.transcription)
            LockedEvidence(chunk, text)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun delete(chunk: ChunkEntity) {
        viewModelScope.launch {
            File(chunk.filePath).delete()
            chunkDao.delete(chunk)
        }
    }

    private fun parseTranscription(json: String?): String {
        if (json.isNullOrBlank()) return "(대화 내용 미확인)"
        return try {
            val type = object : TypeToken<List<TranscriptSegment>>() {}.type
            val list: List<TranscriptSegment> = gson.fromJson(json, type)
            if (list.isEmpty()) return "※ 인식된 대화 내용이 없습니다."
            val timeFmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREA)
            list.joinToString("\n") { seg ->
                "[${timeFmt.format(java.util.Date(seg.absoluteStartMs))}] ${seg.text}"
            }
        } catch (e: Exception) {
            "(대화 내용 없음)"
        }
    }
}
