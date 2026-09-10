package com.safebuffer.app.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safebuffer.app.data.repository.AudioChunkRepository
import com.safebuffer.app.databinding.ActivitySettingsBinding
import com.safebuffer.app.util.TrialManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject lateinit var trialManager: TrialManager
    @Inject lateinit var chunkRepository: AudioChunkRepository

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupTrialStatus()
        setupVersionInfo()
        setupBatteryOptimization()
        setupResetDb()

        binding.btnUpgrade.setOnClickListener {
            // TODO: Play Store 인앱 결제 연동 시 여기에 구현
            Toast.makeText(this, "준비 중입니다. 잠시 기다려 주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // 설정에서 돌아왔을 때 배터리 상태 갱신
        updateBatteryStatus()
    }

    private fun setupTrialStatus() {
        val days = trialManager.daysRemaining()
        if (days > 0) {
            binding.tvTrialDays.text = "D-$days"
            binding.tvTrialStatus.text = "남음"
            binding.tvTrialDays.setTextColor(0xFF_FF_FF_FF.toInt())
        } else {
            binding.tvTrialDays.text = "만료"
            binding.tvTrialStatus.text = "정식 버전으로 업그레이드하세요"
            binding.tvTrialDays.setTextColor(0xFF_FF_44_44.toInt())
        }
    }

    private fun setupVersionInfo() {
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            binding.tvVersion.text = versionName
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvVersion.text = "1.0.0"
        }
    }

    private fun setupBatteryOptimization() {
        updateBatteryStatus()

        binding.btnBatteryOptimization.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = getSystemService(PowerManager::class.java)
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    // 이미 제외됨 — 시스템 배터리 설정으로 이동
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } else {
                    // 이 앱 바로 제외 요청
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun setupResetDb() {
        binding.btnResetDb.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ 전체 초기화")
                .setMessage("모든 녹음 파일과 잠긴 증거가 영구 삭제됩니다.\n복구할 수 없습니다. 계속할까요?")
                .setPositiveButton("초기화") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { chunkRepository.resetAll() }
                        Toast.makeText(this@SettingsActivity, "초기화 완료. 앱을 재시작하세요.", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun updateBatteryStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            val excluded = pm.isIgnoringBatteryOptimizations(packageName)
            binding.tvBatteryStatus.text = if (excluded) "✅ 제외됨" else "⚠️ 제외 안됨"
            binding.tvBatteryStatus.setTextColor(
                if (excluded) 0xFF_44_BB_44.toInt() else 0xFF_FF_99_22.toInt()
            )
            binding.btnBatteryOptimization.text =
                if (excluded) "배터리 설정 보기" else "배터리 최적화 제외 설정하기 →"
        }
    }
}
