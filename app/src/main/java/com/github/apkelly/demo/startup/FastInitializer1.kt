package com.github.apkelly.demo.startup

import android.content.Context
import com.github.apkelly.bolt.startup.BoltInitializer
import kotlinx.coroutines.delay

class FastInitializer1: BoltInitializer {

    override suspend fun create(context: Context) {
        println("FastInitializer1: pre-delay")
        delay(2_000)
        println("FastInitializer1: post-delay")
    }

    override fun dependencies(): List<Class<out BoltInitializer>> {
        return emptyList()
    }
}