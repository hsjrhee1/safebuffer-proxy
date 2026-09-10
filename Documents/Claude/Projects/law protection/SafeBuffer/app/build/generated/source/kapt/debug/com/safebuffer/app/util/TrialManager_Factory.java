package com.safebuffer.app.util;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class TrialManager_Factory implements Factory<TrialManager> {
  private final Provider<Context> contextProvider;

  public TrialManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TrialManager get() {
    return newInstance(contextProvider.get());
  }

  public static TrialManager_Factory create(Provider<Context> contextProvider) {
    return new TrialManager_Factory(contextProvider);
  }

  public static TrialManager newInstance(Context context) {
    return new TrialManager(context);
  }
}
