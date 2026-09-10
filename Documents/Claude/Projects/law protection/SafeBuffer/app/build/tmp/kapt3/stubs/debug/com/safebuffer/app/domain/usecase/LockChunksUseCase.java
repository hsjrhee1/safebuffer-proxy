package com.safebuffer.app.domain.usecase;

/**
 * 유저가 "지금부터 N시간 전 구간 잠금" 버튼을 눌렀을 때 실행
 *
 * @param hoursBack 몇 시간 전부터 잠글지 (예: 2.5 = 2시간 30분 전 ~ 지금)
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0086B\u00a2\u0006\u0002\u0010\tR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/safebuffer/app/domain/usecase/LockChunksUseCase;", "", "repository", "Lcom/safebuffer/app/data/repository/AudioChunkRepository;", "(Lcom/safebuffer/app/data/repository/AudioChunkRepository;)V", "invoke", "Lcom/safebuffer/app/domain/usecase/LockResult;", "hoursBack", "", "(FLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class LockChunksUseCase {
    @org.jetbrains.annotations.NotNull()
    private final com.safebuffer.app.data.repository.AudioChunkRepository repository = null;
    
    @javax.inject.Inject()
    public LockChunksUseCase(@org.jetbrains.annotations.NotNull()
    com.safebuffer.app.data.repository.AudioChunkRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object invoke(float hoursBack, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.safebuffer.app.domain.usecase.LockResult> $completion) {
        return null;
    }
}