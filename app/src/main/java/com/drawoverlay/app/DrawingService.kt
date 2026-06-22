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
            else -> {
                floatingToolbar?.refreshUi()
            }
        }
    }

    private val CHANNEL_ID = "draw_overlay_channel"
    private val NOTIF_ID = 1

    private var currentCanvasParams: WindowManager.LayoutParams? = null

    private fun makeCanvasParams(passThrough: Boolean = false) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        if (passThrough)
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        else
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    )

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(NOTIF_ID, buildNotification())
        } catch (e: Exception) {
            CrashLogger.log(this, "Service", "Failed to start foreground", e)
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
            CrashLogger.log(this, "Service", "Failed to add canvas", e)
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
            CrashLogger.log(this, "Service", "Failed to add toolbar", e)
        }
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
        prefs.unregisterListener(prefChangeListener)
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
        val ch = NotificationChannel(CHANNEL_ID, "Drawing Service", NotificationManager.IMPORTANCE_LOW).apply {
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
            .setContentTitle("Drawly Active")
            .setContentText("Drawing overlay is running")
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_close, "Stop", stopPi)
            .setOngoing(true).setSilent(true).setColor(Color.WHITE).build()
    }
}
