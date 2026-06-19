package com.drawoverlay.app

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.init(this)
        CrashLogger.log(this, "App", "Uygulama başlatıldı - Android SDK ${android.os.Build.VERSION.SDK_INT}")
    }
}
