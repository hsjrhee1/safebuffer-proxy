package com.safebuffer.app.data.repository

import com.safebuffer.app.data.local.ChunkDao
import com.safebuffer.app.data.model.TranscriptSegment
import com.safebuffer.app.data.remote.WhisperApiService
import com.safebuffer.app.data.remote.WhisperSegment
import com.safebuffer.app.util.AudioAnalysisResult
import com.safebuffer.app.util.AudioWaveformLoader
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class WhisperRepository @Inject constructor(
    private val api: WhisperApiService,
    private val chunkDao: ChunkDao
) {
    companion object {
        /** 로컬에서 무음으로 확인된 청크 표식 — 빈 배열("[]")과 구분해야 한다 */
        const val MARKER_SILENT = "__SILENT__"
    }

    // ── 사전 필터 (Whisper 호출 전) ──────────────────────────────
    // 파일 전체 250ms 버킷 중 **최댓값**이 이 값 미만이면 무음으로 판단해 API 호출을 건너뛴다.
    //
    // 평균이 아니라 최댓값 기준이므로 문턱은 조금 높아도 된다.
    // 다만 마이크 게인은 기기마다 크게 다르므로 여전히 보수적으로 잡는다 —
    // 애매하면 Whisper 에 보내는 쪽이 안전하다. 잘못 버리면 증거가 사라진다.
    private val SILENCE_THRESHOLD = 0.006f

    // ── 사후 필터 (Whisper 응답 후) ──────────────────────────────
    // ① 무음 확률: 이 값 이상이면 음성 없다고 판단
    private val MAX_NO_SPEECH_PROB = 0.6

    // ② 신뢰도: avg_logprob 이 값 미만이면 인식 결과 신뢰 불가
    private val MIN_AVG_LOGPROB = -1.0

    // ③ 압축비: 이 값 이상이면 반복/환각 텍스트 의심
    private val MAX_COMPRESSION_RATIO = 2.4

    // ④ 최소 발화 길이: 0.3초 미만은 잡음으로 처리
    private val MIN_SPEECH_DURATION_SEC = 0.3

    // ── Amplitude 세그먼트 필터 (Whisper 응답 + 오디오 분석 교차) ──
    // 세 조건 ALL 충족 시에만 "명백한 무음"으로 간주 → 보수적 AND 조건
    // raw RMS 기준 (0.0 ~ 1.0 스케일)
    private val SEG_AVG_SILENCE    = 0.002f   // 평균 RMS 이하
    private val SEG_PEAK_SILENCE   = 0.006f   // 최대 RMS 이하
    private val SEG_ACTIVE_THRESH  = 0.004f   // 이 값 넘으면 "활동 있음"으로 간주
    private val SEG_ACTIVE_RATIO   = 0.05f    // 활동 버킷 비율 5% 미만

    // ⑤ 환각 blacklist: (문장 일치) AND (짧거나 신뢰도 낮음)일 때만 제거
    //    실제로 그 말을 했을 경우를 위해 단독 조건으로는 삭제 안 함
    private val HALLUCINATION_PHRASES = setOf(
        "감사합니다", "감사합니다.", "고맙습니다",
        "시청해주셔서 감사합니다", "시청해 주셔서 감사합니다",
        "구독과 좋아요 부탁드립니다", "좋아요 구독",
        "자막은 설정에서 선택하실 수 있습니다",
        "자막 제공", "자막 by", "번역 by",
        "MBC", "KBS", "SBS", "JTBC"
    )

    suspend fun transcribeRange(
        fromMs: Long,
        toMs: Long
    ): Result<List<TranscriptSegment>> {
        return try {
            val chunks = chunkDao.getChunksInRange(fromMs, toMs)

            if (chunks.isEmpty()) {
                return Result.failure(Exception(
                    "해당 구간에 녹음된 파일이 없습니다.\n" +
                    "녹음 시작 직후라면 3초 이상 기다린 뒤 다시 시도하세요."
                ))
            }

            val allSegments = mutableListOf<TranscriptSegment>()
            var segmentIdOffset = 0

            for (chunk in chunks) {
                val cached = chunk.transcription

                // ★ 캐시 정책 ────────────────────────────────────────────────
                //  MARKER_SILENT : 로컬에서 무음으로 확인됨 → 재시도 불필요, 캐시 신뢰
                //  비어있지 않은 JSON 배열 : 정상 전사 결과 → 캐시 신뢰
                //  "[]" : 구버전이 HTTP 오류를 빈 결과로 오인해 박아둔 오염 캐시.
                //         (v50 이전 버그) 캐시로 취급하지 않고 재전사한다 → 자동 복구.
                //  null/공백 : 아직 전사 안 함
                val usableCache = when {
                    cached == MARKER_SILENT              -> emptyList()
                    !cached.isNullOrBlank() && cached != "[]" -> parseSegmentsJson(cached)
                    else                                 -> null   // 재전사 필요
                }

                val segments = if (usableCache != null) {
                    usableCache.mapIndexed { i, seg -> seg.copy(id = segmentIdOffset + i) }
                } else {
                    val file = File(chunk.filePath)
                    if (!file.exists()) continue

                    // ★ 침묵 구간은 Whisper 호출 건너뜀 — hallucination 방지
                    // IO 스레드에서 실행 (MediaCodec 디코딩 → Main thread 블로킹 방지)
                    if (withContext(Dispatchers.IO) { isSilent(file) }) {
                        chunkDao.updateTranscription(chunk.id, MARKER_SILENT)
                        continue
                    }

                    val result = transcribeSingleChunk(file, chunk.startTimeMs, segmentIdOffset)
                    // ★ 결과가 있을 때만 캐싱한다.
                    //   빈 결과는 서버 일시 오류·필터 과잉일 수 있으므로 캐싱하면 안 된다.
                    //   (이것을 캐싱한 것이 "한 번 비면 영원히 비는" 버그의 원인이었다)
                    if (result.isNotEmpty()) {
                        chunkDao.updateTranscription(chunk.id, serializeSegmentsJson(result))
                    }
                    result
                }

                allSegments.addAll(segments)
                segmentIdOffset += segments.size
            }

            Result.success(allSegments.sortedBy { it.absoluteStartMs })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun transcribeSingleChunk(
        file: File,
        chunkStartMs: Long,
        idOffset: Int
    ): List<TranscriptSegment> {
        val filePart = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody("audio/mp4".toMediaType())
        )

        // Whisper API 호출 — Call.execute() 사용 (suspend 대신)
        // R8에서 suspend fun → Continuation<ResponseBody> 제네릭 소거 에러를 방지하기 위해
        // Call<ResponseBody>.execute()로 직접 호출. execute()는 블로킹이므로 IO 스레드 필요.
        val rawJson = withContext(Dispatchers.IO) {
            val response = api.transcribe(file = filePart).execute()
            // ★ HTTP 오류(4xx/5xx) 시 예외 발생 → 이 청크를 캐싱하지 않음
            // (response.body()만 체크하면 null → ""로 폴백돼 빈 결과가 영구 캐싱되는 버그 방지)
            if (!response.isSuccessful) {
                throw Exception("STT 서버 오류: HTTP ${response.code()}")
            }
            response.body()?.string() ?: ""
        }
        val whisperSegments = parseWhisperResponse(rawJson)

        // Whisper 응답 후 amplitude 분석 — 세그먼트별 silence 판정용
        val analysis: AudioAnalysisResult? = withContext(Dispatchers.IO) {
            AudioWaveformLoader.analyzeForStt(file.absolutePath)
        }

        return whisperSegments
            .filter { seg -> isValidSegment(seg) }
            .filter { seg -> !isSegmentSilent(seg, analysis) }
            .mapIndexed { i, seg ->
                TranscriptSegment(
                    id = idOffset + i,
                    absoluteStartMs = chunkStartMs + (seg.start * 1000).toLong(),
                    absoluteEndMs = chunkStartMs + (seg.end * 1000).toLong(),
                    text = seg.text.trim()
                )
            }
    }

    /**
     * Amplitude 기반 세그먼트 silence 판정.
     *
     * 보수적 AND 조건: 세 지표 모두 임계값 이하일 때만 "명백한 무음"으로 제거.
     * 하나라도 넘으면 살림 → false negative 최우선.
     *
     * @return true = 무음으로 판단 → 제거 / false = 유지
     */
    private fun isSegmentSilent(
        seg: WhisperSegment,
        analysis: AudioAnalysisResult?
    ): Boolean {
        // analysis 실패 시 → 판단 불가 → 제거하지 않음
        analysis ?: return false

        val buckets     = analysis.amplitudes.size
        val bucketMs    = analysis.bucketDurationMs

        // 세그먼트 시간(초)을 버킷 인덱스로 변환
        val startBucket = ((seg.start * 1000.0) / bucketMs).toInt().coerceIn(0, buckets - 1)
        val endBucket   = ((seg.end   * 1000.0) / bucketMs).toInt().coerceIn(0, buckets - 1)

        // 세그먼트가 버킷 2개 미만 구간이면 판단 불가 → 제거하지 않음
        if (endBucket - startBucket < 2) return false

        val segAmps = analysis.amplitudes.subList(startBucket, endBucket + 1)

        val avgAmp    = segAmps.average().toFloat()
        val peakAmp   = segAmps.maxOrNull() ?: 0f
        val active    = segAmps.count { it > SEG_ACTIVE_THRESH }
        val activeRatio = active.toFloat() / segAmps.size

        // 세 조건 모두 충족해야 제거 (AND)
        return avgAmp < SEG_AVG_SILENCE
            && peakAmp < SEG_PEAK_SILENCE
            && activeRatio < SEG_ACTIVE_RATIO
    }

    /**
     * Whisper 세그먼트가 유효한 음성인지 다단계로 검사.
     *
     * ① 무음 확률 초과 → 제거
     * ② 신뢰도(avgLogprob) 너무 낮음 → 제거
     * ③ 압축비 너무 높음(반복 텍스트) → 제거
     * ④ 발화 길이 너무 짧음 → 제거
     * ⑤ 환각 blacklist + (짧거나 신뢰도 낮음) 복합 조건 → 제거
     */
    private fun isValidSegment(seg: WhisperSegment): Boolean {
        val text = seg.text.trim()
        val duration = seg.end - seg.start

        // ① 무음 확률
        if (seg.noSpeechProb >= MAX_NO_SPEECH_PROB) return false

        // ② 신뢰도 (avg_logprob = 0이면 API가 안 보낸 것, 그 경우는 통과)
        if (seg.avgLogprob != 0.0 && seg.avgLogprob < MIN_AVG_LOGPROB) return false

        // ③ 압축비 (1.0이면 기본값/미수신, 통과)
        if (seg.compressionRatio > MAX_COMPRESSION_RATIO) return false

        // ④ 최소 발화 길이
        if (duration < MIN_SPEECH_DURATION_SEC) return false

        // ⑤ 환각 blacklist — 짧거나 신뢰도 낮을 때만 제거
        //    실제로 그 말을 했으면 duration 길고 noSpeechProb 낮으므로 살아남음
        val isBlacklisted = HALLUCINATION_PHRASES.any { phrase ->
            text.equals(phrase, ignoreCase = true) || text.contains(phrase)
        }
        if (isBlacklisted && (duration < 1.5 || seg.noSpeechProb > 0.3)) return false

        return true
    }

    /**
     * 청크 전체가 무음인지 판단한다.
     *
     * ★ 기존 구현의 치명적 결함:
     *   "처음 30초만 샘플링 + 그 구간의 평균 RMS" 로 판정했다.
     *   10분짜리 청크에서 처음 30초가 조용하고 2분째부터 대화가 시작되면
     *   청크 전체가 무음으로 찍혀 Whisper 에 보내지지도 않고 영구 캐싱됐다.
     *   → "어떤 때는 되고 어떤 때는 안 되는" 전사 누락의 직접 원인.
     *     (대화가 청크 앞 30초에 걸렸는지 아닌지에 따라 갈림)
     *
     * 수정: 파일 **전체**를 250ms 버킷으로 분석하고 **최댓값(peak)** 으로 판단한다.
     *   파일 어느 지점에서든 말소리 수준의 진폭이 한 번이라도 있으면 무음이 아니다.
     *   평균이 아니라 최댓값을 쓰는 이유는, 10분 중 1분만 대화여도 평균은
     *   묻혀버리기 때문이다.
     *
     * 판단 불가(디코딩 실패 등)면 false — 애매하면 Whisper 에 보내는 쪽이 안전하다.
     */
    private fun isSilent(file: File): Boolean {
        return try {
            val analysis = AudioWaveformLoader.analyzeForStt(file.absolutePath)
                ?: return false
            val amps = analysis.amplitudes
            if (amps.isEmpty()) return false

            val peak = amps.maxOrNull() ?: 0f
            val silent = peak < SILENCE_THRESHOLD
            if (silent) {
                android.util.Log.d("WhisperRepository",
                    "무음 판정: peak=$peak < $SILENCE_THRESHOLD (${file.name})")
            }
            silent
        } catch (e: Exception) {
            false  // 판단 불가 시 API 호출 허용
        }
    }

    /** Whisper API 응답(JSON) → WhisperSegment 목록 — org.json 직접 파싱, Gson 불사용 */
    private fun parseWhisperResponse(json: String): List<WhisperSegment> {
        return try {
            val root = org.json.JSONObject(json)
            val arr = root.optJSONArray("segments") ?: return emptyList()
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WhisperSegment(
                    id               = obj.optInt("id", i),
                    start            = obj.optDouble("start", 0.0),
                    end              = obj.optDouble("end", 0.0),
                    text             = obj.optString("text", ""),
                    noSpeechProb     = obj.optDouble("no_speech_prob", 0.0),
                    avgLogprob       = obj.optDouble("avg_logprob", 0.0),
                    compressionRatio = obj.optDouble("compression_ratio", 1.0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** org.json으로 직렬화 — ProGuard 난독화에 안전 */
    private fun serializeSegmentsJson(segments: List<TranscriptSegment>): String {
        val arr = org.json.JSONArray()
        segments.forEach { seg ->
            arr.put(org.json.JSONObject().apply {
                put("id", seg.id)
                put("absoluteStartMs", seg.absoluteStartMs)
                put("absoluteEndMs", seg.absoluteEndMs)
                put("text", seg.text)
            })
        }
        return arr.toString()
    }

    /** org.json으로 파싱 — ProGuard 난독화에 안전 */
    private fun parseSegmentsJson(json: String): List<TranscriptSegment> {
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TranscriptSegment(
                    id             = obj.optInt("id", i),
                    absoluteStartMs = obj.getLong("absoluteStartMs"),
                    absoluteEndMs   = obj.getLong("absoluteEndMs"),
                    text           = obj.getString("text")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
