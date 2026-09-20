package com.safebuffer.app.ui.transcript

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safebuffer.app.data.model.TranscriptSegment
import com.safebuffer.app.data.repository.AudioChunkRepository
import com.safebuffer.app.data.repository.WhisperRepository
import com.safebuffer.app.domain.usecase.FreeLimitException
import com.safebuffer.app.domain.usecase.LockChunksUseCase
import com.safebuffer.app.util.SttQuotaManager
import com.safebuffer.app.util.TierManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val whisperRepository: WhisperRepository,
    private val chunkRepository: AudioChunkRepository,
    private val lockChunksUseCase: LockChunksUseCase,
    private val sttQuotaManager: SttQuotaManager,
    private val tierManager: TierManager
) : ViewModel() {

    private val _segments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val segments: StateFlow<List<TranscriptSegment>> = _segments

    private val _selectionStart = MutableStateFlow<Int?>(null)
    private val _selectionEnd   = MutableStateFlow<Int?>(null)

    val selectedFromMs: Long? get() = _selectionStart.value
        ?.let { startId -> _segments.value.find { it.id == startId }?.absoluteStartMs }

    val selectedToMs: Long? get() = _selectionEnd.value
        ?.let { endId -> _segments.value.find { it.id == endId }?.absoluteEndMs }

    val hasSelection: StateFlow<Boolean> = combine(_selectionStart, _selectionEnd) { s, e ->
        s != null && e != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    sealed class UiState {
        object Idle    : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
        object Done    : UiState()
        /** STT 크레딧 소진 — 남은 시간이 없음 */
        data class QuotaExceeded(
            val purchasedRemaining: String   // "없음" or "X시간 Y분"
        ) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    // ★ replay=1: Activity가 백그라운드에서 돌아왔을 때 마지막 잠금 완료 이벤트를 놓치지 않도록
    private val _lockEvent = MutableSharedFlow<LockEvent>(replay = 1)
    val lockEvent: SharedFlow<LockEvent> = _lockEvent

    // ──────────────────────────────────────────────────────────
    // 전사 요청
    // ──────────────────────────────────────────────────────────

    fun transcribeRange(fromMs: Long, toMs: Long) {
        if (!sttQuotaManager.canUse()) {
            viewModelScope.launch {
                _uiState.emit(
                    UiState.QuotaExceeded(
                        purchasedRemaining = sttQuotaManager.remainingPurchasedText()
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            val durationMs = (toMs - fromMs).coerceAtLeast(0L)
            // 남용 방지를 위해 요청 직전에 차감하되, 어느 쪽에서 차감됐는지 기억해 둔다
            val usedMonthly = sttQuotaManager.willUseMonthly()
            sttQuotaManager.consume(durationMs)
            whisperRepository.transcribeRange(fromMs, toMs)
                .onSuccess { segments ->
                    _segments.emit(segments)
                    _uiState.emit(UiState.Done)
                }
                .onFailure { e ->
                    // ★ 서버 오류·타임아웃은 사용자 잘못이 아니므로 할당량을 되돌린다.
                    //   (월 3회 한도에서 콜드스타트 한 번에 1/3 이 사라지던 문제)
                    sttQuotaManager.refund(durationMs, usedMonthly)
                    _uiState.emit(UiState.Error(e.message ?: "전사 실패"))
                }
        }
    }

    // ──────────────────────────────────────────────────────────
    // 구간 선택
    // ──────────────────────────────────────────────────────────

    fun onSegmentTapped(segmentId: Int) {
        val start = _selectionStart.value
        val end   = _selectionEnd.value

        when {
            start == null -> {
                _selectionStart.value = segmentId
                _selectionEnd.value = null
                refreshSelectionHighlight()
            }
            end == null -> {
                val startSeg   = _segments.value.find { it.id == start } ?: return
                val tappedSeg  = _segments.value.find { it.id == segmentId } ?: return
                if (tappedSeg.absoluteStartMs >= startSeg.absoluteStartMs) {
                    _selectionEnd.value = segmentId
                } else {
                    _selectionStart.value = segmentId
                    _selectionEnd.value = null
                }
                refreshSelectionHighlight()
            }
            else -> {
                _selectionStart.value = segmentId
                _selectionEnd.value = null
                refreshSelectionHighlight()
            }
        }
    }

    private fun refreshSelectionHighlight() {
        val start   = _selectionStart.value
        val end     = _selectionEnd.value
        val startMs = _segments.value.find { it.id == start }?.absoluteStartMs ?: Long.MAX_VALUE
        val endMs   = _segments.value.find { it.id == end }?.absoluteEndMs     ?: Long.MIN_VALUE

        _segments.value = _segments.value.map { seg ->
            seg.copy(
                isSelected = start != null && (
                    if (end == null) seg.id == start
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
            // LockChunksUseCase 사용 — Free 한도 체크 포함
            val result = lockChunksUseCase.invokeWithRange(fromMs, toMs)
            result.fold(
                onSuccess = {
                    _lockEvent.emit(LockEvent.Success(fromMs, toMs))
                },
                onFailure = { e ->
                    if (e is FreeLimitException) {
                        _uiState.emit(UiState.Error("무료 버전 한도 초과: ${e.message}"))
                    } else {
                        _uiState.emit(UiState.Error("잠금 실패: ${e.message}"))
                    }
                }
            )
        }
    }
}

data class LockEvent(val fromMs: Long, val toMs: Long) {
    companion object {
        fun Success(fromMs: Long, toMs: Long) = LockEvent(fromMs, toMs)
    }
}
