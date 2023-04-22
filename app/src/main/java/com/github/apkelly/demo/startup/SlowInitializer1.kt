package com.github.apkelly.demo.startup

import android.content.Context
import com.github.apkelly.bolt.startup.BoltInitializer
import kotlinx.coroutines.delay

class SlowInitializer1: BoltInitializer {

    override suspend fun create(context: Context) {
        println("SlowInitializer1: pre-delay")
        delay(4_000)
        println("SlowInitializer1: post-delay")
    }

    override fun dependencies(): List<Class<out BoltInitializer>> {
        return emptyList()
    }
}