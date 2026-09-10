package com.safebuffer.app.ui.transcript;

/**
 * 대화 타임라인 화면
 *
 * [사용 흐름]
 * 1. MainActivity에서 "대화 내용 확인" 버튼 → 이 화면 실행
 * 2. Whisper STT로 해당 구간 전사
 * 3. 타임라인 목록에서 탭으로 시작/끝 구간 선택 (빨간색 하이라이트)
 * 4. "이 구간 잠금" 버튼 → 해당 청크 영구 보존
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0005\b\u0007\u0018\u0000 !2\u00020\u0001:\u0001!B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0015\u001a\u00020\u0016H\u0002J\u0012\u0010\u0017\u001a\u00020\u00162\b\u0010\u0018\u001a\u0004\u0018\u00010\u0019H\u0014J\b\u0010\u001a\u001a\u00020\u0016H\u0002J\u0018\u0010\u001b\u001a\u00020\u00162\u0006\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u001dH\u0002J\b\u0010\u001f\u001a\u00020\u0016H\u0002J\b\u0010 \u001a\u00020\u0016H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\t\u001a\u00020\n8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001b\u0010\u000f\u001a\u00020\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006\""}, d2 = {"Lcom/safebuffer/app/ui/transcript/TranscriptActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Lcom/safebuffer/app/ui/transcript/TranscriptAdapter;", "binding", "Lcom/safebuffer/app/databinding/ActivityTranscriptBinding;", "timeFmt", "Ljava/text/SimpleDateFormat;", "trialManager", "Lcom/safebuffer/app/util/TrialManager;", "getTrialManager", "()Lcom/safebuffer/app/util/TrialManager;", "setTrialManager", "(Lcom/safebuffer/app/util/TrialManager;)V", "viewModel", "Lcom/safebuffer/app/ui/transcript/TranscriptViewModel;", "getViewModel", "()Lcom/safebuffer/app/ui/transcript/TranscriptViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "observeViewModel", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "setupRecyclerView", "setupUI", "fromMs", "", "toMs", "showLockConfirmDialog", "showTrialExpiredDialog", "Companion", "app_debug"})
public final class TranscriptActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.safebuffer.app.databinding.ActivityTranscriptBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.safebuffer.app.ui.transcript.TranscriptAdapter adapter;
    @javax.inject.Inject()
    public com.safebuffer.app.util.TrialManager trialManager;
    @org.jetbrains.annotations.NotNull()
    private final java.text.SimpleDateFormat timeFmt = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXTRA_FROM_MS = "extra_from_ms";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXTRA_TO_MS = "extra_to_ms";
    @org.jetbrains.annotations.NotNull()
    public static final com.safebuffer.app.ui.transcript.TranscriptActivity.Companion Companion = null;
    
    public TranscriptActivity() {
        super();
    }
    
    private final com.safebuffer.app.ui.transcript.TranscriptViewModel getViewModel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.safebuffer.app.util.TrialManager getTrialManager() {
        return null;
    }
    
    public final void setTrialManager(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.util.TrialManager p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupRecyclerView() {
    }
    
    private final void setupUI(long fromMs, long toMs) {
    }
    
    private final void observeViewModel() {
    }
    
    private final void showLockConfirmDialog() {
    }
    
    private final void showTrialExpiredDialog() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u001e\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/safebuffer/app/ui/transcript/TranscriptActivity$Companion;", "", "()V", "EXTRA_FROM_MS", "", "EXTRA_TO_MS", "newIntent", "Landroid/content/Intent;", "context", "Landroid/content/Context;", "fromMs", "", "toMs", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.content.Intent newIntent(@org.jetbrains.annotations.NotNull()
        android.content.Context context, long fromMs, long toMs) {
            return null;
        }
    }
}