package com.safebuffer.app.ui.settings;

import com.safebuffer.app.data.repository.AudioChunkRepository;
import com.safebuffer.app.util.TrialManager;
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
public final class SettingsActivity_MembersInjector implements MembersInjector<SettingsActivity> {
  private final Provider<TrialManager> trialManagerProvider;

  private final Provider<AudioChunkRepository> chunkRepositoryProvider;

  public SettingsActivity_MembersInjector(Provider<TrialManager> trialManagerProvider,
      Provider<AudioChunkRepository> chunkRepositoryProvider) {
    this.trialManagerProvider = trialManagerProvider;
    this.chunkRepositoryProvider = chunkRepositoryProvider;
  }

  public static MembersInjector<SettingsActivity> create(
      Provider<TrialManager> trialManagerProvider,
      Provider<AudioChunkRepository> chunkRepositoryProvider) {
    return new SettingsActivity_MembersInjector(trialManagerProvider, chunkRepositoryProvider);
  }

  @Override
  public void injectMembers(SettingsActivity instance) {
    injectTrialManager(instance, trialManagerProvider.get());
    injectChunkRepository(instance, chunkRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.safebuffer.app.ui.settings.SettingsActivity.trialManager")
  public static void injectTrialManager(SettingsActivity instance, TrialManager trialManager) {
    instance.trialManager = trialManager;
  }

  @InjectedFieldSignature("com.safebuffer.app.ui.settings.SettingsActivity.chunkRepository")
  public static void injectChunkRepository(SettingsActivity instance,
      AudioChunkRepository chunkRepository) {
    instance.chunkRepository = chunkRepository;
  }
}
