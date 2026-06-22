package com.drawoverlay.app

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class DrawingService : Service() {

    companion object { var isRunning = false }

    private lateinit var wm: WindowManager
    private var drawingCanvas: DrawingCanvas? = null
    private var floatingToolbar: FloatingToolbar? = null
    private lateinit var prefs: AppPrefs

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "minimal_ui" -> {
                if (prefs.minimalUi) {
                    floatingToolbar?.getView()?.let { try { wm.removeView(it) } catch (_: Exception) {} }
                    floatingToolbar = null
                } else if (floatingToolbar == null) {
                    addToolbar()
                }
            }
            "toolbar_side_right" -> {
                floatingToolbar?.refreshPosition()
            }
            "finger_draw", "stylus_only" -> {
                // DrawingCanvas already reads prefs on every touch event, but we can force it if needed
            }
            else -> {
                floatingToolbar?.refreshUi()
            }
        }
    }

    private val CHANNEL_ID = "draw_overlay_channel"
    // ... (rest of constants)

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(NOTIF_ID, buildNotification())
        } catch (e: Exception) {
            CrashLogger.log(this, "Service", "startForeground hatası", e)
            stopSelf(); return
        }

        isRunning = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = AppPrefs(this)
        prefs.registerListener(prefChangeListener)

        if (prefs.vibrateOnStart) vibrate()

        try {
            val params = makeCanvasParams(prefs.autoPassthrough)
            currentCanvasParams = params
            drawingCanvas = DrawingCanvas(this, prefs)
            wm.addView(drawingCanvas, params)
        } catch (e: Exception) {
            CrashLogger.log(this, "Service", "Canvas eklenemedi", e)
            stopSelf(); return
        }

        if (!prefs.minimalUi) {
            addToolbar()
        }
    }

    private fun addToolbar() {
        try {
            floatingToolbar = FloatingToolbar(
                context = this, wm = wm, canvas = drawingCanvas!!,
                prefs = prefs,
                onClose = { stopSelf() },
                onTogglePassThrough = { pt -> setPassThrough(pt) },
                onScreenshot = { floatingToolbar?.startScreenshotCrop(drawingCanvas?.getBitmap()) }
            )
            wm.addView(floatingToolbar!!.getView(), floatingToolbar!!.params)
        } catch (e: Exception) {
            CrashLogger.log(this, "Service", "Toolbar eklenemedi", e)
        }
    }

    private fun setPassThrough(enabled: Boolean) {
        // ... (existing code)
    }

    private fun vibrate() {
        // ... (existing code)
    }

    override fun onDestroy() {
        isRunning = false
        prefs.unregisterListener(prefChangeListener)
        try { drawingCanvas?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { floatingToolbar?.getView()?.let { wm.removeView(it) } } catch (_: Exception) {}
        drawingCanvas = null; floatingToolbar = null
        super.onDestroy()
    }

    private fun setPassThrough(enabled: Boolean) {
        val params = currentCanvasParams ?: return
        params.flags = if (enabled)
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        else
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        try { drawingCanvas?.let { wm.updateViewLayout(it, params) } } catch (_: Exception) {}
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator.vibrate(android.os.VibrationEffect.createOneShot(60, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(60)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        isRunning = false
        try { drawingCanvas?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { floatingToolbar?.getView()?.let { wm.removeView(it) } } catch (_: Exception) {}
        drawingCanvas = null; floatingToolbar = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") stopSelf()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            enableLights(false); enableVibration(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(this, 0,
            Intent(this, DrawingService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val openPi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_pencil)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_close, getString(R.string.close), stopPi)
            .setOngoing(true).setSilent(true).setColor(Color.WHITE).build()
    }
}
