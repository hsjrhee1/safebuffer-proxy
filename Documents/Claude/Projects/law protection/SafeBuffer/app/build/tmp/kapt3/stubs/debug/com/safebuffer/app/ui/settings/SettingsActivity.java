package com.safebuffer.app.ui.settings;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0014J\b\u0010\u0015\u001a\u00020\u0012H\u0014J\b\u0010\u0016\u001a\u00020\u0012H\u0002J\b\u0010\u0017\u001a\u00020\u0012H\u0002J\b\u0010\u0018\u001a\u00020\u0012H\u0002J\b\u0010\u0019\u001a\u00020\u0012H\u0002J\b\u0010\u001a\u001a\u00020\u0012H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0005\u001a\u00020\u00068\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR\u001e\u0010\u000b\u001a\u00020\f8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010\u00a8\u0006\u001b"}, d2 = {"Lcom/safebuffer/app/ui/settings/SettingsActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/safebuffer/app/databinding/ActivitySettingsBinding;", "chunkRepository", "Lcom/safebuffer/app/data/repository/AudioChunkRepository;", "getChunkRepository", "()Lcom/safebuffer/app/data/repository/AudioChunkRepository;", "setChunkRepository", "(Lcom/safebuffer/app/data/repository/AudioChunkRepository;)V", "trialManager", "Lcom/safebuffer/app/util/TrialManager;", "getTrialManager", "()Lcom/safebuffer/app/util/TrialManager;", "setTrialManager", "(Lcom/safebuffer/app/util/TrialManager;)V", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onResume", "setupBatteryOptimization", "setupResetDb", "setupTrialStatus", "setupVersionInfo", "updateBatteryStatus", "app_debug"})
public final class SettingsActivity extends androidx.appcompat.app.AppCompatActivity {
    @javax.inject.Inject()
    public com.safebuffer.app.util.TrialManager trialManager;
    @javax.inject.Inject()
    public com.safebuffer.app.data.repository.AudioChunkRepository chunkRepository;
    private com.safebuffer.app.databinding.ActivitySettingsBinding binding;
    
    public SettingsActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.safebuffer.app.util.TrialManager getTrialManager() {
        return null;
    }
    
    public final void setTrialManager(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.util.TrialManager p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.safebuffer.app.data.repository.AudioChunkRepository getChunkRepository() {
        return null;
    }
    
    public final void setChunkRepository(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.repository.AudioChunkRepository p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    private final void setupTrialStatus() {
    }
    
    private final void setupVersionInfo() {
    }
    
    private final void setupBatteryOptimization() {
    }
    
    private final void setupResetDb() {
    }
    
    private final void updateBatteryStatus() {
    }
}