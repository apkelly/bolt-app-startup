package io.github.apkelly.demo.startup

import android.content.Context
import io.github.apkelly.bolt.startup.BoltInitializer
import kotlinx.coroutines.delay

class FastInitializer2: BoltInitializer {

    override suspend fun create(context: Context) {
        println("FastInitializer2: pre-delay")
        delay(1_000)
        println("FastInitializer2: post-delay")
    }

    override fun dependencies(): List<Class<out BoltInitializer>> {
        return emptyList()
    }
}