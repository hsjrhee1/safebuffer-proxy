package com.safebuffer.app.di

import com.safebuffer.app.data.remote.WhisperApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            // ★ 릴리스 빌드에서는 네트워크 로깅을 끈다.
            //   증거 녹음 앱이므로 요청 URL·크기 등이 logcat 에 남지 않아야 하고,
            //   다른 앱이 읽을 수 있는 로그에 흔적을 남길 이유가 없다.
            .apply {
                if (com.safebuffer.app.BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideWhisperRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://safebuffer-proxy-1.onrender.com/")  // 프록시 서버
            .client(client)
            // GsonConverterFactory 제거 — 모든 엔드포인트가 ResponseBody/RequestBody 직접 사용
            // Gson 제네릭 TypeToken + R8 minification 충돌 완전 방지
            .build()

    @Provides
    @Singleton
    fun provideWhisperApiService(retrofit: Retrofit): WhisperApiService =
        retrofit.create(WhisperApiService::class.java)
}
