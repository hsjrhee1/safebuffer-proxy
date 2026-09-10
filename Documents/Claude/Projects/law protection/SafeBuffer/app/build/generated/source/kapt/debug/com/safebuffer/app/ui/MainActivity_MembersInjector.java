package com.safebuffer.app.ui;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<TrialManager> trialManagerProvider;

  public MainActivity_MembersInjector(Provider<TrialManager> trialManagerProvider) {
    this.trialManagerProvider = trialManagerProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<TrialManager> trialManagerProvider) {
    return new MainActivity_MembersInjector(trialManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectTrialManager(instance, trialManagerProvider.get());
  }

  @InjectedFieldSignature("com.safebuffer.app.ui.MainActivity.trialManager")
  public static void injectTrialManager(MainActivity instance, TrialManager trialManager) {
    instance.trialManager = trialManager;
  }
}
