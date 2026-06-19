package com.drawoverlay.app

import android.content.Context
import android.content.SharedPreferences

class AppPrefs(context: Context) {
    private val p: SharedPreferences =
        context.getSharedPreferences("draw_prefs", Context.MODE_PRIVATE)

    // Interface
    var minimalUi        get() = p.getBoolean("minimal_ui", false)        ; set(v) = p.edit().putBoolean("minimal_ui", v).apply()
    var minimizeOnDraw   get() = p.getBoolean("minimize_on_draw", true)   ; set(v) = p.edit().putBoolean("minimize_on_draw", v).apply()
    var keepScreenOn     get() = p.getBoolean("keep_screen_on", false)    ; set(v) = p.edit().putBoolean("keep_screen_on", v).apply()
    var vibrateOnStart   get() = p.getBoolean("vibrate_start", false)     ; set(v) = p.edit().putBoolean("vibrate_start", v).apply()
    var darkOverlay      get() = p.getBoolean("dark_overlay", false)      ; set(v) = p.edit().putBoolean("dark_overlay", v).apply()

    // Drawing
    var stylusOnly       get() = p.getBoolean("stylus_only", false)       ; set(v) = p.edit().putBoolean("stylus_only", v).apply()
    var fingerDraw       get() = p.getBoolean("finger_draw", true)        ; set(v) = p.edit().putBoolean("finger_draw", v).apply()
    var autoPassthrough  get() = p.getBoolean("auto_passthrough", false)  ; set(v) = p.edit().putBoolean("auto_passthrough", v).apply()
    var smoothing        get() = p.getBoolean("smoothing", true)          ; set(v) = p.edit().putBoolean("smoothing", v).apply()
    var pressureSensitive get() = p.getBoolean("pressure_sensitive", true); set(v) = p.edit().putBoolean("pressure_sensitive", v).apply()

    // Toolbar visibility
    var showUndo         get() = p.getBoolean("show_undo", true)          ; set(v) = p.edit().putBoolean("show_undo", v).apply()
    var showRedo         get() = p.getBoolean("show_redo", false)         ; set(v) = p.edit().putBoolean("show_redo", v).apply()
    var showEraser       get() = p.getBoolean("show_eraser", true)        ; set(v) = p.edit().putBoolean("show_eraser", v).apply()
    var showShapes       get() = p.getBoolean("show_shapes", true)        ; set(v) = p.edit().putBoolean("show_shapes", v).apply()
    var showLaser        get() = p.getBoolean("show_laser", true)         ; set(v) = p.edit().putBoolean("show_laser", v).apply()
    var showSpotlight    get() = p.getBoolean("show_spotlight", true)     ; set(v) = p.edit().putBoolean("show_spotlight", v).apply()
    var showScreenshot   get() = p.getBoolean("show_screenshot", true)    ; set(v) = p.edit().putBoolean("show_screenshot", v).apply()
    var showPassthrough  get() = p.getBoolean("show_passthrough", true)   ; set(v) = p.edit().putBoolean("show_passthrough", v).apply()

    // Stylus
    var stylusButtonAction get() = p.getString("stylus_btn", "ERASER") ?: "ERASER" ; set(v) = p.edit().putString("stylus_btn", v).apply()

    // Defaults
    var defaultTool      get() = p.getString("default_tool", "PEN") ?: "PEN" ; set(v) = p.edit().putString("default_tool", v).apply()
    var defaultColor     get() = p.getInt("default_color", -1)               ; set(v) = p.edit().putInt("default_color", v).apply()
    var defaultStroke    get() = p.getFloat("default_stroke", 8f)            ; set(v) = p.edit().putFloat("default_stroke", v).apply()
    var toolbarOpacity   get() = p.getInt("toolbar_opacity", 220)            ; set(v) = p.edit().putInt("toolbar_opacity", v).apply()
    var toolbarSide      get() = p.getBoolean("toolbar_side_right", false)   ; set(v) = p.edit().putBoolean("toolbar_side_right", v).apply()
}
