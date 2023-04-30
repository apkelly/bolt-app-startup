package io.github.apkelly.demo

import android.app.Application
import java.time.LocalTime

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        println("DemoApplication onCreate()")
        Timing.applicationOnCreate = LocalTime.now()
    }

}