package com.safebuffer.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {

    // 새 청크 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: ChunkEntity): Long

    // 전체 청크 목록 (최신순) — UI 타임라인에 사용
    @Query("SELECT * FROM audio_chunks ORDER BY startTimeMs DESC")
    fun getAllChunks(): Flow<List<ChunkEntity>>

    // 잠금된 청크만
    @Query("SELECT * FROM audio_chunks WHERE isLocked = 1 ORDER BY startTimeMs DESC")
    fun getLockedChunks(): Flow<List<ChunkEntity>>

    // 48시간 초과 & 잠금 안 된 청크 — CleanupWorker 대상
    @Query("""
        SELECT * FROM audio_chunks
        WHERE isLocked = 0
        AND createdAt < :cutoffMs
    """)
    suspend fun getExpiredUnlockedChunks(cutoffMs: Long): List<ChunkEntity>

    // 특정 시간 구간과 겹치는 청크 조회 — 조금이라도 겹치면 포함
    @Query("""
        SELECT * FROM audio_chunks
        WHERE startTimeMs <= :toMs AND endTimeMs >= :fromMs
        ORDER BY startTimeMs ASC
    """)
    suspend fun getChunksInRange(fromMs: Long, toMs: Long): List<ChunkEntity>

    // 구간과 겹치는 청크를 모두 잠금 (청크가 선택 구간과 조금이라도 겹치면 잠금)
    @Query("""
        UPDATE audio_chunks
        SET isLocked = 1
        WHERE startTimeMs <= :toMs AND endTimeMs >= :fromMs
    """)
    suspend fun lockChunksInRange(fromMs: Long, toMs: Long)

    // 단건 삭제 (파일 삭제 후 DB도 삭제)
    @Delete
    suspend fun delete(chunk: ChunkEntity)

    // 만료 청크 일괄 삭제
    @Query("""
        DELETE FROM audio_chunks
        WHERE isLocked = 0
        AND createdAt < :cutoffMs
    """)
    suspend fun deleteExpiredChunks(cutoffMs: Long)

    // 총 청크 수
    @Query("SELECT COUNT(*) FROM audio_chunks")
    suspend fun count(): Int

    // 전체 청크 일괄 조회 (초기화용)
    @Query("SELECT * FROM audio_chunks")
    suspend fun getAllOnce(): List<ChunkEntity>

    // 전체 청크 일괄 삭제 (초기화용)
    @Query("DELETE FROM audio_chunks")
    suspend fun deleteAll()

    // ID로 단건 조회
    @Query("SELECT * FROM audio_chunks WHERE id = :id")
    suspend fun getById(id: Long): ChunkEntity?

    // 전사 결과 저장
    @Query("UPDATE audio_chunks SET transcription = :transcription WHERE id = :id")
    suspend fun updateTranscription(id: Long, transcription: String)

    // 파일 해시 저장
    @Query("UPDATE audio_chunks SET fileHash = :hash WHERE id = :id")
    suspend fun updateFileHash(id: Long, hash: String)

    // 서버 타임스탬프 토큰 저장
    @Query("UPDATE audio_chunks SET serverTimestamp = :serverTime, serverToken = :token WHERE id = :id")
    suspend fun updateServerStamp(id: Long, serverTime: String, token: String)
}
