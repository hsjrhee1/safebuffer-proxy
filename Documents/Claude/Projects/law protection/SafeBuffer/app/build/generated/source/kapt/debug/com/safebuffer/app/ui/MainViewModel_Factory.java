package com.safebuffer.app.ui;

import com.safebuffer.app.data.repository.AudioChunkRepository;
import com.safebuffer.app.domain.usecase.LockChunksUseCase;
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<AudioChunkRepository> repositoryProvider;

  private final Provider<LockChunksUseCase> lockChunksUseCaseProvider;

  public MainViewModel_Factory(Provider<AudioChunkRepository> repositoryProvider,
      Provider<LockChunksUseCase> lockChunksUseCaseProvider) {
    this.repositoryProvider = repositoryProvider;
    this.lockChunksUseCaseProvider = lockChunksUseCaseProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(repositoryProvider.get(), lockChunksUseCaseProvider.get());
  }

  public static MainViewModel_Factory create(Provider<AudioChunkRepository> repositoryProvider,
      Provider<LockChunksUseCase> lockChunksUseCaseProvider) {
    return new MainViewModel_Factory(repositoryProvider, lockChunksUseCaseProvider);
  }

  public static MainViewModel newInstance(AudioChunkRepository repository,
      LockChunksUseCase lockChunksUseCase) {
    return new MainViewModel(repository, lockChunksUseCase);
  }
}
