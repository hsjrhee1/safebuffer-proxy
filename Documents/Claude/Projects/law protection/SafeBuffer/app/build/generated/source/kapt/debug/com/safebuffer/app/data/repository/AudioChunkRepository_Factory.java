package com.safebuffer.app.data.repository;

import com.safebuffer.app.data.local.ChunkDao;
import com.safebuffer.app.data.remote.WhisperApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AudioChunkRepository_Factory implements Factory<AudioChunkRepository> {
  private final Provider<ChunkDao> daoProvider;

  private final Provider<WhisperApiService> apiProvider;

  public AudioChunkRepository_Factory(Provider<ChunkDao> daoProvider,
      Provider<WhisperApiService> apiProvider) {
    this.daoProvider = daoProvider;
    this.apiProvider = apiProvider;
  }

  @Override
  public AudioChunkRepository get() {
    return newInstance(daoProvider.get(), apiProvider.get());
  }

  public static AudioChunkRepository_Factory create(Provider<ChunkDao> daoProvider,
      Provider<WhisperApiService> apiProvider) {
    return new AudioChunkRepository_Factory(daoProvider, apiProvider);
  }

  public static AudioChunkRepository newInstance(ChunkDao dao, WhisperApiService api) {
    return new AudioChunkRepository(dao, api);
  }
}
