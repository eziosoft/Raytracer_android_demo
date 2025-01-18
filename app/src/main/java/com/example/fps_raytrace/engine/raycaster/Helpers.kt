package com.example.fps_raytrace.engine.raycaster

import com.example.fps_raytrace.engine.utils.Screen

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