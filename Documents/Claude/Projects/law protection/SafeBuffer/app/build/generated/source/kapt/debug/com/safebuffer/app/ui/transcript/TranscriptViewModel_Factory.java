package com.safebuffer.app.ui.transcript;

import com.safebuffer.app.data.repository.AudioChunkRepository;
import com.safebuffer.app.data.repository.WhisperRepository;
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
public final class TranscriptViewModel_Factory implements Factory<TranscriptViewModel> {
  private final Provider<WhisperRepository> whisperRepositoryProvider;

  private final Provider<AudioChunkRepository> chunkRepositoryProvider;

  public TranscriptViewModel_Factory(Provider<WhisperRepository> whisperRepositoryProvider,
      Provider<AudioChunkRepository> chunkRepositoryProvider) {
    this.whisperRepositoryProvider = whisperRepositoryProvider;
    this.chunkRepositoryProvider = chunkRepositoryProvider;
  }

  @Override
  public TranscriptViewModel get() {
    return newInstance(whisperRepositoryProvider.get(), chunkRepositoryProvider.get());
  }

  public static TranscriptViewModel_Factory create(
      Provider<WhisperRepository> whisperRepositoryProvider,
      Provider<AudioChunkRepository> chunkRepositoryProvider) {
    return new TranscriptViewModel_Factory(whisperRepositoryProvider, chunkRepositoryProvider);
  }

  public static TranscriptViewModel newInstance(WhisperRepository whisperRepository,
      AudioChunkRepository chunkRepository) {
    return new TranscriptViewModel(whisperRepository, chunkRepository);
  }
}
