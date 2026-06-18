package com.drawoverlay.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.drawoverlay.app.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // User returned from settings — check permission now
        if (Settings.canDrawOverlays(this)) {
            startDrawingService()
        } else {
            Snackbar.make(
                binding.root,
                "Permission required to draw over other apps.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startDrawingService()
            } else {
                requestOverlayPermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If permission was granted while we were in settings, auto-launch
        if (Settings.canDrawOverlays(this) && intent?.getBooleanExtra("FROM_PERMISSION", false) == true) {
            intent?.removeExtra("FROM_PERMISSION")
            startDrawingService()
        }
        updateButtonState()
    }

    private fun updateButtonState() {
        binding.btnStart.text = getString(R.string.start_drawing)
    }

    private fun startDrawingService() {
        val svcIntent = Intent(this, DrawingService::class.java)
        startForegroundService(svcIntent)
        // Minimise so overlay is visible
        moveTaskToBack(true)
    }

    private fun requestOverlayPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.permission_message))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(settingsIntent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
