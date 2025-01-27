package com.example.fps_raytrace.engine.utils

import kotlin.math.abs

class Screen(val width: Int, val height: Int) {
    val bitmap = ByteArray(width * height * 4)
    private val depthMap = IntArray(width * height)

    init {
        clear()
    }


    fun clear(red: Int = 0, green: Int = 0, blue: Int = 0) {
        for (i in 0 until width) {
            for (j in 0 until height) {
                setRGB(i, j, red, green, blue, 0)
            }
        }

        for (i in depthMap.indices) {
            depthMap[i] = 0
        }
    }

    fun setRGB(x: Int, y: Int, red: Int, green: Int, blue: Int, depth: Int, alpha: Int = 0xFF) {
        if (x < 0 || x >= width || y < 0 || y >= height) return

        val offset = (y * width + x) * 4
        bitmap[offset] = red.toByte()
        bitmap[offset + 1] = green.toByte()
        bitmap[offset + 2] = blue.toByte()
        bitmap[offset + 3] = alpha.toByte()

        depthMap[y * width + x] = depth
    }

    fun setRGB(x: Int, y: Int, color: IntArray, depth: Int = 0) {
        if (x < 0 || x >= width || y < 0 || y >= height) return

        val offset = (y * width + x) * 4
        bitmap[offset] = color[0].toByte()
        bitmap[offset + 1] = color[1].toByte()
        bitmap[offset + 2] = color[2].toByte()
        bitmap[offset + 3] = 0xFF.toByte()

        depthMap[y * width + x] = depth
    }

    fun getRGB(x: Int, y: Int): IntArray {
        val offset = (y * width + x) * 4
        return intArrayOf(
            bitmap[offset].toInt() and 0xFF,
            bitmap[offset + 1].toInt() and 0xFF,
            bitmap[offset + 2].toInt() and 0xFF
        )
    }

    fun getByteArray(): ByteArray {
        return bitmap
    }

    fun getDepth(x: Int, y: Int): Int {
        return depthMap[y * width + x]
    }

    fun setDepth(x: Int, y: Int, depth: Int) {
        depthMap[y * width + x] = depth
    }

    fun getDepthBitmap(): IntArray {
        return depthMap
    }

    fun drawFilledRect(x: Int, y: Int, w: Int, h: Int, red: Int, green: Int, blue: Int, depth: Int = 0) {
        for (i in x until x + w) {
            for (j in y until y + h) {
                setRGB(i, j, red, green, blue, depth = depth)
            }
        }
    }

    fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, red: Int, green: Int, blue: Int, depth: Int = 0) {
        var x = x1
        var y = y1
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1

        var err = dx - dy

        // Pre-calculate to avoid recomputation inside the loop
        val dx2 = dx shl 1   // Equivalent to 2 * dx
        val dy2 = dy shl 1   // Equivalent to 2 * dy

        while (true) {
            setRGB(x, y, red, green, blue, depth = depth)

            if (x == x2 && y == y2) break

            val e2 = err

            if (e2 > -dy) {
                err -= dy2
                x += sx
            }
            if (e2 < dx) {
                err += dx2
                y += sy
            }
        }
    }


    /***
     * Bitmap is in form of IntArray. Draws bitmap. Used to draw gun.
     */
    fun drawBitmap(
        bitmap: IntArray,
        x: Int,
        y: Int,
        bitmapSizeX: Int,
        bitmapSizeY: Int,
        transparentColor: Color,
        depth: Int = 0
    ) {
        var offset = 0
        for (j in 0 until bitmapSizeY) {
            for (i in 0 until bitmapSizeX) {
                val r = bitmap[offset]
                val g = bitmap[offset + 1]
                val b = bitmap[offset + 2]
                if (r != transparentColor.red || g != transparentColor.green || b != transparentColor.blue) {
                    setRGB(x + i, y + j, r, g, b, depth = depth)
                }
                offset += 3
            }
        }
    }


    data class Color(var red: Int, var green: Int, var blue: Int)
}


fun Int.darkenColor(intensity: Float) = (this * intensity).toInt().coerceIn(0, 255)
fun IntArray.darkenColor(intensity: Float) = intArrayOf(
    this[0].darkenColor(intensity),
    this[1].darkenColor(intensity),
    this[2].darkenColor(intensity)
)

fun isTransparent(color: IntArray, transparentColor: Screen.Color): Boolean {
    return color[0] == transparentColor.red && color[1] == transparentColor.green && color[2] == transparentColor.blue
}







