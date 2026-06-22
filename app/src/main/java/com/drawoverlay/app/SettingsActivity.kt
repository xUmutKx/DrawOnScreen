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
        b.switchInkBleeding.isChecked     = prefs.inkBleeding
        b.switchShowRuler.isChecked       = prefs.showRuler
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
        // Yardımcı fonksiyon: Satıra tıklandığında switch'i tetikle
        fun setupRow(row: android.view.View, switch: com.google.android.material.materialswitch.MaterialSwitch, prefSetter: (Boolean) -> Unit) {
            row.setOnClickListener { switch.isChecked = !switch.isChecked }
            switch.setOnCheckedChangeListener { _, v -> prefSetter(v) }
        }

        setupRow(b.switchMinimalUi.parent as android.view.View, b.switchMinimalUi) { prefs.minimalUi = it }
        setupRow(b.switchMinimizeOnDraw.parent as android.view.View, b.switchMinimizeOnDraw) { prefs.minimizeOnDraw = it }
        setupRow(b.switchKeepScreenOn.parent as android.view.View, b.switchKeepScreenOn) {
            prefs.keepScreenOn = it
            if (it) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setupRow(b.switchVibrate.parent as android.view.View, b.switchVibrate) { prefs.vibrateOnStart = it }
        setupRow(b.switchFingerDraw.parent as android.view.View, b.switchFingerDraw) { prefs.fingerDraw = it }
        setupRow(b.switchStylusOnly.parent as android.view.View, b.switchStylusOnly) { prefs.stylusOnly = it }
        setupRow(b.switchAutoPassthrough.parent as android.view.View, b.switchAutoPassthrough) { prefs.autoPassthrough = it }
        setupRow(b.switchSmoothing.parent as android.view.View, b.switchSmoothing) { prefs.smoothing = it }
        setupRow(b.switchPressure.parent as android.view.View, b.switchPressure) { prefs.pressureSensitive = it }
        setupRow(b.switchShowUndo.parent as android.view.View, b.switchShowUndo) { prefs.showUndo = it }
        setupRow(b.switchShowRedo.parent as android.view.View, b.switchShowRedo) { prefs.showRedo = it }
        setupRow(b.switchShowEraser.parent as android.view.View, b.switchShowEraser) { prefs.showEraser = it }
        setupRow(b.switchShowShapes.parent as android.view.View, b.switchShowShapes) { prefs.showShapes = it }
        setupRow(b.switchShowLaser.parent as android.view.View, b.switchShowLaser) { prefs.showLaser = it }
        setupRow(b.switchShowSpotlight.parent as android.view.View, b.switchShowSpotlight) { prefs.showSpotlight = it }
        setupRow(b.switchShowScreenshot.parent as android.view.View, b.switchShowScreenshot) { prefs.showScreenshot = it }
        setupRow(b.switchShowPassthrough.parent as android.view.View, b.switchShowPassthrough) { prefs.showPassthrough = it }
        setupRow(b.switchToolbarRight.parent as android.view.View, b.switchToolbarRight) { prefs.toolbarSide = it }
        setupRow(b.switchInkBleeding.parent as android.view.View, b.switchInkBleeding) { prefs.inkBleeding = it }
        setupRow(b.switchShowRuler.parent as android.view.View, b.switchShowRuler) { prefs.showRuler = it }
        setupRow(b.switchShowFountain.parent as android.view.View, b.switchShowFountain) { prefs.showFountain = it }
        setupRow(b.switchShowCalligraphy.parent as android.view.View, b.switchShowCalligraphy) { prefs.showCalligraphy = it }
        setupRow(b.switchShowPencil.parent as android.view.View, b.switchShowPencil) { prefs.showPencil = it }
        setupRow(b.switchShowBrush.parent as android.view.View, b.switchShowBrush) { prefs.showBrush = it }
        setupRow(b.switchShowMarker.parent as android.view.View, b.switchShowMarker) { prefs.showMarker = it }
        setupRow(b.switchHwAcceleration.parent as android.view.View, b.switchHwAcceleration) { prefs.hwAcceleration = it }
        setupRow(b.switchDebugOverlay.parent as android.view.View, b.switchDebugOverlay) { prefs.debugOverlay = it }
        setupRow(b.switchVerboseLogging.parent as android.view.View, b.switchVerboseLogging) { prefs.verboseLogging = it }
        setupRow(b.switchDisableCrashLog.parent as android.view.View, b.switchDisableCrashLog) { prefs.disableCrashLog = it }

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
