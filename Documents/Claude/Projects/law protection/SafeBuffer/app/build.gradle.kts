import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

// ★ 서명 비밀번호는 소스에 넣지 않는다.
//   우선순위: keystore.properties (gitignore 됨) → 환경변수.
//   두 곳 모두 없으면 release 서명 정보를 비워 둔다(그 상태로 릴리스 빌드하면 서명 단계에서
//   명확히 실패하므로, 평문 비밀번호가 저장소에 남는 것보다 안전하다).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
fun secret(propKey: String, envKey: String): String? =
    keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

android {
    namespace = "com.safebuffer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.safebuffer.app"
        minSdk = 26
        targetSdk = 36
        multiDexEnabled = true
        versionCode = 110
        versionName = "1.0.1"
    }

    signingConfigs {
        create("release") {
            // 비밀번호는 keystore.properties 또는 환경변수에서만 읽는다 (평문 금지).
            val storePw = secret("storePassword", "KEYSTORE_PASS")
            val keyPw   = secret("keyPassword", "KEY_PASS")
            if (storePw != null && keyPw != null) {
                storeFile = file(secret("storeFile", "KEYSTORE_FILE") ?: "safebuffer-keystore.jks")
                storePassword = storePw
                keyAlias = secret("keyAlias", "KEY_ALIAS") ?: "safebuffer"
                keyPassword = keyPw
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true   // BuildConfig.DEBUG 사용 (릴리스에서 네트워크 로깅 차단)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit + OkHttp (Whisper API) — converter-gson 제거 (org.json 직접 파싱으로 대체)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Google Play Billing
    implementation("com.android.billingclient:billing:8.0.0")
}
