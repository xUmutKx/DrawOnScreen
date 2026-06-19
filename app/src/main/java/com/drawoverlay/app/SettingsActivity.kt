package com.drawoverlay.app

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.drawoverlay.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = AppPrefs(this)

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Ayarlar"

        loadPrefs()
        setupListeners()
    }

    private fun loadPrefs() {
        b.switchMinimalUi.isChecked       = prefs.minimalUi
        b.switchMinimizeOnDraw.isChecked  = prefs.minimizeOnDraw
        b.switchKeepScreenOn.isChecked    = prefs.keepScreenOn
        b.switchVibrate.isChecked         = prefs.vibrateOnStart
        b.switchFingerDraw.isChecked      = prefs.fingerDraw
        b.switchStylusOnly.isChecked      = prefs.stylusOnly
        b.switchAutoPassthrough.isChecked = prefs.autoPassthrough
        b.switchSmoothing.isChecked       = prefs.smoothing
        b.switchPressure.isChecked        = prefs.pressureSensitive
        b.switchShowUndo.isChecked        = prefs.showUndo
        b.switchShowRedo.isChecked        = prefs.showRedo
        b.switchShowEraser.isChecked      = prefs.showEraser
        b.switchShowShapes.isChecked      = prefs.showShapes
        b.switchShowLaser.isChecked       = prefs.showLaser
        b.switchShowSpotlight.isChecked   = prefs.showSpotlight
        b.switchShowScreenshot.isChecked  = prefs.showScreenshot
        b.switchShowPassthrough.isChecked = prefs.showPassthrough
        b.switchToolbarRight.isChecked    = prefs.toolbarSide
        b.switchShowFountain.isChecked    = prefs.showFountain
        b.switchShowCalligraphy.isChecked = prefs.showCalligraphy
        b.switchShowPencil.isChecked      = prefs.showPencil
        b.switchShowBrush.isChecked       = prefs.showBrush
        b.switchShowMarker.isChecked      = prefs.showMarker
        b.switchHwAcceleration.isChecked  = prefs.hwAcceleration
        b.switchDebugOverlay.isChecked    = prefs.debugOverlay
        b.switchVerboseLogging.isChecked  = prefs.verboseLogging
        b.switchDisableCrashLog.isChecked = prefs.disableCrashLog
    }

    private fun setupListeners() {
        b.switchMinimalUi.setOnCheckedChangeListener       { _, v -> prefs.minimalUi = v }
        b.switchMinimizeOnDraw.setOnCheckedChangeListener  { _, v -> prefs.minimizeOnDraw = v }
        b.switchKeepScreenOn.setOnCheckedChangeListener    { _, v ->
            prefs.keepScreenOn = v
            if (v) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        b.switchVibrate.setOnCheckedChangeListener         { _, v -> prefs.vibrateOnStart = v }
        b.switchFingerDraw.setOnCheckedChangeListener      { _, v -> prefs.fingerDraw = v }
        b.switchStylusOnly.setOnCheckedChangeListener      { _, v -> prefs.stylusOnly = v }
        b.switchAutoPassthrough.setOnCheckedChangeListener { _, v -> prefs.autoPassthrough = v }
        b.switchSmoothing.setOnCheckedChangeListener       { _, v -> prefs.smoothing = v }
        b.switchPressure.setOnCheckedChangeListener        { _, v -> prefs.pressureSensitive = v }
        b.switchShowUndo.setOnCheckedChangeListener        { _, v -> prefs.showUndo = v }
        b.switchShowRedo.setOnCheckedChangeListener        { _, v -> prefs.showRedo = v }
        b.switchShowEraser.setOnCheckedChangeListener      { _, v -> prefs.showEraser = v }
        b.switchShowShapes.setOnCheckedChangeListener      { _, v -> prefs.showShapes = v }
        b.switchShowLaser.setOnCheckedChangeListener       { _, v -> prefs.showLaser = v }
        b.switchShowSpotlight.setOnCheckedChangeListener   { _, v -> prefs.showSpotlight = v }
        b.switchShowScreenshot.setOnCheckedChangeListener  { _, v -> prefs.showScreenshot = v }
        b.switchShowPassthrough.setOnCheckedChangeListener { _, v -> prefs.showPassthrough = v }
        b.switchToolbarRight.setOnCheckedChangeListener    { _, v -> prefs.toolbarSide = v }
        b.switchShowFountain.setOnCheckedChangeListener    { _, v -> prefs.showFountain = v }
        b.switchShowCalligraphy.setOnCheckedChangeListener { _, v -> prefs.showCalligraphy = v }
        b.switchShowPencil.setOnCheckedChangeListener      { _, v -> prefs.showPencil = v }
        b.switchShowBrush.setOnCheckedChangeListener       { _, v -> prefs.showBrush = v }
        b.switchShowMarker.setOnCheckedChangeListener      { _, v -> prefs.showMarker = v }
        b.switchHwAcceleration.setOnCheckedChangeListener  { _, v -> prefs.hwAcceleration = v }
        b.switchDebugOverlay.setOnCheckedChangeListener    { _, v -> prefs.debugOverlay = v }
        b.switchVerboseLogging.setOnCheckedChangeListener  { _, v -> prefs.verboseLogging = v }
        b.switchDisableCrashLog.setOnCheckedChangeListener { _, v -> prefs.disableCrashLog = v }

        b.rowResetAll.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Sıfırla")
                .setMessage("Tüm ayarlar varsayılana dönecek. Emin misiniz?")
                .setPositiveButton("Sıfırla") { _, _ ->
                    prefs.resetAll()
                    loadPrefs()
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
