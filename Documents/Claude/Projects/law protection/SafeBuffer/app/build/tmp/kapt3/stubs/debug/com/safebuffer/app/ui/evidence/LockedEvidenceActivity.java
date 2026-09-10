package com.safebuffer.app.ui.evidence;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0015\u001a\u0004\u0018\u00010\u00162\u0006\u0010\u0017\u001a\u00020\nH\u0002J\u0012\u0010\u0018\u001a\u0004\u0018\u00010\u00192\u0006\u0010\u0017\u001a\u00020\nH\u0002J\u0012\u0010\u001a\u001a\u00020\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u0014J\b\u0010\u001e\u001a\u00020\u001bH\u0014R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000f\u001a\u00020\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006\u001f"}, d2 = {"Lcom/safebuffer/app/ui/evidence/LockedEvidenceActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/safebuffer/app/databinding/ActivityLockedEvidenceBinding;", "isPaused", "", "mediaPlayer", "Landroid/media/MediaPlayer;", "playingPosition", "", "progressHandler", "Landroid/os/Handler;", "progressRunnable", "Ljava/lang/Runnable;", "viewModel", "Lcom/safebuffer/app/ui/evidence/LockedEvidenceViewModel;", "getViewModel", "()Lcom/safebuffer/app/ui/evidence/LockedEvidenceViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "getPlayButton", "Landroid/widget/Button;", "position", "getWaveformView", "Lcom/safebuffer/app/ui/widget/WaveformView;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "app_debug"})
public final class LockedEvidenceActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.safebuffer.app.databinding.ActivityLockedEvidenceBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private android.media.MediaPlayer mediaPlayer;
    private int playingPosition = -1;
    private boolean isPaused = false;
    @org.jetbrains.annotations.NotNull()
    private final android.os.Handler progressHandler = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.Runnable progressRunnable = null;
    
    public LockedEvidenceActivity() {
        super();
    }
    
    private final com.safebuffer.app.ui.evidence.LockedEvidenceViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final com.safebuffer.app.ui.widget.WaveformView getWaveformView(int position) {
        return null;
    }
    
    private final android.widget.Button getPlayButton(int position) {
        return null;
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}