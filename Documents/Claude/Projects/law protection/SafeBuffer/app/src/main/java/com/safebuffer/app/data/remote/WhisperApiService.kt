package com.safebuffer.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

// ──────────────────────────────────────────────────────────────────────────────
// suspend fun 대신 Call<ResponseBody> 사용 이유:
//
// Kotlin suspend fun은 컴파일 시 Continuation<T>로 변환된다.
// R8(릴리즈 빌드 최적화)은 이 Continuation<ResponseBody>의 제네릭 타입 T를 지워버리고,
// Retrofit이 런타임에 반환 타입을 리플렉션으로 읽으려 하면
//   java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType
// 에러가 발생한다. (Gson 제거와 무관한 Retrofit + R8 조합 문제)
//
// Call<ResponseBody>는 리플렉션 없이 직접 처리되므로 R8에서 안전하다.
// ──────────────────────────────────────────────────────────────────────────────

interface WhisperApiService {

    @Multipart
    @POST("api/transcribe")
    fun transcribe(
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @POST("api/stamp")
    fun stamp(
        @Body body: RequestBody
    ): Call<ResponseBody>
}

// ──────────────────────────────────────────────────────────────────────────────
// WhisperSegment — org.json으로 파싱 (parseWhisperResponse 에서 직접 필드명 지정)
// ──────────────────────────────────────────────────────────────────────────────
data class WhisperSegment(
    val id: Int,
    val start: Double,
    val end: Double,
    val text: String,
    val noSpeechProb: Double = 0.0,
    val avgLogprob: Double = 0.0,
    val compressionRatio: Double = 1.0
)
