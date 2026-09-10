package com.safebuffer.app.data.model

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
data class TranscriptSegment(
    val id: Int,
    val absoluteStartMs: Long,
    val absoluteEndMs: Long,
    val text: String,
    val isSelected: Boolean = false
)
