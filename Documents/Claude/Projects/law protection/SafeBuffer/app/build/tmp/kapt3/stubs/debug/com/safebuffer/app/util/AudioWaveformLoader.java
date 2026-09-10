package com.safebuffer.app.util;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010 \n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\b\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u001a\u0010\u0003\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\bJ\u001e\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\f\u001a\u00020\r\u00a8\u0006\u000e"}, d2 = {"Lcom/safebuffer/app/util/AudioWaveformLoader;", "", "()V", "analyzeForStt", "Lcom/safebuffer/app/util/AudioAnalysisResult;", "filePath", "", "bucketDurationMs", "", "load", "", "", "buckets", "", "app_debug"})
public final class AudioWaveformLoader {
    @org.jetbrains.annotations.NotNull()
    public static final com.safebuffer.app.util.AudioWaveformLoader INSTANCE = null;
    
    private AudioWaveformLoader() {
        super();
    }
    
    /**
     * STT 세그먼트 amplitude 분석.
     * UI 파형(load)과 달리 정규화하지 않은 raw RMS를 반환한다.
     * 해상도: bucketDurationMs ms 단위 (기본 250ms → 4 buckets/sec).
     *  10분 파일 → 2,400 buckets (~9.6 KB)
     *  30분 파일 → 7,200 buckets (~28.8 KB)
     * 오류 또는 판단 불가 시 null 반환 → 호출 측에서 필터 건너뜀.
     * IO 스레드에서 실행해야 함.
     */
    @org.jetbrains.annotations.Nullable()
    public final com.safebuffer.app.util.AudioAnalysisResult analyzeForStt(@org.jetbrains.annotations.NotNull()
    java.lang.String filePath, long bucketDurationMs) {
        return null;
    }
    
    /**
     * 오디오 파일을 디코딩해 진폭 배열 반환 (0.0–1.0, 정규화됨).
     * UI 파형 표시 전용. STT 판정에는 analyzeForStt()를 사용할 것.
     * IO 스레드에서 실행해야 함.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.Float> load(@org.jetbrains.annotations.NotNull()
    java.lang.String filePath, int buckets) {
        return null;
    }
}