package com.safebuffer.app.data.remote;

/**
 * OpenAI Whisper API
 * POST https://api.openai.com/v1/audio/transcriptions
 *
 * 요청: multipart/form-data
 *  - file    : 오디오 파일 (.m4a)
 *  - model   : "whisper-1"
 *  - language: "ko"
 *  - response_format: "verbose_json" (타임스탬프 포함)
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u00020\b2\b\b\u0001\u0010\t\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010\u000b\u00a8\u0006\f"}, d2 = {"Lcom/safebuffer/app/data/remote/WhisperApiService;", "", "stamp", "Lcom/safebuffer/app/data/remote/StampResponse;", "request", "Lcom/safebuffer/app/data/remote/StampRequest;", "(Lcom/safebuffer/app/data/remote/StampRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "transcribe", "Lcom/safebuffer/app/data/remote/WhisperResponse;", "file", "Lokhttp3/MultipartBody$Part;", "(Lokhttp3/MultipartBody$Part;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface WhisperApiService {
    
    @retrofit2.http.Multipart()
    @retrofit2.http.POST(value = "api/transcribe")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object transcribe(@retrofit2.http.Part()
    @org.jetbrains.annotations.NotNull()
    okhttp3.MultipartBody.Part file, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.safebuffer.app.data.remote.WhisperResponse> $completion);
    
    @retrofit2.http.POST(value = "api/stamp")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object stamp(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.remote.StampRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.safebuffer.app.data.remote.StampResponse> $completion);
}