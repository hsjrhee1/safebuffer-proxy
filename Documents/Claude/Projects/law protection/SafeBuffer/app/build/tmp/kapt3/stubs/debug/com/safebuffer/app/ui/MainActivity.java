package com.safebuffer.app.ui;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\t\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0015\u001a\u00020\u0016H\u0002J\u0010\u0010\u0017\u001a\u00020\b2\u0006\u0010\u0018\u001a\u00020\u0019H\u0002J\b\u0010\u001a\u001a\u00020\u001bH\u0002J\b\u0010\u001c\u001a\u00020\u0016H\u0002J\u0012\u0010\u001d\u001a\u00020\u00162\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0014J\b\u0010 \u001a\u00020\u0016H\u0002J\b\u0010!\u001a\u00020\u0016H\u0002J\b\u0010\"\u001a\u00020\u0016H\u0002J\b\u0010#\u001a\u00020\u0016H\u0002J\b\u0010$\u001a\u00020\u0016H\u0002J\u0010\u0010%\u001a\u00020\u00162\u0006\u0010&\u001a\u00020\u001bH\u0002J\b\u0010\'\u001a\u00020\u0016H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\t\u001a\u00020\n8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001b\u0010\u000f\u001a\u00020\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006("}, d2 = {"Lcom/safebuffer/app/ui/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/safebuffer/app/databinding/ActivityMainBinding;", "permissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "", "trialManager", "Lcom/safebuffer/app/util/TrialManager;", "getTrialManager", "()Lcom/safebuffer/app/util/TrialManager;", "setTrialManager", "(Lcom/safebuffer/app/util/TrialManager;)V", "viewModel", "Lcom/safebuffer/app/ui/MainViewModel;", "getViewModel", "()Lcom/safebuffer/app/ui/MainViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "checkPermissionsAndStart", "", "formatHours", "hours", "", "isRecordingServiceRunning", "", "observeViewModel", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "setupUI", "showHoursPickerDialog", "showPermissionDeniedDialog", "startSafeBuffer", "stopSafeBuffer", "updateRecordingUI", "isRecording", "warmUpServer", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.safebuffer.app.databinding.ActivityMainBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @javax.inject.Inject()
    public com.safebuffer.app.util.TrialManager trialManager;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String[]> permissionLauncher = null;
    
    public MainActivity() {
        super();
    }
    
    private final com.safebuffer.app.ui.MainViewModel getViewModel() {
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
    
    private final void setupUI() {
    }
    
    private final void showHoursPickerDialog() {
    }
    
    private final void observeViewModel() {
    }
    
    private final void updateRecordingUI(boolean isRecording) {
    }
    
    private final void checkPermissionsAndStart() {
    }
    
    private final void startSafeBuffer() {
    }
    
    private final void stopSafeBuffer() {
    }
    
    private final void showPermissionDeniedDialog() {
    }
    
    private final boolean isRecordingServiceRunning() {
        return false;
    }
    
    private final void warmUpServer() {
    }
    
    private final java.lang.String formatHours(float hours) {
        return null;
    }
}