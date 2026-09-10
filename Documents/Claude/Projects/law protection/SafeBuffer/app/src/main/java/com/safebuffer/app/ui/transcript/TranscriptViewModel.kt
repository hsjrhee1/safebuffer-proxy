package com.safebuffer.app.ui.transcript

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safebuffer.app.data.model.TranscriptSegment
import com.safebuffer.app.data.repository.AudioChunkRepository
import com.safebuffer.app.data.repository.WhisperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val whisperRepository: WhisperRepository,
    private val chunkRepository: AudioChunkRepository
) : ViewModel() {

    // 전사된 세그먼트 목록
    private val _segments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val segments: StateFlow<List<TranscriptSegment>> = _segments

    // 유저가 선택한 구간 (시작/끝 세그먼트 ID)
    private val _selectionStart = MutableStateFlow<Int?>(null)
    private val _selectionEnd   = MutableStateFlow<Int?>(null)

    // 선택 구간이 확정된 실제 ms
    val selectedFromMs: Long? get() = _selectionStart.value
        ?.let { startId -> _segments.value.find { it.id == startId }?.absoluteStartMs }

    val selectedToMs: Long? get() = _selectionEnd.value
        ?.let { endId -> _segments.value.find { it.id == endId }?.absoluteEndMs }

    val hasSelection: StateFlow<Boolean> = combine(_selectionStart, _selectionEnd) { s, e ->
        s != null && e != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // 로딩 / 에러 / 완료 상태
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
        object Done : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    // 잠금 이벤트
    private val _lockEvent = MutableSharedFlow<LockEvent>()
    val lockEvent: SharedFlow<LockEvent> = _lockEvent

    // ──────────────────────────────────────────────────────────
    // 전사 요청
    // ──────────────────────────────────────────────────────────

    fun transcribeRange(fromMs: Long, toMs: Long) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            whisperRepository.transcribeRange(fromMs, toMs)
                .onSuccess { segments ->
                    _segments.emit(segments)
                    _uiState.emit(UiState.Done)
                }
                .onFailure { e ->
                    _uiState.emit(UiState.Error(e.message ?: "전사 실패"))
                }
        }
    }

    // ──────────────────────────────────────────────────────────
    // 구간 선택 (탭 한 번 = 시작점, 두 번 = 끝점)
    // ──────────────────────────────────────────────────────────

    fun onSegmentTapped(segmentId: Int) {
        val start = _selectionStart.value
        val end   = _selectionEnd.value

        when {
            // 아무것도 선택 안 된 상태 → 시작점 설정
            start == null -> {
                _selectionStart.value = segmentId
                _selectionEnd.value = null
                refreshSelectionHighlight()
            }
            // 시작점만 있는 상태 → 끝점 설정 (시작 < 끝 보장)
            end == null -> {
                val startSeg = _segments.value.find { it.id == start } ?: return
                val tappedSeg = _segments.value.find { it.id == segmentId } ?: return
                if (tappedSeg.absoluteStartMs >= startSeg.absoluteStartMs) {
                    _selectionEnd.value = segmentId
                } else {
                    // 탭한 게 시작점보다 앞이면 시작점을 교체
                    _selectionStart.value = segmentId
                    _selectionEnd.value = null
                }
                refreshSelectionHighlight()
            }
            // 이미 둘 다 선택된 상태 → 초기화 후 새 시작점
            else -> {
                _selectionStart.value = segmentId
                _selectionEnd.value = null
                refreshSelectionHighlight()
            }
        }
    }

    private fun refreshSelectionHighlight() {
        val start = _selectionStart.value
        val end   = _selectionEnd.value
        val startMs = _segments.value.find { it.id == start }?.absoluteStartMs ?: Long.MAX_VALUE
        val endMs   = _segments.value.find { it.id == end }?.absoluteEndMs   ?: Long.MIN_VALUE

        _segments.value = _segments.value.map { seg ->
            seg.copy(
                isSelected = start != null && (
                    // 끝점 없으면 시작점만 하이라이트
                    if (end == null) seg.id == start
                    // 끝점 있으면 구간 전체 하이라이트
                    else seg.absoluteStartMs >= startMs && seg.absoluteEndMs <= endMs
                )
            )
        }
    }

    // ──────────────────────────────────────────────────────────
    // 구간 잠금
    // ──────────────────────────────────────────────────────────

    fun lockSelectedRange() {
        val fromMs = selectedFromMs ?: return
        val toMs   = selectedToMs   ?: return

        viewModelScope.launch {
            chunkRepository.lockRange(fromMs, toMs)
            // 원본 증명: SHA-256 해시 + 서버 타임스탬프 발급
            try {
                chunkRepository.stampChunksInRange(fromMs, toMs)
            } catch (e: Exception) {
                android.util.Log.w("TranscriptVM", "stamp 실패: ${e.message}")
            }
            _lockEvent.emit(LockEvent.Success(fromMs, toMs))
        }
    }
}

data class LockEvent(val fromMs: Long, val toMs: Long) {
    companion object {
        fun Success(fromMs: Long, toMs: Long) = LockEvent(fromMs, toMs)
    }
}
