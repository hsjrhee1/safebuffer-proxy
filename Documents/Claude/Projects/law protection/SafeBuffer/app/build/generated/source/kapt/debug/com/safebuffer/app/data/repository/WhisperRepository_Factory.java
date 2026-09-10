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
public final class WhisperRepository_Factory implements Factory<WhisperRepository> {
  private final Provider<WhisperApiService> apiProvider;

  private final Provider<ChunkDao> chunkDaoProvider;

  public WhisperRepository_Factory(Provider<WhisperApiService> apiProvider,
      Provider<ChunkDao> chunkDaoProvider) {
    this.apiProvider = apiProvider;
    this.chunkDaoProvider = chunkDaoProvider;
  }

  @Override
  public WhisperRepository get() {
    return newInstance(apiProvider.get(), chunkDaoProvider.get());
  }

  public static WhisperRepository_Factory create(Provider<WhisperApiService> apiProvider,
      Provider<ChunkDao> chunkDaoProvider) {
    return new WhisperRepository_Factory(apiProvider, chunkDaoProvider);
  }

  public static WhisperRepository newInstance(WhisperApiService api, ChunkDao chunkDao) {
    return new WhisperRepository(api, chunkDao);
  }
}
