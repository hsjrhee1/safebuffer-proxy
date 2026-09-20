package com.safebuffer.app.util

import android.content.Context
import android.net.Uri
import com.safebuffer.app.data.local.ChunkEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 보호된 녹음 ZIP 백업 / 복원
 *
 * [ZIP 구조]
 * SafeBuffer_backup_YYYYMMDD_HHmmss.zip
 * ├── audio_1.m4a          ← 녹음 원본
 * ├── audio_2.m4a
 * └── metadata.json        ← 시각·해시·타임스탬프·전사 텍스트
 *
 * [복원 시 SHA-256 재검증]
 * 가져오기 시 각 파일의 SHA-256을 재계산해 metadata의 해시와 비교.
 * 불일치 파일은 건너뜀 → 변조된 파일은 복원되지 않음.
 */
object BackupManager {

    private val fileFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA)

    // ── 내보내기 ──────────────────────────────────────────────

    /**
     * 잠긴 청크 목록 → ZIP 파일 생성 (공유용)
     * @param context
     * @param lockedChunks isLocked = true 인 청크 리스트
     * @return 생성된 ZIP 파일 (getExternalFilesDir/backups/ 에 저장)
     */
    fun exportBackup(context: Context, lockedChunks: List<ChunkEntity>): Result<File> {
        return try {
            val backupDir = File(context.getExternalFilesDir(null), "backups").also { it.mkdirs() }
            val zipFile   = File(backupDir, "SafeBuffer_backup_${fileFmt.format(Date())}.zip")
            val metaArray = JSONArray()

            ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
                lockedChunks.forEachIndexed { idx, chunk ->
                    val audioFile = File(chunk.filePath)
                    if (!audioFile.exists()) return@forEachIndexed

                    val entryName = "audio_${idx + 1}.m4a"

                    // 오디오 파일 ZIP 항목 추가
                    zos.putNextEntry(ZipEntry(entryName))
                    audioFile.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()

                    // 메타데이터 JSON 추가
                    metaArray.put(JSONObject().apply {
                        put("file",            entryName)
                        put("startTimeMs",     chunk.startTimeMs)
                        put("endTimeMs",       chunk.endTimeMs)
                        put("fileHash",        chunk.fileHash        ?: "")
                        put("serverTimestamp", chunk.serverTimestamp ?: "")
                        put("serverToken",     chunk.serverToken     ?: "")
                        put("transcription",   chunk.transcription   ?: "")
                    })
                }

                // metadata.json 추가
                zos.putNextEntry(ZipEntry("metadata.json"))
                zos.write(metaArray.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── 가져오기 ──────────────────────────────────────────────

    data class ImportResult(
        val chunks: List<ChunkEntity>,  // 복원 성공한 청크 (DB에 insert 필요)
        val failedCount: Int            // 해시 불일치 등으로 건너뛴 파일 수
    )

    /**
     * ZIP 파일에서 증거 복원
     *
     * 1. ZIP 압축 해제 → 임시 디렉토리
     * 2. metadata.json 파싱
     * 3. 각 파일 SHA-256 재검증
     * 4. 앱 내부 chunks/ 로 복사
     * 5. ChunkEntity 목록 반환 (호출자가 DB insert)
     *
     * @param context
     * @param uri   ActivityResultContracts.OpenDocument 로 선택한 ZIP URI
     */
    fun importBackup(context: Context, uri: Uri): Result<ImportResult> {
        val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}").also { it.mkdirs() }
        return try {
            // ① ZIP 압축 해제
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        // ★ ZIP 경로 탈주 방지 — 정규화 경로가 tempDir 밖이면 건너뜀
                        if (!outFile.canonicalPath.startsWith(
                                tempDir.canonicalPath + File.separator)) {
                            android.util.Log.w("BackupManager",
                                "경로 탈주 시도 무시: ${entry.name}")
                            zis.closeEntry()
                            entry = zis.nextEntry
                            continue
                        }
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zis.copyTo(it) }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: return Result.failure(Exception("파일을 열 수 없습니다"))

            // ② metadata.json 파싱
            val metaFile = File(tempDir, "metadata.json")
            if (!metaFile.exists()) {
                return Result.failure(Exception("백업 파일이 손상되었습니다 (metadata.json 없음)"))
            }
            val metaArray = JSONArray(metaFile.readText())

            // ③ 복원 대상 디렉토리
            val destDir = File(context.filesDir, "chunks").also { it.mkdirs() }
            val restoredChunks = mutableListOf<ChunkEntity>()
            var failedCount = 0

            for (i in 0 until metaArray.length()) {
                val meta      = metaArray.getJSONObject(i)
                val entryName = meta.getString("file")
                val tempAudio = File(tempDir, entryName)

                if (!tempAudio.exists()) {
                    android.util.Log.w("BackupManager", "파일 없음: $entryName")
                    failedCount++
                    continue
                }

                // ④ SHA-256 검증 (해시가 기록된 경우만)
                val recordedHash = meta.getString("fileHash")
                if (recordedHash.isNotEmpty()) {
                    val actualHash = sha256(tempAudio)
                    if (actualHash != recordedHash) {
                        android.util.Log.w("BackupManager", "해시 불일치 — 복원 건너뜀: $entryName")
                        failedCount++
                        continue
                    }
                }

                // ⑤ 내부 저장소로 복사
                val destFile = File(destDir, "restored_${System.currentTimeMillis()}_${i}_$entryName")
                tempAudio.copyTo(destFile, overwrite = true)

                restoredChunks.add(ChunkEntity(
                    startTimeMs     = meta.getLong("startTimeMs"),
                    endTimeMs       = meta.getLong("endTimeMs"),
                    filePath        = destFile.absolutePath,
                    isLocked        = true,
                    fileHash        = meta.getString("fileHash").ifEmpty { null },
                    serverTimestamp = meta.getString("serverTimestamp").ifEmpty { null },
                    serverToken     = meta.getString("serverToken").ifEmpty { null },
                    transcription   = meta.getString("transcription").ifEmpty { null }
                ))
            }

            Result.success(ImportResult(restoredChunks, failedCount))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── SHA-256 ───────────────────────────────────────────────

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) md.update(buf, 0, read)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
