package com.example.fps_raytrace.sprites

import android.content.Context
import com.example.fps_raytrace.R
import com.example.fps_raytrace.engine.utils.Screen
import com.example.fps_raytrace.engine.utils.readPpmImage

class OtherSprites(context: Context) {

    private val spriteSheet = readPpmImage(context, R.raw.sprites1)
    val transparentColor = Screen.Color(167, 107, 107)

    val light = getSubArray(88, 80, 56, 56, 320)


    // Main array is unknown size, so we need to pass the width and height
    private fun getSubArray(x: Int, y: Int, width: Int, height: Int, inputArrayWidth: Int): IntArray {
        val subArray = IntArray(width * height * 3)
        for (i in 0 until height) {
            for (j in 0 until width) {
                val index = (y + i) * inputArrayWidth * 3 + (x + j) * 3
                val subIndex = i * width * 3 + j * 3
                subArray[subIndex] = spriteSheet[index]
                subArray[subIndex + 1] = spriteSheet[index + 1]
                subArray[subIndex + 2] = spriteSheet[index + 2]
            }
        }
        return subArray
    }


}