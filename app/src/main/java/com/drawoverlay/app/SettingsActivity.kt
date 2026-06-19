package com.drawoverlay.app

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.drawoverlay.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Toolbar null check
        try {
            setSupportActionBar(b.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.settings)
        } catch (_: Exception) {}

        val prefs = AppPrefs(this)

        // Interface
        b.switchMinimalUi.isChecked       = prefs.minimalUi
        b.switchMinimizeOnDraw.isChecked  = prefs.minimizeOnDraw
        b.switchKeepScreenOn.isChecked    = prefs.keepScreenOn
        b.switchVibrate.isChecked         = prefs.vibrateOnStart
        b.switchDarkTheme.isChecked       = prefs.darkOverlay

        // Drawing
        b.switchFingerDraw.isChecked      = prefs.fingerDraw
        b.switchStylusOnly.isChecked      = prefs.stylusOnly
        b.switchAutoPassthrough.isChecked = prefs.autoPassthrough
        b.switchSmoothing.isChecked       = prefs.smoothing
        b.switchPressure.isChecked        = prefs.pressureSensitive

        // Toolbar
        b.switchShowUndo.isChecked        = prefs.showUndo
        b.switchShowRedo.isChecked        = prefs.showRedo
        b.switchShowEraser.isChecked      = prefs.showEraser
        b.switchShowShapes.isChecked      = prefs.showShapes
        b.switchShowLaser.isChecked       = prefs.showLaser
        b.switchShowSpotlight.isChecked   = prefs.showSpotlight
        b.switchShowScreenshot.isChecked  = prefs.showScreenshot
        b.switchShowPassthrough.isChecked = prefs.showPassthrough
        b.switchToolbarRight.isChecked    = prefs.toolbarSide

        // Listeners - Interface
        b.switchMinimalUi.setOnCheckedChangeListener       { _, v -> prefs.minimalUi = v }
        b.switchMinimizeOnDraw.setOnCheckedChangeListener  { _, v -> prefs.minimizeOnDraw = v }
        b.switchKeepScreenOn.setOnCheckedChangeListener    { _, v -> prefs.keepScreenOn = v; updateKeepScreenOn(v) }
        b.switchVibrate.setOnCheckedChangeListener         { _, v -> prefs.vibrateOnStart = v }
        b.switchDarkTheme.setOnCheckedChangeListener       { _, v -> prefs.darkOverlay = v }

        // Listeners - Drawing
        b.switchFingerDraw.setOnCheckedChangeListener      { _, v -> prefs.fingerDraw = v }
        b.switchStylusOnly.setOnCheckedChangeListener      { _, v -> prefs.stylusOnly = v }
        b.switchAutoPassthrough.setOnCheckedChangeListener { _, v -> prefs.autoPassthrough = v }
        b.switchSmoothing.setOnCheckedChangeListener       { _, v -> prefs.smoothing = v }
        b.switchPressure.setOnCheckedChangeListener        { _, v -> prefs.pressureSensitive = v }

        // Listeners - Toolbar
        b.switchShowUndo.setOnCheckedChangeListener        { _, v -> prefs.showUndo = v }
        b.switchShowRedo.setOnCheckedChangeListener        { _, v -> prefs.showRedo = v }
        b.switchShowEraser.setOnCheckedChangeListener      { _, v -> prefs.showEraser = v }
        b.switchShowShapes.setOnCheckedChangeListener      { _, v -> prefs.showShapes = v }
        b.switchShowLaser.setOnCheckedChangeListener       { _, v -> prefs.showLaser = v }
        b.switchShowSpotlight.setOnCheckedChangeListener   { _, v -> prefs.showSpotlight = v }
        b.switchShowScreenshot.setOnCheckedChangeListener  { _, v -> prefs.showScreenshot = v }
        b.switchShowPassthrough.setOnCheckedChangeListener { _, v -> prefs.showPassthrough = v }
        b.switchToolbarRight.setOnCheckedChangeListener    { _, v -> prefs.toolbarSide = v }
    }

    private fun updateKeepScreenOn(on: Boolean) {
        if (on) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
