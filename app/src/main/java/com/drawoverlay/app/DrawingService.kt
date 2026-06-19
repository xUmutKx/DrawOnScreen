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

    private val CHANNEL_ID = "draw_overlay_channel"
    private val NOTIF_ID   = 1

    private val canvasParams get() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    )
    private var currentCanvasParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        CrashLogger.log(this, "Service", "onCreate başladı")

        try {
            createNotificationChannel()
            startForeground(NOTIF_ID, buildNotification())
            CrashLogger.log(this, "Service", "startForeground OK")
        } catch (e: Exception) {
            CrashLogger.log(this, "Service", "startForeground HATA", e)
            stopSelf(); return
        }

        isRunning = true

        try {
            wm = getSystemService(WINDOW_SERVICE) as WindowManager
            CrashLogger.log(this, "Service", "WindowManager OK")
        } catch (e: Exception) {
            CrashLogger.log(this, "Service", "WindowManager HATA", e)
            stopSelf(); return
        }

        val prefs = AppPrefs(this)

        // Titreşim
        if (prefs.vibrateOnStart) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator.vibrate(android.os.VibrationEffect.createOneShot(60, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(60)
                }
            } catch (e: Exception) {
                CrashLogger.log(this, "Service", "Titreşim hatası", e)
            }
        }

        // Canvas ekle
        try {
            val params = canvasParams
            currentCanvasParams = params
            drawingCanvas = DrawingCanvas(this, prefs)
            wm.addView(drawingCanvas, params)
            CrashLogger.log(this, "Service", "DrawingCanvas eklendi")
        } catch (e: Exception) {
            CrashLogger.log(this, "Service", "DrawingCanvas HATA - FATAL", e)
            CrashLogger.saveToDownloads(this, "CRASH - DrawingCanvas eklenemedi: ${e.message}")
            stopSelf(); return
        }

        // Toolbar ekle
        if (!prefs.minimalUi) {
            try {
                val canvas = drawingCanvas!!
                floatingToolbar = FloatingToolbar(
                    context             = this,
                    wm                  = wm,
                    canvas              = canvas,
                    prefs               = prefs,
                    onClose             = { stopSelf() },
                    onTogglePassThrough = { pt -> setPassThrough(pt) },
                    onScreenshot        = { takeScreenshotOverlay() }
                )
                wm.addView(floatingToolbar!!.getView(), floatingToolbar!!.params)
                CrashLogger.log(this, "Service", "FloatingToolbar eklendi")
            } catch (e: Exception) {
                CrashLogger.log(this, "Service", "FloatingToolbar HATA", e)
                CrashLogger.saveToDownloads(this, "CRASH - Toolbar eklenemedi: ${e.message}")
                // Toolbar olmadan devam et - canvas çalışmaya devam eder
            }
        }

        CrashLogger.log(this, "Service", "onCreate tamamlandı")
    }

    private fun setPassThrough(enabled: Boolean) {
        val params = currentCanvasParams ?: return
        params.flags = if (enabled) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }
        try {
            drawingCanvas?.let { wm.updateViewLayout(it, params) }
        } catch (e: Exception) {
            CrashLogger.log(this, "Service", "setPassThrough hatası", e)
        }
    }

    private fun takeScreenshotOverlay() {
        floatingToolbar?.startScreenshotCrop(drawingCanvas?.getBitmap())
    }

    override fun onDestroy() {
        CrashLogger.log(this, "Service", "onDestroy")
        isRunning = false
        try { drawingCanvas?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { floatingToolbar?.getView()?.let { wm.removeView(it) } } catch (_: Exception) {}
        drawingCanvas = null
        floatingToolbar = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") stopSelf()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.channel_desc)
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
            .setSmallIcon(R.drawable.ic_notif_pencil)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_close, getString(R.string.close), stopPi)
            .setOngoing(true).setSilent(true).setColor(Color.WHITE).build()
    }
}
