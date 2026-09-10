package com.safebuffer.app.ui.transcript;

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
public final class TranscriptActivity_MembersInjector implements MembersInjector<TranscriptActivity> {
  private final Provider<TrialManager> trialManagerProvider;

  public TranscriptActivity_MembersInjector(Provider<TrialManager> trialManagerProvider) {
    this.trialManagerProvider = trialManagerProvider;
  }

  public static MembersInjector<TranscriptActivity> create(
      Provider<TrialManager> trialManagerProvider) {
    return new TranscriptActivity_MembersInjector(trialManagerProvider);
  }

  @Override
  public void injectMembers(TranscriptActivity instance) {
    injectTrialManager(instance, trialManagerProvider.get());
  }

  @InjectedFieldSignature("com.safebuffer.app.ui.transcript.TranscriptActivity.trialManager")
  public static void injectTrialManager(TranscriptActivity instance, TrialManager trialManager) {
    instance.trialManager = trialManager;
  }
}
