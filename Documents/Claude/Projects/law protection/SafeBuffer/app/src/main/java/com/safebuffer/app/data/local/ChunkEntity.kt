package com.safebuffer.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 오디오 청크 1개 = 10분짜리 .m4a 파일 1개
 *
 * [저장 구조]
 * id          : 자동 증가 PK
 * startTimeMs : 녹음 시작 Unix timestamp (ms)
 * endTimeMs   : 녹음 종료 Unix timestamp (ms)
 * filePath    : 기기 내부 저장소 절대 경로
 * isLocked    : true = 유저가 잠금 → 자동삭제 제외
 * createdAt   : 생성 시각 (삭제 기준)
 */
@Entity(tableName = "audio_chunks")
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val filePath: String,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val transcription: String? = null,   // Phase 2: Whisper STT 결과
    val note: String? = null,            // Phase 2: LexNote 연동 메모

    // ── 원본 증명 (SHA-256 + 서버 타임스탬프) ──
    val fileHash: String? = null,          // 파일 SHA-256 해시 (변조 여부 검증)
    val serverTimestamp: String? = null,   // 서버 수신 시각 (ISO 8601)
    val serverToken: String? = null        // 서버 발급 토큰 (HMAC-SHA256 서명 포함)
)
