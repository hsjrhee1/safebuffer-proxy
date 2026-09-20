// 루트 build.gradle.kts — 앱 모듈(:app)이 사용하는 플러그인 버전을 여기서 선언한다.
// apply false = 루트에는 적용하지 않고 버전만 등록. 실제 적용은 app/build.gradle.kts에서.
plugins {
    id("com.android.application")        version "8.3.2" apply false
    id("org.jetbrains.kotlin.android")   version "1.9.22" apply false
    id("org.jetbrains.kotlin.kapt")      version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50"   apply false
}
