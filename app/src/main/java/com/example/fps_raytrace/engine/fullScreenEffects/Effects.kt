package com.example.fps_raytrace.engine.fullScreenEffects

import com.example.fps_raytrace.engine.utils.Screen
import kotlin.math.cos
import kotlin.math.sin

fun Screen.waveEffect(): Screen {
    val wave = 2
    for (i in 0 until w) {
        for (j in 0 until h) {
            val x = (i + (sin(j.toDouble() / wave/10) * wave).toInt()) % w
            val y = (j + (cos(i.toDouble() / wave/10) * wave).toInt()) % h

            if (x < 0 || x >= w || y < 0 || y >= h) continue

            val color = getRGB(x, y)
            setRGB(i, j, color[0], color[1], color[2])
        }
    }

    return this
}

fun Screen.blurEffect(): Screen {
    val radius = 2
    val size = radius * 2 + 1
    val weight = 1.0 / (size * size)

    val temp = ByteArray(bitmap.size)
    for (i in 0 until w) {
        for (j in 0 until h) {
            var r = 0
            var g = 0
            var b = 0
            for (k in -radius..radius) {
                for (l in -radius..radius) {
                    val x = (i + k).coerceIn(0, w - 1)
                    val y = (j + l).coerceIn(0, h - 1)
                    val color = getRGB(x, y)
                    r += color[0]
                    g += color[1]
                    b += color[2]
                }
            }
            temp[(j * w + i) * 4] = (r * weight).toInt().toByte()
            temp[(j * w + i) * 4 + 1] = (g * weight).toInt().toByte()
            temp[(j * w + i) * 4 + 2] = (b * weight).toInt().toByte()
            temp[(j * w + i) * 4 + 3] = 0xFF.toByte()
        }
    }
    bitmap.indices.forEach { bitmap[it] = temp[it] }

    return this
}