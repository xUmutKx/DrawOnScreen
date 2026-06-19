package com.drawoverlay.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.drawoverlay.app.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPrefs

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // İzin verildiyse hemen başlat — kullanıcı geri döndüğünde onResume da çalışır
        if (Settings.canDrawOverlays(this)) {
            startDrawingService()
        } else {
            Snackbar.make(binding.root, "İzin verilmedi. Ayarlardan etkinleştirin.", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = AppPrefs(this)

        if (prefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.btnStart.setOnClickListener {
            if (DrawingService.isRunning) {
                stopService(Intent(this, DrawingService::class.java))
                // Kısa gecikme ile state güncelle — service onDestroy async
                Handler(Looper.getMainLooper()).postDelayed({ updateButtonState() }, 300)
            } else if (Settings.canDrawOverlays(this)) {
                startDrawingService()
            } else {
                requestOverlayPermission()
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
    }

    private fun updateButtonState() {
        val running = DrawingService.isRunning
        binding.btnStart.text = if (running) "Stop Drawing" else "Start Drawing"
        binding.tvStatus.text = if (running) "● Active" else "● Idle"
        binding.btnStart.setIconResource(if (running) R.drawable.ic_close else R.drawable.ic_play)
        binding.btnStart.setTextColor(if (running) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
        binding.btnStart.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (running) 0xFF333333.toInt() else 0xFFFFFFFF.toInt()
        )
        binding.btnStart.iconTint = android.content.res.ColorStateList.valueOf(
            if (running) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        )
    }

    private fun startDrawingService() {
        try {
            startForegroundService(Intent(this, DrawingService::class.java))
            // Kısa gecikme: service isRunning=true yapmadan önce buton state güncellenmesi
            Handler(Looper.getMainLooper()).postDelayed({ updateButtonState() }, 200)
            if (prefs.minimizeOnDraw) {
                Handler(Looper.getMainLooper()).postDelayed({
                    try { moveTaskToBack(true) } catch (_: Exception) {}
                }, 500)
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Başlatılamadı: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun requestOverlayPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("İzin Gerekli")
            .setMessage("Drawly'nin \"Diğer uygulamaların üzerinde göster\" iznine ihtiyacı var.\n\nAç → Drawly'yi bul → Açık konuma getir → Geri dön.")
            .setPositiveButton("Ayarları Aç") { _, _ ->
                overlayPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
