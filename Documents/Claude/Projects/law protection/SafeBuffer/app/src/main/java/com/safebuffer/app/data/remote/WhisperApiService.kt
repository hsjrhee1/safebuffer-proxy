package com.safebuffer.app.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

// ── 타임스탬프 요청/응답 ──────────────────────────────────────
data class StampRequest(
    val hashes: List<String>,
    val deviceTime: String          // ISO 8601, 기기 시각 (참고용)
)

data class StampResponse(
    val token: String,              // 서버 발급 고유 UUID
    val serverTime: String,         // 서버 수신 시각 (ISO 8601)
    val hashes: List<String>,
    val sig: String                 // HMAC-SHA256 서명
)

/**
 * OpenAI Whisper API
 * POST https://api.openai.com/v1/audio/transcriptions
 *
 * 요청: multipart/form-data
 *   - file    : 오디오 파일 (.m4a)
 *   - model   : "whisper-1"
 *   - language: "ko"
 *   - response_format: "verbose_json" (타임스탬프 포함)
 */
interface WhisperApiService {

    @Multipart
    @POST("api/transcribe")
    suspend fun transcribe(
        @Part file: MultipartBody.Part
    ): WhisperResponse

    @POST("api/stamp")
    suspend fun stamp(
        @Body request: StampRequest
    ): StampResponse
}

// ──────────────────────────────────────────────────────────────
// 응답 모델
// ──────────────────────────────────────────────────────────────

data class WhisperResponse(
    val text: String,                          // 전체 텍스트
    val segments: List<WhisperSegment>?        // 타임스탬프별 세그먼트
)

data class WhisperSegment(
    val id: Int,
    val start: Double,                         // 청크 시작 기준 상대 초(seconds)
    val end: Double,
    val text: String,

    @SerializedName("no_speech_prob")
    val noSpeechProb: Double = 0.0,            // 높을수록 무음 가능성↑

    @SerializedName("avg_logprob")
    val avgLogprob: Double = 0.0,              // 낮을수록 신뢰도↓ (보통 -1.0 이하면 불신)

    @SerializedName("compression_ratio")
    val compressionRatio: Double = 1.0         // 2.4 이상이면 반복/환각 의심
)
