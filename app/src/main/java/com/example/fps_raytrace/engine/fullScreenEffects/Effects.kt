package com.example.fps_raytrace.engine.fullScreenEffects

import android.util.Log
import com.example.fps_raytrace.engine.utils.Screen

//fun Screen.waveEffect(): Screen {
//    val wave = 2
//    for (i in 0 until width) {
//        for (j in 0 until height) {
//            val x = (i + (sin(j.toDouble() / wave/10) * wave).toInt()) % width
//            val y = (j + (cos(i.toDouble() / wave/10) * wave).toInt()) % height
//
//            if (x < 0 || x >= width || y < 0 || y >= height) continue
//
//            val color = getRGB(x, y)
//            setRGB(i, j, color[0], color[1], color[2])
//        }
//    }
//
//    return this
//}

fun Screen.blurEffect(): Screen {
    val radius = 2
    val size = radius * 2 + 1
    val weight = 1.0 / (size * size)

    val temp = ByteArray(bitmap.size)
    for (i in 0 until width) {
        for (j in 0 until height) {
            var r = 0
            var g = 0
            var b = 0
            for (k in -radius..radius) {
                for (l in -radius..radius) {
                    val x = (i + k).coerceIn(0, width - 1)
                    val y = (j + l).coerceIn(0, height - 1)
                    val color = getRGB(x, y)
                    r += color[0]
                    g += color[1]
                    b += color[2]
                }
            }

            temp[(j * width + i) * 4] = (r * weight).toInt().toByte()
            temp[(j * width + i) * 4 + 1] = (g * weight).toInt().toByte()
            temp[(j * width + i) * 4 + 2] = (b * weight).toInt().toByte()
            temp[(j * width + i) * 4 + 3] = 0xFF.toByte()
        }
    }
    bitmap.indices.forEach { bitmap[it] = temp[it] }

    return this
}


fun Screen.blurBasedOnDepth(): Screen {
    val radius = 2
    val size = radius * 2 + 1

    val temp = ByteArray(bitmap.size)
    for (i in 0 until width) {
        for (j in 0 until height) {
            var r = 0
            var g = 0
            var b = 0
            var totalWeight = 0.0
            for (k in -radius..radius) {
                for (l in -radius..radius) {
                    val x = (i + k).coerceIn(0, width - 1)
                    val y = (j + l).coerceIn(0, height - 1)
                    val color = getRGB(x, y)
                    val depth = getDepth(x, y)
                    val weight = 1.0 / (1 + depth)
                    r += (color[0] * weight).toInt()
                    g += (color[1] * weight).toInt()
                    b += (color[2] * weight).toInt()
                    totalWeight += weight
                }
            }

            temp[(j * width + i) * 4] = (r / totalWeight).toInt().toByte()
            temp[(j * width + i) * 4 + 1] = (g / totalWeight).toInt().toByte()
            temp[(j * width + i) * 4 + 2] = (b / totalWeight).toInt().toByte()
            temp[(j * width + i) * 4 + 3] = 0xFF.toByte()
        }
    }
    bitmap.indices.forEach { bitmap[it] = temp[it] }

    return this
}

fun Screen.depthMap():Screen{
    val temp = ByteArray(bitmap.size)
    for (i in 0 until width) {
        for (j in 0 until height) {
            val depth = (getDepth(i, j)/10f).coerceIn(0f, 1f)
//            Log.d("aaa", "depthMap: $depth")
            val color = 255-(depth * 255).toInt()
            temp[(j * width + i) * 4] = color.toByte()
            temp[(j * width + i) * 4 + 1] = color.toByte()
            temp[(j * width + i) * 4 + 2] = color.toByte()
            temp[(j * width + i) * 4 + 3] = 0xFF.toByte()
        }
    }
    bitmap.indices.forEach { bitmap[it] = temp[it] }

    return this
}