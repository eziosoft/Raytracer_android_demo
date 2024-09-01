package engine

import kotlin.math.abs

class Screen(val w: Int, val h: Int) {
    private val bitmap = ByteArray(w * h * 4)

    init {
        clear()
    }


    fun clear(red: Int = 0, green: Int = 0, blue: Int = 0) {
        for (i in 0 until w) {
            for (j in 0 until h) {
                setRGB(i, j, red, green, blue)
            }
        }
    }

    fun setRGB(x: Int, y: Int, red: Int, green: Int, blue: Int, alpha: Int = 0xFF) {
        if (x < 0 || x >= w || y < 0 || y >= h) return

        val offset = (y * w + x) * 4
        bitmap[offset] = red.toByte()
        bitmap[offset + 1] = green.toByte()
        bitmap[offset + 2] = blue.toByte()
        bitmap[offset + 3] = alpha.toByte()
    }

    fun getByteArray(): ByteArray {
        return bitmap
    }

    fun drawFilledRect(x: Int, y: Int, w: Int, h: Int, red: Int, green: Int, blue: Int) {
        for (i in x until x + w) {
            for (j in y until y + h) {
                setRGB(i, j, red, green, blue)
            }
        }
    }

    fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, red: Int, green: Int, blue: Int) {
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
            setRGB(x, y, red, green, blue)

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
        transparentColor: Color
    ) {
        var offset = 0
        for (j in 0 until bitmapSizeY) {
            for (i in 0 until bitmapSizeX) {
                val r = bitmap[offset]
                val g = bitmap[offset + 1]
                val b = bitmap[offset + 2]
                if (r != transparentColor.red || g != transparentColor.green || b != transparentColor.blue) {
                    setRGB(x + i, y + j, r, g, b)
                }
                offset += 3
            }
        }
    }


    data class Color(var red: Int, var green: Int, var blue: Int)
}


fun Int.darkenColor(intensity: Float) = (this * intensity).toInt().coerceIn(0, 255)







