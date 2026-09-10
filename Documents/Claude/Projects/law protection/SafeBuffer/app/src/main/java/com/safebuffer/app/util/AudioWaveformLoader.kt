package com.safebuffer.app.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * STT 세그먼트별 amplitude 판정용 분석 결과.
 *
 * @param durationMs       파일 전체 길이 (ms)
 * @param amplitudes       비정규화(raw) RMS 값 리스트. 인덱스 하나 = [bucketDurationMs] ms 구간
 * @param bucketDurationMs 버킷 하나의 시간 길이 (ms). 기본 250ms
 */
data class AudioAnalysisResult(
    val durationMs: Long,
    val amplitudes: List<Float>,
    val bucketDurationMs: Long
)

object AudioWaveformLoader {

    /**
     * STT 세그먼트 amplitude 분석.
     * UI 파형(load)과 달리 정규화하지 않은 raw RMS를 반환한다.
     * 해상도: bucketDurationMs ms 단위 (기본 250ms → 4 buckets/sec).
     *   10분 파일 → 2,400 buckets (~9.6 KB)
     *   30분 파일 → 7,200 buckets (~28.8 KB)
     * 오류 또는 판단 불가 시 null 반환 → 호출 측에서 필터 건너뜀.
     * IO 스레드에서 실행해야 함.
     */
    fun analyzeForStt(
        filePath: String,
        bucketDurationMs: Long = 250L
    ): AudioAnalysisResult? {
        if (!File(filePath).exists()) return null

        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(filePath)

            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    trackIndex = i; format = f; break
                }
            }
            if (trackIndex < 0 || format == null) return null

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
                format.getLong(MediaFormat.KEY_DURATION) else 0L
            if (durationUs <= 0L) return null

            val durationMs = durationUs / 1000L
            val buckets = ((durationMs + bucketDurationMs - 1) / bucketDurationMs)
                .toInt().coerceIn(1, 12_000)  // 최대 50분 @ 250ms

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val rmsSum   = FloatArray(buckets)
            val rmsCount = IntArray(buckets)
            val info     = MediaCodec.BufferInfo()
            var inputDone  = false
            var outputDone = false
            var loopCount  = 0

            try {
                while (!outputDone && loopCount++ < 100_000) {
                    if (!inputDone) {
                        val inIdx = codec.dequeueInputBuffer(5_000)
                        if (inIdx >= 0) {
                            val buf  = codec.getInputBuffer(inIdx)!!
                            val size = extractor.readSampleData(buf, 0)
                            if (size < 0) {
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
                        val bucketIdx =
                            ((info.presentationTimeUs.toDouble() / durationUs) * buckets)
                                .toInt().coerceIn(0, buckets - 1)

                        buf.order(ByteOrder.LITTLE_ENDIAN)
                        val shorts = buf.asShortBuffer()
                        var sumSq = 0.0
                        var count = 0
                        while (shorts.hasRemaining()) {
                            val s = shorts.get().toFloat() / 32768f
                            sumSq += s * s
                            count++
                        }
                        if (count > 0) {
                            rmsSum[bucketIdx]   += sqrt(sumSq / count).toFloat()
                            rmsCount[bucketIdx] += 1
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

            // 버킷 평균 — 정규화 안 함 (raw RMS 유지)
            val amps = FloatArray(buckets) { i ->
                if (rmsCount[i] > 0) rmsSum[i] / rmsCount[i] else 0f
            }

            AudioAnalysisResult(
                durationMs      = durationMs,
                amplitudes      = amps.toList(),
                bucketDurationMs = bucketDurationMs
            )
        } catch (e: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    /**
     * 오디오 파일을 디코딩해 진폭 배열 반환 (0.0–1.0, 정규화됨).
     * UI 파형 표시 전용. STT 판정에는 analyzeForStt()를 사용할 것.
     * IO 스레드에서 실행해야 함.
     */
    fun load(filePath: String, buckets: Int = 150): List<Float> {
        if (!File(filePath).exists()) return emptyList()

        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(filePath)

            // 오디오 트랙 찾기
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return emptyList()

            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
                format.getLong(MediaFormat.KEY_DURATION) else 0L
            if (durationUs <= 0L) return emptyList()

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val rmsSum   = FloatArray(buckets)
            val rmsCount = IntArray(buckets)
            val info     = MediaCodec.BufferInfo()
            var inputDone  = false
            var outputDone = false
            var loopCount  = 0
            val maxLoops   = 50_000   // 무한루프 방지

            try {
                while (!outputDone && loopCount++ < maxLoops) {
                    // 입력 공급
                    if (!inputDone) {
                        val inIdx = codec.dequeueInputBuffer(5_000)
                        if (inIdx >= 0) {
                            val buf  = codec.getInputBuffer(inIdx)!!
                            val size = extractor.readSampleData(buf, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    // 출력 읽기
                    val outIdx = codec.dequeueOutputBuffer(info, 5_000)
                    if (outIdx >= 0) {
                        val buf = codec.getOutputBuffer(outIdx)!!
                        val bucketIdx = ((info.presentationTimeUs.toDouble() / durationUs) * buckets)
                            .toInt().coerceIn(0, buckets - 1)

                        buf.order(ByteOrder.LITTLE_ENDIAN)
                        val shorts = buf.asShortBuffer()
                        var sumSq = 0.0
                        var count = 0
                        while (shorts.hasRemaining()) {
                            val s = shorts.get().toFloat() / 32768f
                            sumSq += s * s
                            count++
                        }
                        if (count > 0) {
                            rmsSum[bucketIdx]   += sqrt(sumSq / count).toFloat()
                            rmsCount[bucketIdx] += 1
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

            // 버킷 평균
            val amps = FloatArray(buckets) { i ->
                if (rmsCount[i] > 0) rmsSum[i] / rmsCount[i] else 0f
            }

            // 0..1 정규화 (maxOrNull — Kotlin 버전 무관하게 안전)
            val maxAmp = amps.maxOrNull() ?: 0f
            if (maxAmp <= 0f) return amps.toList()
            amps.map { it / maxAmp }

        } catch (e: Exception) {
            emptyList()
        } finally {
            extractor.release()
        }
    }
}
