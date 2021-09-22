package com.living.streamlivingpush

import android.app.Application
import android.content.Context

class AppApplication : Application() {
    companion object {
        var appContext: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this.applicationContext
    }
}