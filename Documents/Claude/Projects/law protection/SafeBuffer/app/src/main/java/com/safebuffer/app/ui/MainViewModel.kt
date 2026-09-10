package com.safebuffer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safebuffer.app.data.local.ChunkEntity
import com.safebuffer.app.data.repository.AudioChunkRepository
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
     * 구간 잠금 — 핵심 기능
     * @param hoursBack 슬라이더 값 (0.5 ~ 48.0)
     */
    fun lockRange(hoursBack: Float) {
        viewModelScope.launch {
            try {
                val result = lockChunksUseCase(hoursBack)
                _uiEvent.emit(UiEvent.LockSuccess(result))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.Error("구간 잠금 실패: ${e.message}"))
            }
        }
    }
}

sealed class UiEvent {
    data class LockSuccess(val result: LockResult) : UiEvent()
    data class Error(val message: String) : UiEvent()
}
