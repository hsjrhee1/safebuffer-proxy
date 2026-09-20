# SafeBuffer ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class *

# Retrofit + OkHttp — R8 제네릭 시그니처 보호
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# WhisperApiService 인터페이스 — Call<ResponseBody> 반환 타입 시그니처 보호
# suspend fun을 Call<ResponseBody>로 교체했으므로 Continuation 제네릭 소거 문제 없음
-keep interface com.safebuffer.app.data.remote.WhisperApiService { *; }
-keepclassmembers interface com.safebuffer.app.data.remote.WhisperApiService {
    <methods>;
}

# JSON 모델 (org.json 수동 파싱)
-keep class com.safebuffer.app.data.remote.** { *; }
-keepclassmembers class com.safebuffer.app.data.remote.** { *; }
-keep class com.safebuffer.app.data.model.** { *; }
-keepclassmembers class com.safebuffer.app.data.model.** { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ViewBinding
-keep class com.safebuffer.app.databinding.** { *; }

# 일반 Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
