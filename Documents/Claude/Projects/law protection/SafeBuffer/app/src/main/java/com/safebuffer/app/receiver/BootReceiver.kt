package com.safebuffer.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.safebuffer.app.service.RecordingService

/**
 * 기기 재부팅 후 SafeBuffer 자동 재시작
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // ★ try-catch 필수: 부팅 직후 포그라운드 서비스 시작 실패 시 앱 크래시 방지
            try {
                ContextCompat.startForegroundService(
                    context,
                    RecordingService.startIntent(context)
                )
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "부팅 후 서비스 시작 실패: ${e.message}")
            }
        }
    }
}
