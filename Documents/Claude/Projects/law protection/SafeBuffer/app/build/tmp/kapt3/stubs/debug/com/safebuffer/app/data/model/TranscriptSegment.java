package com.safebuffer.app.data.model;

/**
 * Whisper 세그먼트를 절대 시각으로 변환한 모델
 *
 * Whisper 는 파일 내 상대적 초(second)를 반환하므로,
 * 청크의 startTimeMs 를 더해 절대 Unix timestamp 로 변환한다.
 *
 * @param absoluteStartMs  실제 녹음 시작 시각 기준 Unix ms
 * @param absoluteEndMs    실제 녹음 종료 시각 기준 Unix ms
 * @param text             인식된 텍스트
 * @param isSelected       유저가 잠금 구간으로 선택 여부 (UI 상태)
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0014\b\u0086\b\u0018\u00002\u00020\u0001B/\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0014\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\bH\u00c6\u0003J\t\u0010\u0018\u001a\u00020\nH\u00c6\u0003J;\u0010\u0019\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\nH\u00c6\u0001J\u0013\u0010\u001a\u001a\u00020\n2\b\u0010\u001b\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001c\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u001d\u001a\u00020\bH\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u0011R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013\u00a8\u0006\u001e"}, d2 = {"Lcom/safebuffer/app/data/model/TranscriptSegment;", "", "id", "", "absoluteStartMs", "", "absoluteEndMs", "text", "", "isSelected", "", "(IJJLjava/lang/String;Z)V", "getAbsoluteEndMs", "()J", "getAbsoluteStartMs", "getId", "()I", "()Z", "getText", "()Ljava/lang/String;", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class TranscriptSegment {
    private final int id = 0;
    private final long absoluteStartMs = 0L;
    private final long absoluteEndMs = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String text = null;
    private final boolean isSelected = false;
    
    public TranscriptSegment(int id, long absoluteStartMs, long absoluteEndMs, @org.jetbrains.annotations.NotNull()
    java.lang.String text, boolean isSelected) {
        super();
    }
    
    public final int getId() {
        return 0;
    }
    
    public final long getAbsoluteStartMs() {
        return 0L;
    }
    
    public final long getAbsoluteEndMs() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getText() {
        return null;
    }
    
    public final boolean isSelected() {
        return false;
    }
    
    public final int component1() {
        return 0;
    }
    
    public final long component2() {
        return 0L;
    }
    
    public final long component3() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    public final boolean component5() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.safebuffer.app.data.model.TranscriptSegment copy(int id, long absoluteStartMs, long absoluteEndMs, @org.jetbrains.annotations.NotNull()
    java.lang.String text, boolean isSelected) {
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