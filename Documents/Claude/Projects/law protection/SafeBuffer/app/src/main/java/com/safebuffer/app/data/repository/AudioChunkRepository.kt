package com.safebuffer.app.data.repository

import android.content.Context
import android.util.Log
import com.safebuffer.app.data.local.ChunkDao
import com.safebuffer.app.data.local.ChunkEntity
import com.safebuffer.app.data.local.SavedRangeEntity
import com.safebuffer.app.data.remote.WhisperApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioChunkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ChunkDao,
    private val api: WhisperApiService
) {

    // 청크 저장
    suspend fun saveChunk(chunk: ChunkEntity): Long = dao.insert(chunk)

    // 전체 청크 스트림 (UI 구독용)
    fun getAllChunks(): Flow<List<ChunkEntity>> = dao.getAllChunks()

    // 잠긴 청크만
    fun getLockedChunks(): Flow<List<ChunkEntity>> = dao.getLockedChunks()
    fun getSavedRanges(): Flow<List<SavedRangeEntity>> = dao.getSavedRanges()
    fun getLogicalSavedCount(): Flow<Int> = dao.observeLogicalSavedCount()
    suspend fun countLogicalSaved(): Int = dao.countLogicalSaved()

    /**
     * 특정 시간 구간 잠금
     * [fromMs ~ toMs] 사이에 걸친 청크를 모두 isLocked = true 로 변경
     */
    suspend fun lockRange(fromMs: Long, toMs: Long, saveGroupId: Long? = null) {
        if (saveGroupId == null) dao.lockChunksInRange(fromMs, toMs)
        else {
            dao.upsertSavedRange(SavedRangeEntity(saveGroupId, fromMs, toMs))
            dao.lockChunksInRangeAsGroup(fromMs, toMs, saveGroupId)
        }
    }

    /**
     * 48시간 초과 미잠금 청크 삭제
     * - DB 레코드 삭제
     * - 실제 파일도 함께 삭제
     */
    suspend fun cleanupExpired(cutoffMs: Long): Int = withContext(Dispatchers.IO) {
        val expired = dao.getExpiredUnlockedChunks(cutoffMs)
        var deleted = 0
        expired.forEach { chunk ->
            File(chunk.filePath).apply {
                if (exists()) delete()
            }
            dao.delete(chunk)
            deleted++
        }
        deleted
    }

    // 전체 청크 수 (디버그/통계)
    suspend fun count(): Int = dao.count()

    // 잠긴 청크 수 (Free 티어 제한 체크용)
    suspend fun countLocked(): Int = dao.countLocked()

    // 잠긴 청크 전체 한번에 조회 (백업용)
    suspend fun getAllLockedOnce(): List<ChunkEntity> =
        dao.getAllOnce().filter { it.isLocked }

    /** 구간과 겹치는 청크를 시간순으로 — 미리듣기용 */
    suspend fun chunksInRange(fromMs: Long, toMs: Long): List<ChunkEntity> =
        dao.getChunksInRange(fromMs, toMs)

    // 범위 내 미잠금 청크 수 — 잠금 전 한도 미리 계산용
    suspend fun countUnlockedInRange(fromMs: Long, toMs: Long): Int =
        dao.countUnlockedInRange(fromMs, toMs)

    /**
     * 파일이 실제로 존재하지 않는 "유령 청크" DB 레코드 정리
     * 앱 시작 시 호출 — 불완전한 reset 후 남은 잔존 레코드 제거
     */
    suspend fun cleanupOrphanedChunks() = withContext(Dispatchers.IO) {
        val all = dao.getAllOnce()
        all.forEach { chunk ->
            if (!File(chunk.filePath).exists()) {
                dao.delete(chunk)
            }
        }
    }

    /**
     * 모든 청크 + 파일 완전 삭제 (베타 테스트/초기화용)
     * 잠긴 증거 포함 전부 삭제됨.
     */
    suspend fun resetAll() = withContext(Dispatchers.IO) {
        // 전체 초기화에서는 DB가 알고 있는 파일뿐 아니라 legacy/orphan 파일도
        // 남으면 안 된다. recorder가 완전히 종료된 뒤 호출되는 이 경로에서만
        // 앱 전용 녹음 디렉터리를 authoritative cleanup 대상으로 삼는다.
        val chunksDir = File(context.filesDir, "chunks")
        chunksDir.listFiles()?.forEach { file ->
            check(file.deleteRecursively()) {
                "Failed to delete recording during reset: ${file.absolutePath}"
            }
        }
        check(chunksDir.exists() || chunksDir.mkdirs()) {
            "Failed to create recording directory after reset: ${chunksDir.absolutePath}"
        }
        dao.deleteAll()
        dao.deleteAllSavedRanges()
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
    suspend fun stampChunksInRange(fromMs: Long, toMs: Long): Unit = withContext(Dispatchers.IO) {
        val chunks = dao.getChunksInRange(fromMs, toMs).filter { it.isLocked }
        if (chunks.isEmpty()) return@withContext

        // ① 각 파일 SHA-256 계산 + DB 저장
        // ★ 반드시 IO 스레드 — 구간이 길면 수십 개 청크(수십 MB)를 읽으므로
        //   호출부(viewModelScope = Main)에서 실행되면 ANR 이 발생한다.
        val hashes = mutableListOf<String>()
        for (chunk in chunks) {
            val file = File(chunk.filePath)
            if (!file.exists()) continue
            val hash = sha256(file)
            dao.updateFileHash(chunk.id, hash)
            hashes.add(hash)
        }
        if (hashes.isEmpty()) return@withContext

        // ② 서버에 타임스탬프 요청 — org.json으로 직접 빌드/파싱 (Gson TypeToken R8 에러 방지)
        try {
            val deviceTime = Instant.now().toString()

            // 요청 JSON 직접 빌드
            val reqJson = org.json.JSONObject().apply {
                val arr = org.json.JSONArray()
                hashes.forEach { arr.put(it) }
                put("hashes", arr)
                put("deviceTime", deviceTime)
            }
            val requestBody = reqJson.toString()
                .toRequestBody("application/json".toMediaType())

            // 응답 JSON 직접 파싱 — Call.execute() (suspend 대신, R8 제네릭 소거 방지)
            // 이미 IO 컨텍스트 안이므로 추가 withContext 불필요
            val stampResponse = api.stamp(requestBody).execute()
            if (!stampResponse.isSuccessful) {
                throw Exception("타임스탬프 서버 오류: HTTP ${stampResponse.code()}")
            }
            val rawJson = stampResponse.body()?.string() ?: ""
            val root = org.json.JSONObject(rawJson)
            val token      = root.getString("token")
            val serverTime = root.getString("serverTime")
            val sig        = root.getString("sig")

            // ③ 서버 응답 저장
            val tokenWithSig = "$token|$sig"
            for (chunk in chunks) {
                dao.updateServerStamp(chunk.id, serverTime, tokenWithSig)
            }
            Log.i("Stamp", "타임스탬프 발급 완료: $serverTime / token=$token")
        } catch (e: Exception) {
            // 서버 오류 → 해시는 이미 저장됐으므로 조용히 무시
            Log.w("Stamp", "서버 타임스탬프 발급 실패 (해시는 저장됨): ${e.message}")
        }
    }

    /**
     * Pro 티어: SHA-256 해시만 로컬에 저장 (서버 전송 없음)
     */
    suspend fun hashChunksInRange(fromMs: Long, toMs: Long): Unit = withContext(Dispatchers.IO) {
        // ★ 반드시 IO 스레드 — 위 stampChunksInRange 와 동일 이유 (ANR 방지)
        val chunks = dao.getChunksInRange(fromMs, toMs).filter { it.isLocked }
        for (chunk in chunks) {
            val file = File(chunk.filePath)
            if (!file.exists()) continue
            val hash = sha256(file)
            dao.updateFileHash(chunk.id, hash)
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
