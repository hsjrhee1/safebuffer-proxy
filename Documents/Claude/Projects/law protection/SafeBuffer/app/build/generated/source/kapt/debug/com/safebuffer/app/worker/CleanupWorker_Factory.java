package com.safebuffer.app.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.safebuffer.app.data.repository.AudioChunkRepository;
import dagger.internal.DaggerGenerated;
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
public final class CleanupWorker_Factory {
  private final Provider<AudioChunkRepository> repositoryProvider;

  public CleanupWorker_Factory(Provider<AudioChunkRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public CleanupWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, repositoryProvider.get());
  }

  public static CleanupWorker_Factory create(Provider<AudioChunkRepository> repositoryProvider) {
    return new CleanupWorker_Factory(repositoryProvider);
  }

  public static CleanupWorker newInstance(Context context, WorkerParameters workerParams,
      AudioChunkRepository repository) {
    return new CleanupWorker(context, workerParams, repository);
  }
}
