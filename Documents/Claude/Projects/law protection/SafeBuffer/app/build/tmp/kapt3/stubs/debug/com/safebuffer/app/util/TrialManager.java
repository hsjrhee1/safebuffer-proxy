package com.safebuffer.app.util;

/**
 * 3일 무료 체험 관리
 *
 * - 첫 실행 시 타임스탬프 저장
 * - 남은 일수 계산 (소수점 올림)
 * - 만료 여부 확인
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u0007\u0018\u0000 \u000f2\u00020\u0001:\u0001\u000fB\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0007\u001a\u00020\bJ\u0006\u0010\t\u001a\u00020\nJ\u0006\u0010\u000b\u001a\u00020\fJ\u0006\u0010\r\u001a\u00020\u000eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/safebuffer/app/util/TrialManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "prefs", "Landroid/content/SharedPreferences;", "daysRemaining", "", "firstLaunchMs", "", "initIfNeeded", "", "isExpired", "", "Companion", "app_debug"})
public final class TrialManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS_NAME = "safebuffer_trial";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_FIRST_LAUNCH = "first_launch_ms";
    private static final long TRIAL_DAYS = 3L;
    private static final long TRIAL_MS = 259200000L;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.safebuffer.app.util.TrialManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public TrialManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    /**
     * 첫 실행 시 호출 — 이미 저장된 경우 무시
     */
    public final void initIfNeeded() {
    }
    
    /**
     * 체험 만료 여부
     */
    public final boolean isExpired() {
        return false;
    }
    
    /**
     * 남은 일수 (만료 시 0)
     */
    public final int daysRemaining() {
        return 0;
    }
    
    /**
     * 첫 실행일 ms
     */
    public final long firstLaunchMs() {
        return 0L;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0007X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/safebuffer/app/util/TrialManager$Companion;", "", "()V", "KEY_FIRST_LAUNCH", "", "PREFS_NAME", "TRIAL_DAYS", "", "TRIAL_MS", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}