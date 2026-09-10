package com.safebuffer.app.service;

import com.safebuffer.app.data.repository.AudioChunkRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class RecordingService_MembersInjector implements MembersInjector<RecordingService> {
  private final Provider<AudioChunkRepository> repositoryProvider;

  public RecordingService_MembersInjector(Provider<AudioChunkRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public static MembersInjector<RecordingService> create(
      Provider<AudioChunkRepository> repositoryProvider) {
    return new RecordingService_MembersInjector(repositoryProvider);
  }

  @Override
  public void injectMembers(RecordingService instance) {
    injectRepository(instance, repositoryProvider.get());
  }

  @InjectedFieldSignature("com.safebuffer.app.service.RecordingService.repository")
  public static void injectRepository(RecordingService instance, AudioChunkRepository repository) {
    instance.repository = repository;
  }
}
