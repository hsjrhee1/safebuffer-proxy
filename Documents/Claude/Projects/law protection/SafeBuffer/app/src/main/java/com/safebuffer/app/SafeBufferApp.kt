package com.safebuffer.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * @HiltWorker 가 붙은 CleanupWorker 는 생성자에 AudioChunkRepository / TierManager 를
 * 주입받으므로 WorkManager 기본 WorkerFactory 로는 생성할 수 없다.
 * Configuration.Provider 를 구현해 HiltWorkerFactory 를 넘겨야 한다.
 *
 * 함께 AndroidManifest 의 기본 WorkManagerInitializer 를 제거해야
 * 이 설정으로 on-demand 초기화가 이루어진다.
 */
@HiltAndroidApp
class SafeBufferApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
