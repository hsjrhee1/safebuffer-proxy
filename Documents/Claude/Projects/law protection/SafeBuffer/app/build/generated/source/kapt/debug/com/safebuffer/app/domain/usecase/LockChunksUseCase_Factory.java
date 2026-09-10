package com.safebuffer.app.domain.usecase;

import com.safebuffer.app.data.repository.AudioChunkRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class LockChunksUseCase_Factory implements Factory<LockChunksUseCase> {
  private final Provider<AudioChunkRepository> repositoryProvider;

  public LockChunksUseCase_Factory(Provider<AudioChunkRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public LockChunksUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static LockChunksUseCase_Factory create(
      Provider<AudioChunkRepository> repositoryProvider) {
    return new LockChunksUseCase_Factory(repositoryProvider);
  }

  public static LockChunksUseCase newInstance(AudioChunkRepository repository) {
    return new LockChunksUseCase(repository);
  }
}
