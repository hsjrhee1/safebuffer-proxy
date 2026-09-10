package com.safebuffer.app.data.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000r\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u001a\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001a2\b\u0010\u001b\u001a\u0004\u0018\u00010\u001cH\u0002J\u0010\u0010\u001d\u001a\u00020\u00182\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\u0010\u0010 \u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J2\u0010!\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020$0#0\"2\u0006\u0010%\u001a\u00020&2\u0006\u0010\'\u001a\u00020&H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b(\u0010)J,\u0010*\u001a\b\u0012\u0004\u0012\u00020$0#2\u0006\u0010\u001e\u001a\u00020\u001f2\u0006\u0010+\u001a\u00020&2\u0006\u0010,\u001a\u00020-H\u0082@\u00a2\u0006\u0002\u0010.R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u000bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0010X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0010X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0010X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0010X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006/"}, d2 = {"Lcom/safebuffer/app/data/repository/WhisperRepository;", "", "api", "Lcom/safebuffer/app/data/remote/WhisperApiService;", "chunkDao", "Lcom/safebuffer/app/data/local/ChunkDao;", "(Lcom/safebuffer/app/data/remote/WhisperApiService;Lcom/safebuffer/app/data/local/ChunkDao;)V", "HALLUCINATION_PHRASES", "", "", "MAX_COMPRESSION_RATIO", "", "MAX_NO_SPEECH_PROB", "MIN_AVG_LOGPROB", "MIN_SPEECH_DURATION_SEC", "SEG_ACTIVE_RATIO", "", "SEG_ACTIVE_THRESH", "SEG_AVG_SILENCE", "SEG_PEAK_SILENCE", "SILENCE_THRESHOLD", "gson", "Lcom/google/gson/Gson;", "isSegmentSilent", "", "seg", "Lcom/safebuffer/app/data/remote/WhisperSegment;", "analysis", "Lcom/safebuffer/app/util/AudioAnalysisResult;", "isSilent", "file", "Ljava/io/File;", "isValidSegment", "transcribeRange", "Lkotlin/Result;", "", "Lcom/safebuffer/app/data/model/TranscriptSegment;", "fromMs", "", "toMs", "transcribeRange-0E7RQCE", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "transcribeSingleChunk", "chunkStartMs", "idOffset", "", "(Ljava/io/File;JILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class WhisperRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.safebuffer.app.data.remote.WhisperApiService api = null;
    @org.jetbrains.annotations.NotNull()
    private final com.safebuffer.app.data.local.ChunkDao chunkDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    private final float SILENCE_THRESHOLD = 0.008F;
    private final double MAX_NO_SPEECH_PROB = 0.6;
    private final double MIN_AVG_LOGPROB = -1.0;
    private final double MAX_COMPRESSION_RATIO = 2.4;
    private final double MIN_SPEECH_DURATION_SEC = 0.3;
    private final float SEG_AVG_SILENCE = 0.002F;
    private final float SEG_PEAK_SILENCE = 0.006F;
    private final float SEG_ACTIVE_THRESH = 0.004F;
    private final float SEG_ACTIVE_RATIO = 0.05F;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> HALLUCINATION_PHRASES = null;
    
    @javax.inject.Inject()
    public WhisperRepository(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.remote.WhisperApiService api, @org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.local.ChunkDao chunkDao) {
        super();
    }
    
    private final java.lang.Object transcribeSingleChunk(java.io.File file, long chunkStartMs, int idOffset, kotlin.coroutines.Continuation<? super java.util.List<com.safebuffer.app.data.model.TranscriptSegment>> $completion) {
        return null;
    }
    
    /**
     * Amplitude 기반 세그먼트 silence 판정.
     *
     * 보수적 AND 조건: 세 지표 모두 임계값 이하일 때만 "명백한 무음"으로 제거.
     * 하나라도 넘으면 살림 → false negative 최우선.
     *
     * @return true = 무음으로 판단 → 제거 / false = 유지
     */
    private final boolean isSegmentSilent(com.safebuffer.app.data.remote.WhisperSegment seg, com.safebuffer.app.util.AudioAnalysisResult analysis) {
        return false;
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
    private final boolean isValidSegment(com.safebuffer.app.data.remote.WhisperSegment seg) {
        return false;
    }
    
    /**
     * 오디오 파일의 평균 RMS를 계산해 침묵 여부 판단.
     * 처음 30초만 샘플링해서 속도 확보.
     */
    private final boolean isSilent(java.io.File file) {
        return false;
    }
}