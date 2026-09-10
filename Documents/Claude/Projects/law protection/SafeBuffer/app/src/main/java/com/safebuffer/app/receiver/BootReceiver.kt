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
            ContextCompat.startForegroundService(
                context,
                RecordingService.startIntent(context)
            )
        }
    }
}
