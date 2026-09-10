package com.safebuffer.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Groq API 키를 SharedPreferences에 저장/조회
 * 발급: https://console.groq.com/keys
 * (실제 배포 시 EncryptedSharedPreferences 로 교체 권장)
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("safebuffer_prefs", Context.MODE_PRIVATE)
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    companion object {
        private const val KEY_API_KEY = "openai_api_key"
    }
}
