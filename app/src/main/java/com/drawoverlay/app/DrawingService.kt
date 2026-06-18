package com.drawoverlay.app

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import android.graphics.Color

class DrawingService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var drawingCanvas: DrawingCanvas
    private var floatingToolbar: FloatingToolbar? = null

    private val CHANNEL_ID = "draw_overlay_channel"
    private val NOTIF_ID   = 1

    private val canvasParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    )

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        drawingCanvas = DrawingCanvas(this)
        wm.addView(drawingCanvas, canvasParams)

        floatingToolbar = FloatingToolbar(
            context             = this,
            wm                  = wm,
            canvas              = drawingCanvas,
            onClose             = { stopSelf() },
            onTogglePassThrough = { passThrough -> setPassThrough(passThrough) },
            onScreenshot        = { takeScreenshotOverlay() }
        )
        wm.addView(floatingToolbar!!.getView(), floatingToolbar!!.params)
    }

    private fun setPassThrough(enabled: Boolean) {
        val flags = if (enabled) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }
        canvasParams.flags = flags
        wm.updateViewLayout(drawingCanvas, canvasParams)
    }

    private fun takeScreenshotOverlay() {
        // Capture current canvas + show crop UI via toolbar
        val bmp = drawingCanvas.getBitmap()
        floatingToolbar?.startScreenshotCrop(bmp)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            wm.removeView(drawingCanvas)
            floatingToolbar?.getView()?.let { wm.removeView(it) }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_desc)
            enableLights(false)
            enableVibration(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, DrawingService::class.java).apply { action = "STOP" }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_draw_logo)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_close, getString(R.string.close), stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setColor(Color.WHITE)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") stopSelf()
        return START_NOT_STICKY
    }
}
