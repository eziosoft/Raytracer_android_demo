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

        // Read all remaining content and split by whitespace to get individual pixel values
        val remainingContent = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            line?.let {
                if (!it.trim().startsWith("#") && it.trim().isNotEmpty()) {
                    remainingContent.append(it).append(" ")
                }
            }
        }

        // Split by whitespace and parse pixel values
        val pixelValues = remainingContent.toString().trim().split("\\s+".toRegex())

        // Fill the array with pixel data
        for (i in array.indices) {
            if (i < pixelValues.size) {
                array[i] = pixelValues[i].toInt()
            }
        }

        array
    }
}

private fun readNonCommentLine(reader: BufferedReader): String {
    var line: String
    do {
        line = reader.readLine().trim()
    } while (line.startsWith("#") || line.isEmpty())
    return line
}
