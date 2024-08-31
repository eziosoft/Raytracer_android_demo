package engine

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.min

class Screen(val w: Int, val h: Int, color: Color = Color(0, 0, 0)) {
    private val bitmap = Array(w * h) { color }

    private val outputBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    private val pixels = IntArray(w * h)
    private val cpuCores = Runtime.getRuntime().availableProcessors()

    fun clear() {
        for (i in 0 until w) {
            for (j in 0 until h) {
                setRGB(i, j, Color(0, 0, 0))
            }
        }
    }

    fun setRGB(x: Int, y: Int, color: Color) {
        if (x < 0 || x >= w || y < 0 || y >= h) return

        bitmap[y * w + x] = color
    }

    private fun getRGB(x: Int, y: Int): Color {
        return bitmap[y * w + x]
    }


    fun getBitmap(): Bitmap {
        val threadPool = Executors.newFixedThreadPool(cpuCores)
        val chunkSize = h / cpuCores
        try {
            runBlocking {
                val jobs = (0 until cpuCores).map { core ->
                    async(threadPool.asCoroutineDispatcher()) {
                        val startRow = core * chunkSize
                        val endRow = min(startRow + chunkSize, h)
                        for (j in startRow until endRow) {
                            val rowOffset = j * w
                            for (i in 0 until w) {
                                pixels[rowOffset + i] = bitmap[rowOffset + i].toRgb()
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }
        } finally {
            threadPool.shutdown()
        }

        outputBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return outputBitmap
    }

    fun drawFilledRect(x: Int, y: Int, w: Int, h: Int, color: Color) {
        for (i in x until x + w) {
            for (j in y until y + h) {
                setRGB(i, j, color)
            }
        }
    }

    fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: Color) {
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        var err = dx - dy

        var x = x1
        var y = y1

        while (true) {
            setRGB(x, y, color)

            if (x == x2 && y == y2) break

            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
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
        transparentColor: Color
    ) {
        val transparentRed = transparentColor.red
        val transparentGreen = transparentColor.green
        val transparentBlue = transparentColor.blue

        var offset = 0
        for (j in 0 until bitmapSizeY) {
            for (i in 0 until bitmapSizeX) {
                val r = bitmap[offset]
                val g = bitmap[offset + 1]
                val b = bitmap[offset + 2]
                if (r != transparentRed || g != transparentGreen || b != transparentBlue) {
                    setRGB(x + i, y + j, Color(r, g, b))
                }
                offset += 3
            }
        }
    }


    data class Color(var red: Int, var green: Int, var blue: Int) {
        fun toArgb(): Int {
            return (red shl 16) or (green shl 8) or blue
        }

        fun toRgb(): Int {
            return android.graphics.Color.rgb(red, green, blue)
        }

        // Helper function to darken a color based on intensity used for shading
        fun darken(intensity: Float) {
            red = (red * intensity).toInt().coerceIn(0, 255)
            green = (green * intensity).toInt().coerceIn(0, 255)
            blue = (blue * intensity).toInt().coerceIn(0, 255)
        }
    }
}







