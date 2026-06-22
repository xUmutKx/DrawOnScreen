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
        if (Settings.canDrawOverlays(this)) {
            startDrawingService()
        } else {
            Snackbar.make(binding.root, "Permission not granted. Please enable it in Settings.", Snackbar.LENGTH_LONG).show()
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
        binding.tvStatus.text = if (running) "● ACTIVE" else "● IDLE"
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
            Handler(Looper.getMainLooper()).postDelayed({ updateButtonState() }, 200)
            if (prefs.minimizeOnDraw) {
                Handler(Looper.getMainLooper()).postDelayed({
                    try { moveTaskToBack(true) } catch (_: Exception) {}
                }, 500)
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Failed to start: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun requestOverlayPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage("Drawly needs 'Display over other apps' permission to work.\n\nOpen Settings -> Find Drawly -> Turn on the toggle -> Come back.")
            .setPositiveButton("Open Settings") { _, _ ->
                overlayPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
