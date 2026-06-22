package com.drawoverlay.app

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        CrashLogger.init(this)
        CrashLogger.log(this, "App", "Uygulama başlatıldı - Android SDK ${android.os.Build.VERSION.SDK_INT}")
    }
}
