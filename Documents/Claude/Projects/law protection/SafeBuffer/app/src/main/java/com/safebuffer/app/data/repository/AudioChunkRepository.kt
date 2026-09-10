package com.safebuffer.app.data.repository

import android.util.Log
import com.safebuffer.app.data.local.ChunkDao
import com.safebuffer.app.data.local.ChunkEntity
import com.safebuffer.app.data.remote.StampRequest
import com.safebuffer.app.data.remote.WhisperApiService
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioChunkRepository @Inject constructor(
    private val dao: ChunkDao,
    private val api: WhisperApiService
) {

    // 청크 저장
    suspend fun saveChunk(chunk: ChunkEntity): Long = dao.insert(chunk)

    // 전체 청크 스트림 (UI 구독용)
    fun getAllChunks(): Flow<List<ChunkEntity>> = dao.getAllChunks()

    // 잠긴 청크만
    fun getLockedChunks(): Flow<List<ChunkEntity>> = dao.getLockedChunks()

    /**
     * 특정 시간 구간 잠금
     * [fromMs ~ toMs] 사이에 걸친 청크를 모두 isLocked = true 로 변경
     */
    suspend fun lockRange(fromMs: Long, toMs: Long) {
        dao.lockChunksInRange(fromMs, toMs)
    }

    /**
     * 48시간 초과 미잠금 청크 삭제
     * - DB 레코드 삭제
     * - 실제 파일도 함께 삭제
     */
    suspend fun cleanupExpired(cutoffMs: Long): Int {
        val expired = dao.getExpiredUnlockedChunks(cutoffMs)
        var deleted = 0
        expired.forEach { chunk ->
            File(chunk.filePath).apply {
                if (exists()) delete()
            }
            dao.delete(chunk)
            deleted++
        }
        return deleted
    }

    // 전체 청크 수 (디버그/통계)
    suspend fun count(): Int = dao.count()

    /**
     * 모든 청크 + 파일 완전 삭제 (베타 테스트/초기화용)
     * 잠긴 증거 포함 전부 삭제됨.
     */
    suspend fun resetAll() {
        val all = dao.getAllOnce()
        all.forEach { chunk ->
            File(chunk.filePath).apply { if (exists()) delete() }
        }
        dao.deleteAll()
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
    suspend fun stampChunksInRange(fromMs: Long, toMs: Long) {
        val chunks = dao.getChunksInRange(fromMs, toMs).filter { it.isLocked }
        if (chunks.isEmpty()) return

        // ① 각 파일 SHA-256 계산 + DB 저장
        val hashes = mutableListOf<String>()
        for (chunk in chunks) {
            val file = File(chunk.filePath)
            if (!file.exists()) continue
            val hash = sha256(file)
            dao.updateFileHash(chunk.id, hash)
            hashes.add(hash)
        }
        if (hashes.isEmpty()) return

        // ② 서버에 타임스탬프 요청
        try {
            val deviceTime = Instant.now().toString()
            val response = api.stamp(StampRequest(hashes = hashes, deviceTime = deviceTime))
            // ③ 서버 응답 저장 (token = UUID + HMAC 서명 합쳐서 저장)
            val tokenWithSig = "${response.token}|${response.sig}"
            for (chunk in chunks) {
                dao.updateServerStamp(chunk.id, response.serverTime, tokenWithSig)
            }
            Log.i("Stamp", "타임스탬프 발급 완료: ${response.serverTime} / token=${response.token}")
        } catch (e: Exception) {
            // 서버 오류 → 해시는 이미 저장됐으므로 조용히 무시
            Log.w("Stamp", "서버 타임스탬프 발급 실패 (해시는 저장됨): ${e.message}")
        }
    }

    /** SHA-256 해시 계산 */
    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
