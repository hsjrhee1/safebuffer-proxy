package com.safebuffer.app.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safebuffer.app.data.repository.AudioChunkRepository
import com.safebuffer.app.databinding.ActivitySettingsBinding
import com.safebuffer.app.util.BackupManager
import com.safebuffer.app.util.BillingManager
import com.safebuffer.app.util.SttQuotaManager
import com.safebuffer.app.util.TierManager
import com.safebuffer.app.util.TrialManager
import com.safebuffer.app.service.RecordingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject lateinit var trialManager: TrialManager
    @Inject lateinit var chunkRepository: AudioChunkRepository
    @Inject lateinit var billingManager: BillingManager
    @Inject lateinit var tierManager: TierManager
    @Inject lateinit var sttQuotaManager: SttQuotaManager

    private lateinit var binding: ActivitySettingsBinding

    // ZIP 파일 선택기 (복원용)
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            Toast.makeText(this@SettingsActivity, "복원 중...", Toast.LENGTH_SHORT).show()
            val result = withContext(Dispatchers.IO) {
                BackupManager.importBackup(this@SettingsActivity, uri)
            }
            result.fold(
                onSuccess = { importResult ->
                    withContext(Dispatchers.IO) {
                        importResult.chunks.forEach { chunk ->
                            chunkRepository.saveChunk(chunk)
                        }
                    }
                    val msg = buildString {
                        append("복원 완료: ${importResult.chunks.size}개")
                        if (importResult.failedCount > 0) {
                            append("\n(해시 불일치 ${importResult.failedCount}개 건너뜀)")
                        }
                    }
                    MaterialAlertDialogBuilder(this@SettingsActivity)
                        .setTitle("✅ 복원 완료")
                        .setMessage(msg)
                        .setPositiveButton("확인", null)
                        .show()
                },
                onFailure = { e ->
                    MaterialAlertDialogBuilder(this@SettingsActivity)
                        .setTitle("❌ 복원 실패")
                        .setMessage(e.message ?: "알 수 없는 오류")
                        .setPositiveButton("확인", null)
                        .show()
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ★ 상단 상태바 / 하단 제스처바에 내용이 가리지 않도록 인셋 패딩
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        billingManager.connect()
        setupTrialStatus()
        setupSttCredit()
        setupVersionInfo()
        setupBatteryOptimization()
        setupBackup()
        setupResetDb()

        binding.tvWebsite.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://safebuffer.app")))
        }
        binding.tvContact.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@safebuffer.app")))
        }

        binding.btnUpgrade.setOnClickListener {
            if (tierManager.isPremium()) {
                Toast.makeText(this, "Premium을 이용 중입니다.", Toast.LENGTH_SHORT).show()
            } else {
                showPurchaseDialog()
            }
        }

        // 구독 상태 실시간 반영
        lifecycleScope.launch {
            billingManager.isPremium.collect {
                setupTrialStatus()
                setupSttCredit()
            }
        }

        // 일회성 상품 구매 이벤트 수신
        lifecycleScope.launch {
            billingManager.oneTimePurchaseEvent.collect { productId ->
                when (productId) {
                    BillingManager.PRODUCT_UNLOCK -> {
                        // 정식 버전 구매 — 맛보기 1시간은 SttQuotaManager 가 자동 지급.
                        setupTrialStatus()
                        setupSttCredit()
                        Toast.makeText(
                            this@SettingsActivity,
                            "정식 버전이 활성화되었습니다. 맛보기 1시간이 지급되었어요.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    BillingManager.PRODUCT_STT_CREDIT_10H -> {
                        sttQuotaManager.addPurchasedHours(SttQuotaManager.HOURS_PER_CREDIT_PACK)
                        setupSttCredit()
                        Toast.makeText(
                            this@SettingsActivity,
                            "AI 대화 기록 ${SttQuotaManager.HOURS_PER_CREDIT_PACK}시간이 추가되었습니다.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        // 구독 복원 (롱클릭)
        binding.btnUpgrade.setOnLongClickListener {
            lifecycleScope.launch {
                billingManager.queryExistingPurchases()
                if (tierManager.isPremium()) {
                    Toast.makeText(this@SettingsActivity,
                        "구매가 복원되었습니다.", Toast.LENGTH_SHORT).show()
                    setupTrialStatus()
                    setupSttCredit()
                } else {
                    Toast.makeText(this@SettingsActivity,
                        "복원할 구매 내역이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateBatteryStatus()
        setupSttCredit()
    }

    private fun setupTrialStatus() {
        // 카드 본문(무료/정식 설명)은 레이아웃에 고정. 여기서는 구매 버튼 상태만 바꾼다.
        if (tierManager.isPremium()) {
            binding.btnUpgrade.text      = "✅ 정식 버전 이용 중"
            binding.btnUpgrade.isEnabled = false
            binding.btnUpgrade.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF_1A_2A_1A.toInt())
        } else {
            binding.btnUpgrade.text      = "정식 버전 구매 (5,500원)"
            binding.btnUpgrade.isEnabled = true
            binding.btnUpgrade.backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF_2A_4A_8A.toInt())
        }
    }

    private fun setupSttCredit() {
        // 카드는 항상 보인다 — 미구매자도 추가 상품이 있다는 걸 알아야 한다.
        binding.cardSttCredit.visibility = android.view.View.VISIBLE
        val isPremium = tierManager.isPremium()
        val creditPrice = billingManager.getSttCreditPrice()

        if (isPremium) {
            binding.tvSttCreditBalance.text = sttQuotaManager.remainingPurchasedText()
            binding.btnBuySttCredit.isEnabled = true
            binding.btnBuySttCredit.text = "5시간 구매 ($creditPrice)"
            binding.btnBuySttCredit.setOnClickListener {
                val launched = billingManager.launchSttCreditPurchase(this)
                if (!launched) {
                    Toast.makeText(this,
                        "결제 서비스 연결 중입니다. 잠시 후 다시 시도해주세요.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // 미구매자: 정식 버전을 먼저 사야 한다는 것만 안내
            binding.tvSttCreditBalance.text = "—"
            binding.btnBuySttCredit.isEnabled = false
            binding.btnBuySttCredit.text = "정식 버전 구매 후 이용 가능"
            binding.btnBuySttCredit.setOnClickListener(null)
        }
    }

    private fun showPurchaseDialog() {
        val price = billingManager.getUnlockPrice()
        MaterialAlertDialogBuilder(this)
            .setTitle("정식 버전 구매")
            .setMessage(
                "한 번만 결제하면 계속 사용합니다.\n\n" +
                "· AI 대화 기록 만들기 (맛보기 1시간 포함)\n" +
                "· 대화 기록 문서 (PDF·Word)\n" +
                "· 증거 무제한 보관 · 48시간 버퍼\n" +
                "· 서버 시각 인증\n\n" +
                "추가 변환이 필요하면 'AI 대화 기록 5시간(12,900원)'을 따로 구매할 수 있습니다."
            )
            .setPositiveButton("구매 ($price)") { _, _ ->
                val launched = billingManager.launchUnlockPurchase(this)
                if (!launched) {
                    Toast.makeText(this,
                        "결제 서비스 연결 중입니다. 잠시 후 다시 시도해주세요.",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupVersionInfo() {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            // ★ versionName 만 보여주면 "1.0.0"이 모든 빌드에 동일해서
            //   어느 빌드가 설치됐는지 확인할 방법이 없다. versionCode 를 함께 표시한다.
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION") info.versionCode.toLong()
            }
            binding.tvVersion.text = "${info.versionName} ($code)"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvVersion.text = "알 수 없음"
        }
    }

    private fun setupBatteryOptimization() {
        updateBatteryStatus()
        binding.btnBatteryOptimization.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = getSystemService(PowerManager::class.java)
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } else {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
            }
        }
    }

    private fun setupBackup() {
        // ── 내보내기 ──
        binding.btnExportBackup.setOnClickListener {
            lifecycleScope.launch {
                val lockedChunks = withContext(Dispatchers.IO) {
                    chunkRepository.getAllLockedOnce()
                }
                if (lockedChunks.isEmpty()) {
                    Toast.makeText(this@SettingsActivity,
                        "보호된 녹음이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Toast.makeText(this@SettingsActivity,
                    "백업 생성 중...", Toast.LENGTH_SHORT).show()

                val result = withContext(Dispatchers.IO) {
                    BackupManager.exportBackup(this@SettingsActivity, lockedChunks)
                }
                result.fold(
                    onSuccess = { zipFile ->
                        val uri = FileProvider.getUriForFile(
                            this@SettingsActivity,
                            "${packageName}.fileprovider",
                            zipFile
                        )
                        startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "SafeBuffer 증거 백업")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, "백업 파일 저장"
                        ))
                    },
                    onFailure = { e ->
                        Toast.makeText(this@SettingsActivity,
                            "백업 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        // ── 가져오기 ──
        binding.btnImportBackup.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("📥 백업 복원")
                .setMessage(
                    "SafeBuffer ZIP 백업 파일을 선택하면 보호된 녹음을 복원합니다.\n\n" +
                    "· 기존 보호된 녹음은 삭제되지 않습니다\n" +
                    "· SHA-256 해시 검증을 통과한 파일만 복원됩니다"
                )
                .setPositiveButton("파일 선택") { _, _ ->
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun setupResetDb() {
        binding.btnResetDb.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ 전체 초기화")
                .setMessage("모든 녹음 파일과 보관된 녹음이 영구 삭제됩니다.\n복구할 수 없습니다. 계속할까요?")
                .setPositiveButton("초기화") { _, _ ->
                    lifecycleScope.launch {
                        // 1. recorder 해제 + 이전 DB 작업 종료를 실제 완료 신호로 기다린다.
                        if (RecordingService.isServiceActive) {
                            val baseline = RecordingService.resetStopCounter.value
                            startService(RecordingService.resetStopIntent(this@SettingsActivity))
                            val stopped = withTimeoutOrNull(10_000L) {
                                RecordingService.resetStopCounter.first { it > baseline }
                            }
                            if (stopped == null) {
                                Toast.makeText(
                                    this@SettingsActivity,
                                    "녹음 종료를 확인하지 못해 초기화를 취소했습니다.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }
                        }
                        // 2. DB + 파일 전체 삭제
                        withContext(Dispatchers.IO) { chunkRepository.resetAll() }
                        // 3. 녹음 시작 시각 초기화 — 타이머가 이전 시각부터 계산되지 않도록
                        getSharedPreferences(RecordingService.PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .remove(RecordingService.KEY_RECORDING_START_MS)
                            .apply()
                        // SafeBuffer는 항상 자동 녹음이 기본이다. 초기화 때문에 중지한
                        // 서비스를 데이터 삭제가 끝난 직후 새 세션(00:00)으로 다시 시작한다.
                        ContextCompat.startForegroundService(
                            this@SettingsActivity,
                            RecordingService.startIntent(this@SettingsActivity)
                        )
                        Toast.makeText(
                            this@SettingsActivity,
                            "초기화 완료. 녹음을 다시 시작했습니다.",
                            Toast.LENGTH_LONG
                        ).show()
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
            binding.tvBatteryStatus.text =
                if (excluded) "✅ 제외됨" else "⚠️ 제외 안됨"
            binding.tvBatteryStatus.setTextColor(
                if (excluded) 0xFF_44_BB_44.toInt() else 0xFF_FF_99_22.toInt()
            )
            binding.btnBatteryOptimization.text =
                if (excluded) "배터리 설정 보기" else "배터리 최적화 제외 설정하기 →"
        }
    }
}
