package com.example.fps_raytrace.engine.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

fun readPpmImage(context: Context, path: Int): IntArray {
    return context.resources.openRawResource(path).use {
        val reader = BufferedReader(InputStreamReader(it))

        // Read PPM header
        val format = readNonCommentLine(reader)  // P3 or P6
        if (format != "P3") {
            throw IllegalArgumentException("Unsupported PPM format: $format")
        }

        // Read image size
        val sizeLine = readNonCommentLine(reader)
        val size = sizeLine.split(" ")
        val width = size[0].toInt()
        val height = size[1].toInt()

        // Read max color value (e.g., 255)
        val maxColorValue = readNonCommentLine(reader).toInt()

        // Initialize array to store the pixel data (RGB)
        val array = IntArray(width * height * 3)

        // Read pixel data
        for (i in array.indices) {
            array[i] = readNonCommentLine(reader).toInt()
        }

        array
    }
}


fun readPpmImageTo2DArray(context: Context, path: Int): Array<Array<Int>> {
    return context.resources.openRawResource(path).use {
        val reader = BufferedReader(InputStreamReader(it))

        // Read PPM header
        val format = readNonCommentLine(reader)  // P3 or P6
        if (format != "P3") {
            throw IllegalArgumentException("Unsupported PPM format: $format")
        }

        // Read image size
        val sizeLine = readNonCommentLine(reader)
        val size = sizeLine.split(" ")
        val width = size[0].toInt()
        val height = size[1].toInt()

        // Read max color value (e.g., 255)
        val maxColorValue = readNonCommentLine(reader).toInt()

        // Initialize array to store the pixel data (RGB)
        val array = Array(height) { Array(width) { 0 } }

        // Read pixel data
        // Merge RGB values into a single integer
        for (i in 0 until height) {
            for (j in 0 until width) {
                val r = readNonCommentLine(reader).toInt()
                val g = readNonCommentLine(reader).toInt()
                val b = readNonCommentLine(reader).toInt()
                array[i][j] = (r shl 16) or (g shl 8) or b
            }
        }
        array
    }
}

fun Array<Array<Int>>.getPixel(x: Int, y: Int): Int {
    return this[y][x]
}

fun Array<Array<Int>>.getXSize(): Int {
    return this[0].size
}

fun Array<Array<Int>>.getYSize(): Int {
    return this.size
}

fun Int.toRGB(): Array<Int> {
    val r = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val b = this and 0xFF
    return arrayOf(r, g, b)
}

fun Array<Int>.darkenColor(intensity: Float): Array<Int> {
    return this.map { it.darkenColor(intensity) }.toTypedArray()
}

private fun readNonCommentLine(reader: BufferedReader): String {
    var line: String
    do {
        line = reader.readLine().trim()
    } while (line.startsWith("#") || line.isEmpty())
    return line
}
