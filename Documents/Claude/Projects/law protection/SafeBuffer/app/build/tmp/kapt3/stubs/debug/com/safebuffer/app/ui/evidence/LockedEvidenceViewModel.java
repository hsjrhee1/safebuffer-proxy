package com.safebuffer.app.ui.evidence;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010J\u0012\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0012H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/safebuffer/app/ui/evidence/LockedEvidenceViewModel;", "Landroidx/lifecycle/ViewModel;", "chunkDao", "Lcom/safebuffer/app/data/local/ChunkDao;", "(Lcom/safebuffer/app/data/local/ChunkDao;)V", "evidenceList", "Lkotlinx/coroutines/flow/StateFlow;", "", "Lcom/safebuffer/app/ui/evidence/LockedEvidence;", "getEvidenceList", "()Lkotlinx/coroutines/flow/StateFlow;", "gson", "Lcom/google/gson/Gson;", "delete", "", "chunk", "Lcom/safebuffer/app/data/local/ChunkEntity;", "parseTranscription", "", "json", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class LockedEvidenceViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.safebuffer.app.data.local.ChunkDao chunkDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.safebuffer.app.ui.evidence.LockedEvidence>> evidenceList = null;
    
    @javax.inject.Inject()
    public LockedEvidenceViewModel(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.local.ChunkDao chunkDao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.safebuffer.app.ui.evidence.LockedEvidence>> getEvidenceList() {
        return null;
    }
    
    public final void delete(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.local.ChunkEntity chunk) {
    }
    
    private final java.lang.String parseTranscription(java.lang.String json) {
        return null;
    }
}