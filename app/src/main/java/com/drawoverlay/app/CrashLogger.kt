package com.drawoverlay.app

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashLogger {

    private const val LOG_DIR = "DrawOnScreen_Logs"
    private const val MAX_LOGS = 15
    private const val PREFS_NAME = "crash_logs"
    private const val PREFS_KEY = "last_crash"

    fun init(context: Context) {
        // SharedPreferences her zaman çalışır, dosya sistemi izinlerinden bağımsızdır.
        // Bu yüzden bu birincil log kanalı.
        val appCtx = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try { saveCrashEverywhere(appCtx, thread, throwable) } catch (_: Throwable) {}
            // Sistemin varsayılan davranışını koru ki cihaz/loglama bozulmasın
            defaultHandler?.uncaughtException(thread, throwable)
        }
        log(context, "CrashLogger", "init() çağrıldı, handler kuruldu")
    }

    fun log(context: Context, tag: String, msg: String, e: Throwable? = null) {
        val line = buildString {
            append("[${timestamp()}] [$tag] $msg")
            if (e != null) {
                val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
                append("\n"); append(sw.toString())
            }
        }
        // 1) SharedPreferences -- en garantili, asla başarısız olmaz
        try {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = p.getString(PREFS_KEY, "") ?: ""
            val combined = (existing + "\n" + line).takeLast(20000) // son 20k karakter
            p.edit().putString(PREFS_KEY, combined).apply()
        } catch (_: Throwable) {}

        // 2) Dosyaya da yazmayı dene (varsa)
        try { getLogFile(context)?.appendText("$line\n") } catch (_: Throwable) {}

        // 3) Logcat'e de yaz (adb logcat ile görülebilir)
        try { android.util.Log.e("DrawOnScreen-$tag", msg, e) } catch (_: Throwable) {}
    }

    private fun saveCrashEverywhere(context: Context, thread: Thread, t: Throwable) {
        val sw = StringWriter(); t.printStackTrace(PrintWriter(sw))
        val info = buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Time    : ${timestamp()}")
            appendLine("Android : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device  : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread  : ${thread.name}")
            appendLine("")
            appendLine(sw.toString())
        }
        // SharedPreferences -- HER ZAMAN ÇALIŞIR
        try {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().putString(PREFS_KEY, info).apply()
        } catch (_: Throwable) {}

        // Dosyaya da yaz (mümkünse)
        try { getLogFile(context)?.appendText(info) } catch (_: Throwable) {}
        try { saveToDownloads(context, info) } catch (_: Throwable) {}
    }

    /** SharedPreferences'taki son logu okur -- her zaman çalışır. */
    fun getLastLog(context: Context): String {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREFS_KEY, "") ?: ""
        } catch (_: Throwable) { "" }
    }

    fun clearLog(context: Context) {
        try { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply() } catch (_: Throwable) {}
    }

    private fun getLogFile(context: Context): File? {
        return try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val logDir = File(dir, LOG_DIR).also { it.mkdirs() }
            val logs = logDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
            if (logs.size >= MAX_LOGS) logs.take(logs.size - MAX_LOGS + 1).forEach { it.delete() }
            File(logDir, "log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt")
        } catch (_: Throwable) { null }
    }

    fun saveToDownloads(context: Context, msg: String) {
        try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(downloads, LOG_DIR).also { it.mkdirs() }
            val file = File(dir, "draw_log_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.txt")
            file.appendText("[${timestamp()}] $msg\n")
        } catch (_: Throwable) {}
    }

    private fun timestamp() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}
