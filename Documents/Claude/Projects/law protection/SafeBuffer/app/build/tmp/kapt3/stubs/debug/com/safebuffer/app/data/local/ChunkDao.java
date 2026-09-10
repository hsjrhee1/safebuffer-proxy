package com.safebuffer.app.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\r\n\u0002\u0010\u000e\n\u0002\b\b\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ\u000e\u0010\n\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u0014\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00110\u0010H\'J\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\b0\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\u0013\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0014\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ$\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\b0\u00112\u0006\u0010\u0016\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u0018J\u001c\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\b0\u00112\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u0014\u0010\u001a\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00110\u0010H\'J\u0016\u0010\u001b\u001a\u00020\r2\u0006\u0010\u0007\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ\u001e\u0010\u001c\u001a\u00020\u00062\u0006\u0010\u0016\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u0018J\u001e\u0010\u001d\u001a\u00020\u00062\u0006\u0010\u0014\u001a\u00020\r2\u0006\u0010\u001e\u001a\u00020\u001fH\u00a7@\u00a2\u0006\u0002\u0010 J&\u0010!\u001a\u00020\u00062\u0006\u0010\u0014\u001a\u00020\r2\u0006\u0010\"\u001a\u00020\u001f2\u0006\u0010#\u001a\u00020\u001fH\u00a7@\u00a2\u0006\u0002\u0010$J\u001e\u0010%\u001a\u00020\u00062\u0006\u0010\u0014\u001a\u00020\r2\u0006\u0010&\u001a\u00020\u001fH\u00a7@\u00a2\u0006\u0002\u0010 \u00a8\u0006\'"}, d2 = {"Lcom/safebuffer/app/data/local/ChunkDao;", "", "count", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "delete", "", "chunk", "Lcom/safebuffer/app/data/local/ChunkEntity;", "(Lcom/safebuffer/app/data/local/ChunkEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteAll", "deleteExpiredChunks", "cutoffMs", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllChunks", "Lkotlinx/coroutines/flow/Flow;", "", "getAllOnce", "getById", "id", "getChunksInRange", "fromMs", "toMs", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getExpiredUnlockedChunks", "getLockedChunks", "insert", "lockChunksInRange", "updateFileHash", "hash", "", "(JLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateServerStamp", "serverTime", "token", "(JLjava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateTranscription", "transcription", "app_debug"})
@androidx.room.Dao()
public abstract interface ChunkDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.local.ChunkEntity chunk, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM audio_chunks ORDER BY startTimeMs DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.safebuffer.app.data.local.ChunkEntity>> getAllChunks();
    
    @androidx.room.Query(value = "SELECT * FROM audio_chunks WHERE isLocked = 1 ORDER BY startTimeMs DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.safebuffer.app.data.local.ChunkEntity>> getLockedChunks();
    
    @androidx.room.Query(value = "\n        SELECT * FROM audio_chunks\n        WHERE isLocked = 0\n        AND createdAt < :cutoffMs\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getExpiredUnlockedChunks(long cutoffMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.safebuffer.app.data.local.ChunkEntity>> $completion);
    
    @androidx.room.Query(value = "\n        SELECT * FROM audio_chunks\n        WHERE startTimeMs <= :toMs AND endTimeMs >= :fromMs\n        ORDER BY startTimeMs ASC\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getChunksInRange(long fromMs, long toMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.safebuffer.app.data.local.ChunkEntity>> $completion);
    
    @androidx.room.Query(value = "\n        UPDATE audio_chunks\n        SET isLocked = 1\n        WHERE startTimeMs <= :toMs AND endTimeMs >= :fromMs\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object lockChunksInRange(long fromMs, long toMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.local.ChunkEntity chunk, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "\n        DELETE FROM audio_chunks\n        WHERE isLocked = 0\n        AND createdAt < :cutoffMs\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteExpiredChunks(long cutoffMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM audio_chunks")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object count(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM audio_chunks")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllOnce(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.safebuffer.app.data.local.ChunkEntity>> $completion);
    
    @androidx.room.Query(value = "DELETE FROM audio_chunks")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAll(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM audio_chunks WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getById(long id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.safebuffer.app.data.local.ChunkEntity> $completion);
    
    @androidx.room.Query(value = "UPDATE audio_chunks SET transcription = :transcription WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateTranscription(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String transcription, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE audio_chunks SET fileHash = :hash WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateFileHash(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String hash, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE audio_chunks SET serverTimestamp = :serverTime, serverToken = :token WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateServerStamp(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String serverTime, @org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}