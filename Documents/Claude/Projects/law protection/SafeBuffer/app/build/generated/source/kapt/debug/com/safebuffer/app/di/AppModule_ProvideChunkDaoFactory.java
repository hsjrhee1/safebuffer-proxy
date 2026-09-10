package com.safebuffer.app.di;

import com.safebuffer.app.data.local.ChunkDao;
import com.safebuffer.app.data.local.ChunkDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideChunkDaoFactory implements Factory<ChunkDao> {
  private final Provider<ChunkDatabase> dbProvider;

  public AppModule_ProvideChunkDaoFactory(Provider<ChunkDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ChunkDao get() {
    return provideChunkDao(dbProvider.get());
  }

  public static AppModule_ProvideChunkDaoFactory create(Provider<ChunkDatabase> dbProvider) {
    return new AppModule_ProvideChunkDaoFactory(dbProvider);
  }

  public static ChunkDao provideChunkDao(ChunkDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChunkDao(db));
  }
}
