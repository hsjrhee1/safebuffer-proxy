package com.safebuffer.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedRange(range: SavedRangeEntity)

    @Query("SELECT * FROM saved_ranges ORDER BY requestedStartMs DESC")
    fun getSavedRanges(): Flow<List<SavedRangeEntity>>

    @Query("""
        SELECT * FROM saved_ranges
        WHERE requestedStartMs <= :chunkEndMs AND requestedEndMs >= :chunkStartMs
        ORDER BY requestedStartMs ASC
        LIMIT 1
    """)
    suspend fun getFirstSavedRangeOverlapping(
        chunkStartMs: Long,
        chunkEndMs: Long
    ): SavedRangeEntity?

    @Query("""
        SELECT
            (SELECT COUNT(*) FROM saved_ranges) +
            (SELECT COUNT(DISTINCT CASE
                WHEN saveGroupId IS NULL THEN -id
                ELSE saveGroupId
            END)
             FROM audio_chunks
             WHERE isLocked = 1
             AND (saveGroupId IS NULL OR saveGroupId NOT IN
                 (SELECT saveGroupId FROM saved_ranges)))
    """)
    fun observeLogicalSavedCount(): Flow<Int>

    @Query("""
        SELECT
            (SELECT COUNT(*) FROM saved_ranges) +
            (SELECT COUNT(DISTINCT CASE
                WHEN saveGroupId IS NULL THEN -id
                ELSE saveGroupId
            END)
             FROM audio_chunks
             WHERE isLocked = 1
             AND (saveGroupId IS NULL OR saveGroupId NOT IN
                 (SELECT saveGroupId FROM saved_ranges)))
    """)
    suspend fun countLogicalSaved(): Int

    @Query("DELETE FROM saved_ranges")
    suspend fun deleteAllSavedRanges()

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
        AND NOT EXISTS (
            SELECT 1 FROM saved_ranges
            WHERE requestedStartMs <= audio_chunks.endTimeMs
            AND requestedEndMs >= audio_chunks.startTimeMs
        )
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

    @Query("""
        UPDATE audio_chunks
        SET isLocked = 1, saveGroupId = :groupId
        WHERE isLocked = 0
        AND startTimeMs <= :toMs AND endTimeMs >= :fromMs
    """)
    suspend fun lockChunksInRangeAsGroup(fromMs: Long, toMs: Long, groupId: Long)

    // 단건 삭제 (파일 삭제 후 DB도 삭제)
    @Delete
    suspend fun delete(chunk: ChunkEntity)

    @Query("SELECT * FROM audio_chunks WHERE saveGroupId = :groupId")
    suspend fun getChunksBySaveGroupId(groupId: Long): List<ChunkEntity>

    @Query("UPDATE audio_chunks SET saveGroupId = :groupId, isLocked = 1 WHERE id = :chunkId")
    suspend fun moveChunkToSaveGroup(chunkId: Long, groupId: Long)

    @Query("DELETE FROM saved_ranges WHERE saveGroupId = :groupId")
    suspend fun deleteSavedRange(groupId: Long)

    // 만료 청크 일괄 삭제
    @Query("""
        DELETE FROM audio_chunks
        WHERE isLocked = 0
        AND createdAt < :cutoffMs
        AND NOT EXISTS (
            SELECT 1 FROM saved_ranges
            WHERE requestedStartMs <= audio_chunks.endTimeMs
            AND requestedEndMs >= audio_chunks.startTimeMs
        )
    """)
    suspend fun deleteExpiredChunks(cutoffMs: Long)

    // 총 청크 수
    @Query("SELECT COUNT(*) FROM audio_chunks")
    suspend fun count(): Int

    // 잠긴 청크 수 (Free 티어 제한 체크)
    @Query("SELECT COUNT(*) FROM audio_chunks WHERE isLocked = 1")
    suspend fun countLocked(): Int

    // 특정 범위에서 아직 안 잠긴 청크 수 — 잠금 전 한도 미리 계산용
    @Query("""
        SELECT COUNT(*) FROM audio_chunks
        WHERE isLocked = 0
        AND startTimeMs <= :toMs AND endTimeMs >= :fromMs
    """)
    suspend fun countUnlockedInRange(fromMs: Long, toMs: Long): Int

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
