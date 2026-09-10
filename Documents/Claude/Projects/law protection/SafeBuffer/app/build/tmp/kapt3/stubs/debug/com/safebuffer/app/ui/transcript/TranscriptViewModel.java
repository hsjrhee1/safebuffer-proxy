package com.safebuffer.app.ui.transcript;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\t\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001:\u0001.B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010&\u001a\u00020\'J\u000e\u0010(\u001a\u00020\'2\u0006\u0010)\u001a\u00020\u000fJ\b\u0010*\u001a\u00020\'H\u0002J\u0016\u0010+\u001a\u00020\'2\u0006\u0010,\u001a\u00020\u001f2\u0006\u0010-\u001a\u00020\u001fR\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\r0\f0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\t0\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u001d\u0010\u001c\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\r0\f0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0017R\u0013\u0010\u001e\u001a\u0004\u0018\u00010\u001f8F\u00a2\u0006\u0006\u001a\u0004\b \u0010!R\u0013\u0010\"\u001a\u0004\u0018\u00010\u001f8F\u00a2\u0006\u0006\u001a\u0004\b#\u0010!R\u0017\u0010$\u001a\b\u0012\u0004\u0012\u00020\u00120\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u0017R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006/"}, d2 = {"Lcom/safebuffer/app/ui/transcript/TranscriptViewModel;", "Landroidx/lifecycle/ViewModel;", "whisperRepository", "Lcom/safebuffer/app/data/repository/WhisperRepository;", "chunkRepository", "Lcom/safebuffer/app/data/repository/AudioChunkRepository;", "(Lcom/safebuffer/app/data/repository/WhisperRepository;Lcom/safebuffer/app/data/repository/AudioChunkRepository;)V", "_lockEvent", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/safebuffer/app/ui/transcript/LockEvent;", "_segments", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/safebuffer/app/data/model/TranscriptSegment;", "_selectionEnd", "", "_selectionStart", "_uiState", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState;", "hasSelection", "Lkotlinx/coroutines/flow/StateFlow;", "", "getHasSelection", "()Lkotlinx/coroutines/flow/StateFlow;", "lockEvent", "Lkotlinx/coroutines/flow/SharedFlow;", "getLockEvent", "()Lkotlinx/coroutines/flow/SharedFlow;", "segments", "getSegments", "selectedFromMs", "", "getSelectedFromMs", "()Ljava/lang/Long;", "selectedToMs", "getSelectedToMs", "uiState", "getUiState", "lockSelectedRange", "", "onSegmentTapped", "segmentId", "refreshSelectionHighlight", "transcribeRange", "fromMs", "toMs", "UiState", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class TranscriptViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.safebuffer.app.data.repository.WhisperRepository whisperRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.safebuffer.app.data.repository.AudioChunkRepository chunkRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.safebuffer.app.data.model.TranscriptSegment>> _segments = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.safebuffer.app.data.model.TranscriptSegment>> segments = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _selectionStart = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _selectionEnd = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> hasSelection = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.safebuffer.app.ui.transcript.LockEvent> _lockEvent = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.safebuffer.app.ui.transcript.LockEvent> lockEvent = null;
    
    @javax.inject.Inject()
    public TranscriptViewModel(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.repository.WhisperRepository whisperRepository, @org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.repository.AudioChunkRepository chunkRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.safebuffer.app.data.model.TranscriptSegment>> getSegments() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getSelectedFromMs() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getSelectedToMs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getHasSelection() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.safebuffer.app.ui.transcript.LockEvent> getLockEvent() {
        return null;
    }
    
    public final void transcribeRange(long fromMs, long toMs) {
    }
    
    public final void onSegmentTapped(int segmentId) {
    }
    
    private final void refreshSelectionHighlight() {
    }
    
    public final void lockSelectedRange() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0004\u0003\u0004\u0005\u0006B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0004\u0007\b\t\n\u00a8\u0006\u000b"}, d2 = {"Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState;", "", "()V", "Done", "Error", "Idle", "Loading", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState$Done;", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState$Error;", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState$Idle;", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState$Loading;", "app_debug"})
    public static abstract class UiState {
        
        private UiState() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState$Done;", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState;", "()V", "app_debug"})
        public static final class Done extends com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState {
            @org.jetbrains.annotations.NotNull()
            public static final com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState.Done INSTANCE = null;
            
            private Done() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState$Error;", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState;", "message", "", "(Ljava/lang/String;)V", "getMessage", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Error extends com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String message = null;
            
            public Error(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getMessage() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState.Error copy(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState$Idle;", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState;", "()V", "app_debug"})
        public static final class Idle extends com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState {
            @org.jetbrains.annotations.NotNull()
            public static final com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState.Idle INSTANCE = null;
            
            private Idle() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState$Loading;", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel$UiState;", "()V", "app_debug"})
        public static final class Loading extends com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState {
            @org.jetbrains.annotations.NotNull()
            public static final com.safebuffer.app.ui.transcript.TranscriptViewModel.UiState.Loading INSTANCE = null;
            
            private Loading() {
            }
        }
    }
}