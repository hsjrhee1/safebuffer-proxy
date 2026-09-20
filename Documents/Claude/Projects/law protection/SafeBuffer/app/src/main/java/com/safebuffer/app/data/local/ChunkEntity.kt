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
    val serverToken: String? = null,       // 서버 발급 토큰 (HMAC-SHA256 서명 포함)

    /**
     * 녹음 중 일정 간격으로 수집한 진폭 요약. 쉼표로 구분된 0~100 정수.
     *
     * 홈 화면 48시간 타임라인을 그리는 데 쓴다. 파일을 디코딩해서 계산하면
     * 청크 하나에 수 초씩 걸려 144개를 훑는 게 불가능하므로,
     * 녹음하면서 MediaRecorder.getMaxAmplitude() 로 미리 모아둔다. (디코딩 없음)
     */
    val amplitudes: String? = null,

    /**
     * 저장 묶음 ID. 한 번 '저장'할 때 포함된 청크들이 같은 값을 갖는다.
     *
     * 저장 한 번이 목록에서 항목 하나로 보여야 한다. 이 값이 없으면
     * 10분 청크들이 제각각 흩어져 나타나고, 문서도 조각별로 만들어진다.
     * null = 아직 저장되지 않은 버퍼 청크.
     */
    val saveGroupId: Long? = null
)
