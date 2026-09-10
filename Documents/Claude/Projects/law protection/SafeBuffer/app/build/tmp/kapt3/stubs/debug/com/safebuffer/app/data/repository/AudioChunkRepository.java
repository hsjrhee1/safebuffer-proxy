package com.safebuffer.app.data.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\b\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u000bJ\u000e\u0010\f\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\rJ\u0012\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u00100\u000fJ\u0012\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u00100\u000fJ\u001e\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\n2\u0006\u0010\u0016\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0017J\u000e\u0010\u0018\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\rJ\u0016\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u001bJ\u0010\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\u001e\u0010 \u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\n2\u0006\u0010\u0016\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0017R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/safebuffer/app/data/repository/AudioChunkRepository;", "", "dao", "Lcom/safebuffer/app/data/local/ChunkDao;", "api", "Lcom/safebuffer/app/data/remote/WhisperApiService;", "(Lcom/safebuffer/app/data/local/ChunkDao;Lcom/safebuffer/app/data/remote/WhisperApiService;)V", "cleanupExpired", "", "cutoffMs", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "count", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllChunks", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/safebuffer/app/data/local/ChunkEntity;", "getLockedChunks", "lockRange", "", "fromMs", "toMs", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resetAll", "saveChunk", "chunk", "(Lcom/safebuffer/app/data/local/ChunkEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sha256", "", "file", "Ljava/io/File;", "stampChunksInRange", "app_debug"})
public final class AudioChunkRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.safebuffer.app.data.local.ChunkDao dao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.safebuffer.app.data.remote.WhisperApiService api = null;
    
    @javax.inject.Inject()
    public AudioChunkRepository(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.local.ChunkDao dao, @org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.remote.WhisperApiService api) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveChunk(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.local.ChunkEntity chunk, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.safebuffer.app.data.local.ChunkEntity>> getAllChunks() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.safebuffer.app.data.local.ChunkEntity>> getLockedChunks() {
        return null;
    }
    
    /**
     * 특정 시간 구간 잠금
     * [fromMs ~ toMs] 사이에 걸친 청크를 모두 isLocked = true 로 변경
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object lockRange(long fromMs, long toMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 48시간 초과 미잠금 청크 삭제
     * - DB 레코드 삭제
     * - 실제 파일도 함께 삭제
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object cleanupExpired(long cutoffMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object count(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    /**
     * 모든 청크 + 파일 완전 삭제 (베타 테스트/초기화용)
     * 잠긴 증거 포함 전부 삭제됨.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object resetAll(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 잠긴 구간의 파일들에 SHA-256 해시를 계산하고 서버 타임스탬프를 발급받는다.
     *
     * - 파일 해시 계산 → DB 저장
     * - 서버(/api/stamp)에 해시 목록 전송 → 서버 수신 시각 + HMAC 서명 반환
     * - 서버 응답 → DB 저장
     *
     * 네트워크 실패 시 해시만 저장하고 조용히 종료 (잠금은 이미 완료됨).
     * IO 스레드에서 호출해야 함.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object stampChunksInRange(long fromMs, long toMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * SHA-256 해시 계산
     */
    private final java.lang.String sha256(java.io.File file) {
        return null;
    }
}