package com.example.fps_raytrace.engine.raycaster

import com.example.fps_raytrace.engine.utils.Screen
import kotlin.math.cos
import kotlin.math.sin

fun drawCross(screen: Screen, screenWidth: Int, screenHeight: Int) {
    val crossSize = 10
    val x = screenWidth / 2 - crossSize / 2
    val y = screenHeight / 2 - crossSize / 2

    screen.drawLine(
        x,
        y,
        x + crossSize,
        y + crossSize,
        255,
        255,
        255
    )
    screen.drawLine(
        x + crossSize,
        y,
        x,
        y + crossSize,
        255,
        255,
        255
    )
}

fun getTexturePixelColor(texture: IntArray, index: Int): IntArray {
    val red = texture[index]
    val green = texture[index + 1]
    val blue = texture[index + 2]
    return intArrayOf(red, green, blue)
}


// Much faster cached trigonometric values - using radians directly
private const val CACHE_SIZE_RAD = 8192 // Power of 2 for faster modulo
private const val CACHE_MASK = CACHE_SIZE_RAD - 1 // Bit mask for modulo
private const val RAD_TO_INDEX = CACHE_SIZE_RAD / (2 * Math.PI).toFloat()
private val fastSinCache = FloatArray(CACHE_SIZE_RAD) {
    sin((it * 2 * Math.PI / CACHE_SIZE_RAD)).toFloat()
}
private val fastCosCache = FloatArray(CACHE_SIZE_RAD) {
    cos((it * 2 * Math.PI / CACHE_SIZE_RAD)).toFloat()
}

// Much faster helper functions - no degree conversion, no interpolation
fun fastSin(angleRad: Float): Float {
    // Normalize angle to positive range and convert to index
    val normalizedAngle = if (angleRad < 0) angleRad + (2 * Math.PI).toFloat() else angleRad
    val index = (normalizedAngle * RAD_TO_INDEX).toInt() and CACHE_MASK
    return fastSinCache[index]
}

fun fastCos(angleRad: Float): Float {
    // Normalize angle to positive range and convert to index
    val normalizedAngle = if (angleRad < 0) angleRad + (2 * Math.PI).toFloat() else angleRad
    val index = (normalizedAngle * RAD_TO_INDEX).toInt() and CACHE_MASK
    return fastCosCache[index]
}