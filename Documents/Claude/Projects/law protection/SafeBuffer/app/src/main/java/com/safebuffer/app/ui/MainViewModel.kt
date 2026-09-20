package com.safebuffer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safebuffer.app.data.local.ChunkEntity
import com.safebuffer.app.data.repository.AudioChunkRepository
import com.safebuffer.app.domain.usecase.FreeLimitException
import com.safebuffer.app.domain.usecase.LockChunksUseCase
import com.safebuffer.app.domain.usecase.LockResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AudioChunkRepository,
    private val lockChunksUseCase: LockChunksUseCase
) : ViewModel() {

    // 전체 청크 목록 (타임라인 표시용)
    val allChunks: StateFlow<List<ChunkEntity>> = repository
        .getAllChunks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 잠긴 청크만
    val lockedChunks: StateFlow<List<ChunkEntity>> = repository
        .getLockedChunks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logicalSavedCount: StateFlow<Int> = repository
        .getLogicalSavedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI 이벤트
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    // 녹음 중 여부 (서비스 상태와 동기화)
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun setRecordingState(recording: Boolean) {
        _isRecording.value = recording
    }

    /**
     * '녹음 확인하기' 배지 숫자.
     *
     * 목록은 아직 글로 옮기지 않은 것만 보여주므로, 전체 잠금 수를 세면
     * "22개"라고 떠 있는데 들어가면 비어 있는 상황이 된다. 같은 기준으로 센다.
     */
    suspend fun getLockedCount(): Int =
        repository.countLogicalSaved()

    /** '보관된 대화 기록' 배지 — 글로 옮긴 것만 */
    suspend fun getTranscriptCount(): Int =
        repository.getAllLockedOnce().count { c -> hasTranscript(c) }

    private fun hasTranscript(c: ChunkEntity): Boolean =
        c.transcription?.let {
            it.isNotBlank() && it != "[]" && it != "__SILENT__"
        } ?: false

    suspend fun cleanupOrphans() = repository.cleanupOrphanedChunks()

    /** 미리듣기용 — 선택 구간과 겹치는 청크를 시간순으로 */
    suspend fun chunksInRange(fromMs: Long, toMs: Long): List<ChunkEntity> =
        repository.chunksInRange(fromMs, toMs)

    /**
     * 슬라이더 값(hoursBack)으로 잠금 — 내부에서 nowMs 계산
     * STT 화면에서 "이 구간 보호" 버튼용으로 유지
     */
    fun lockRange(hoursBack: Float) {
        viewModelScope.launch {
            val result = lockChunksUseCase(hoursBack)
            result.fold(
                onSuccess = { lockResult -> _uiEvent.emit(UiEvent.LockSuccess(lockResult)) },
                onFailure = { e ->
                    if (e is FreeLimitException) _uiEvent.emit(UiEvent.FreeLimitReached(e.message ?: ""))
                    else _uiEvent.emit(UiEvent.Error("구간 잠금 실패: ${e.message}"))
                }
            )
        }
    }

    /**
     * 절대 ms 범위로 잠금 — 잠금 버튼용
     * 현재 청크는 RecordingService 가 isLocked=true 로 직접 저장하므로 여기서는 기존 DB 청크만 처리
     */
    fun lockRangeMs(fromMs: Long, toMs: Long) {
        viewModelScope.launch {
            val result = lockChunksUseCase.invokeWithRange(fromMs, toMs)
            result.fold(
                onSuccess = { lockResult -> _uiEvent.emit(UiEvent.LockSuccess(lockResult)) },
                onFailure = { e ->
                    if (e is FreeLimitException) _uiEvent.emit(UiEvent.FreeLimitReached(e.message ?: ""))
                    else _uiEvent.emit(UiEvent.Error("구간 잠금 실패: ${e.message}"))
                }
            )
        }
    }
}

sealed class UiEvent {
    data class LockSuccess(val result: LockResult) : UiEvent()
    data class FreeLimitReached(val message: String) : UiEvent()
    data class Error(val message: String) : UiEvent()
}
