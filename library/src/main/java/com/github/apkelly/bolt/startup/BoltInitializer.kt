package com.github.apkelly.bolt.startup

import android.content.Context

interface BoltInitializer {
    suspend fun create(context: Context)

    fun dependencies(): List<Class<out BoltInitializer>>
}
