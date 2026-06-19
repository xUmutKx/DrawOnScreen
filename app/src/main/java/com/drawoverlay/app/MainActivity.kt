package com.drawoverlay.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.drawoverlay.app.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            CrashLogger.log(this, "Main", "Overlay izni verildi")
            startDrawingService()
        } else {
            CrashLogger.log(this, "Main", "Overlay izni REDDEDİLDİ")
            Snackbar.make(binding.root, getString(R.string.permission_denied), Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CrashLogger.log(this, "Main", "MainActivity onCreate, overlay izni: ${Settings.canDrawOverlays(this)}")

        binding.btnStart.setOnClickListener {
            if (Settings.canDrawOverlays(this)) startDrawingService()
            else requestOverlayPermission()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Uzun basınca crash loglarını göster
        binding.btnStart.setOnLongClickListener {
            showCrashLogs()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
    }

    private fun updateButtonState() {
        val running = DrawingService.isRunning
        binding.btnStart.text = if (running) getString(R.string.stop_drawing) else getString(R.string.start_drawing)
        binding.tvStatus.text = if (running) getString(R.string.status_active) else getString(R.string.status_idle)
        binding.btnStart.setIconResource(if (running) R.drawable.ic_close else R.drawable.ic_play)
    }

    private fun startDrawingService() {
        CrashLogger.log(this, "Main", "startDrawingService çağrıldı, isRunning=${DrawingService.isRunning}")
        if (DrawingService.isRunning) {
            stopService(Intent(this, DrawingService::class.java))
            updateButtonState()
            return
        }
        try {
            startForegroundService(Intent(this, DrawingService::class.java))
            updateButtonState()

            // ÖNEMLİ: moveTaskToBack'i hemen çağırmak, servisin startForeground()
            // çağrısını zamanında tamamlamasını engelleyebilir (Android 12+ kısıtlaması).
            // Activity arka plana geçerken servis henüz foreground bildirimini
            // göstermemişse sistem servisi anında öldürür (ForegroundServiceDidNotStartInTimeException).
            // Bu yüzden küçük bir gecikme veriyoruz, servise nefes alma payı bırakıyoruz.
            val prefs = AppPrefs(this)
            if (prefs.minimizeOnDraw) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try { moveTaskToBack(true) } catch (e: Exception) {
                        CrashLogger.log(this, "Main", "moveTaskToBack hatası", e)
                    }
                }, 600)
            }
        } catch (e: Exception) {
            CrashLogger.log(this, "Main", "startForegroundService HATA", e)
            Snackbar.make(binding.root, "Servis başlatılamadı: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun requestOverlayPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.permission_message))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                overlayPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCrashLogs() {
        try {
            val dirs = listOf(
                getExternalFilesDir(null),
                filesDir,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            )
            val logs = dirs.filterNotNull()
                .flatMap { File(it, "DrawOnScreen_Logs").listFiles()?.toList() ?: emptyList() }
                .sortedByDescending { it.lastModified() }

            if (logs.isEmpty()) {
                Snackbar.make(binding.root, "Crash logu bulunamadı", Snackbar.LENGTH_SHORT).show()
                return
            }

            val lastLog = logs.first().readText().takeLast(2000)
            MaterialAlertDialogBuilder(this)
                .setTitle("Son Crash Logu (${logs.size} dosya)")
                .setMessage(lastLog)
                .setPositiveButton("Tamam", null)
                .setNeutralButton("Log Klasörü") { _, _ ->
                    val path = logs.first().parentFile?.absolutePath ?: ""
                    Snackbar.make(binding.root, path, Snackbar.LENGTH_LONG).show()
                }
                .show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Log okunamadı: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }
}
