package com.safebuffer.app.data.repository

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class WhisperRepository @Inject constructor(
    private val api: WhisperApiService,
    private val chunkDao: ChunkDao
) {
    private val gson = Gson()

    // ── 사전 필터 (Whisper 호출 전) ──────────────────────────────
    // RMS 이하면 침묵으로 판단, API 호출 건너뜀
    private val SILENCE_THRESHOLD = 0.008f

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
                return Result.failure(Exception("해당 구간에 녹음된 파일이 없습니다."))
            }

            val allSegments = mutableListOf<TranscriptSegment>()
            var segmentIdOffset = 0

            for (chunk in chunks) {
                // 캐시 확인 — 이미 전사된 청크는 API 호출 없이 바로 사용
                val cached = chunk.transcription
                val segments = if (!cached.isNullOrBlank()) {
                    val type = object : TypeToken<List<TranscriptSegment>>() {}.type
                    val list: List<TranscriptSegment> = gson.fromJson(cached, type)
                    list.mapIndexed { i, seg -> seg.copy(id = segmentIdOffset + i) }
                } else {
                    val file = File(chunk.filePath)
                    if (!file.exists()) continue

                    // ★ 침묵 구간은 Whisper 호출 건너뜀 — hallucination 방지
                    // IO 스레드에서 실행 (MediaCodec 디코딩 → Main thread 블로킹 방지)
                    if (withContext(Dispatchers.IO) { isSilent(file) }) {
                        chunkDao.updateTranscription(chunk.id, gson.toJson(emptyList<TranscriptSegment>()))
                        continue
                    }

                    val result = transcribeSingleChunk(file, chunk.startTimeMs, segmentIdOffset)
                    chunkDao.updateTranscription(chunk.id, gson.toJson(result))
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

        // Whisper API 호출 (시간 소요)
        val response = api.transcribe(file = filePart)

        // Whisper 응답 후 amplitude 분석 — 세그먼트별 silence 판정용
        // IO 스레드에서 실행, null 반환 시 amplitude 필터 건너뜀 → false negative 방지
        val analysis: AudioAnalysisResult? = withContext(Dispatchers.IO) {
            AudioWaveformLoader.analyzeForStt(file.absolutePath)
        }

        return response.segments
            ?.filter { seg -> isValidSegment(seg) }
            ?.filter { seg -> !isSegmentSilent(seg, analysis) }
            ?.mapIndexed { i, seg ->
                TranscriptSegment(
                    id = idOffset + i,
                    absoluteStartMs = chunkStartMs + (seg.start * 1000).toLong(),
                    absoluteEndMs = chunkStartMs + (seg.end * 1000).toLong(),
                    text = seg.text.trim()
                )
            } ?: emptyList()
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
     * 오디오 파일의 평균 RMS를 계산해 침묵 여부 판단.
     * 처음 30초만 샘플링해서 속도 확보.
     */
    private fun isSilent(file: File): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)

            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    trackIndex = i; format = f; break
                }
            }
            if (trackIndex < 0 || format == null) return false

            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return false
            // 처음 30초만 체크
            val sampleLimitUs = 30_000_000L

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var sumSq = 0.0
            var totalSamples = 0L
            var loopCount = 0

            try {
                while (!outputDone && loopCount++ < 20_000) {
                    if (!inputDone) {
                        val inIdx = codec.dequeueInputBuffer(5_000)
                        if (inIdx >= 0) {
                            val buf = codec.getInputBuffer(inIdx)!!
                            val size = extractor.readSampleData(buf, 0)
                            if (size < 0 || extractor.sampleTime > sampleLimitUs) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIdx = codec.dequeueOutputBuffer(info, 5_000)
                    if (outIdx >= 0) {
                        val buf = codec.getOutputBuffer(outIdx)!!
                        buf.order(ByteOrder.LITTLE_ENDIAN)
                        val shorts = buf.asShortBuffer()
                        while (shorts.hasRemaining()) {
                            val s = shorts.get().toFloat() / 32768f
                            sumSq += s * s
                            totalSamples++
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                            outputDone = true
                    }
                }
            } finally {
                codec.stop()
                codec.release()
            }

            if (totalSamples == 0L) return true
            val rms = sqrt(sumSq / totalSamples).toFloat()
            rms < SILENCE_THRESHOLD

        } catch (e: Exception) {
            false  // 판단 불가 시 API 호출 허용
        } finally {
            extractor.release()
        }
    }
}
