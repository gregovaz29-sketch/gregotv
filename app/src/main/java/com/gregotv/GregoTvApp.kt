package com.gregotv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GregoTvApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Record uncaught exceptions to a file readable from Settings, so a
        // crash on the TV box can be diagnosed without adb.
        CrashReporter.install(this)
    }
}
