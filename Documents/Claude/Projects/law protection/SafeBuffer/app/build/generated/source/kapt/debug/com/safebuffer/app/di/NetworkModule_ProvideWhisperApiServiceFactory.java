package com.safebuffer.app.di;

import com.safebuffer.app.data.remote.WhisperApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideWhisperApiServiceFactory implements Factory<WhisperApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideWhisperApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public WhisperApiService get() {
    return provideWhisperApiService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideWhisperApiServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideWhisperApiServiceFactory(retrofitProvider);
  }

  public static WhisperApiService provideWhisperApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWhisperApiService(retrofit));
  }
}
