package io.github.apkelly.demo.startup

import android.content.Context
import io.github.apkelly.bolt.startup.BoltInitializer
import kotlinx.coroutines.delay

class SlowInitializer2: BoltInitializer {

    override suspend fun create(context: Context) {
        println("SlowInitializer2: pre-delay")
        delay(5_000)
        println("SlowInitializer2: post-delay")
    }

    override fun dependencies(): List<Class<out BoltInitializer>> {
        return listOf(FastInitializer2::class.java)
    }
}