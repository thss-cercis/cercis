package cn.cercis

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CercisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        application = this
    }

    companion object {
        lateinit var application: Application
            private set
    }
}
