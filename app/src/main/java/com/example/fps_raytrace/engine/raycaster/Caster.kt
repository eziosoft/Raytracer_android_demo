package com.example.fps_raytrace.engine.raycaster

import com.example.fps_raytrace.engine.Player
import com.example.fps_raytrace.engine.utils.Screen
import com.example.fps_raytrace.engine.utils.darkenColor
import com.example.fps_raytrace.textures.Walls
import kotlin.math.floor
import kotlin.math.sqrt

fun renderSurface(
    rowRange: IntRange,
    player: Player,
    rayDirectionX: Float,
    rayDirectionY: Float,
    texture: IntArray,
    screen: Screen,
    screenColumn: Int,
    isCeiling: Boolean,
    height: Int,
    cellSize: Int
) {
    val textureSize = sqrt((texture.size / 3).toDouble()).toInt()
    val textureSizeMask = textureSize - 1 // For fast modulo with power-of-2 textures

    // Pre-calculate constants
    val heightFloat = height.toFloat()
    val cellSizeInv = 1.0f / cellSize
    val playerWorldX = player.x * cellSizeInv
    val playerWorldY = player.y * cellSizeInv

    // Pre-calculate darkening factor (0.5f corresponds to multiplying by 128)
    val darkenShift = 1 // Equivalent to multiplying by 0.5

    for (screenY in rowRange) {
        // Optimized distance calculation
        val distance = if (isCeiling) {
            heightFloat / (height - (screenY shl 1)) // Use bit shift for *2
        } else {
            heightFloat / ((screenY shl 1) - height)
        }

        // Calculate world coordinates with pre-computed values
        val worldX = playerWorldX + distance * rayDirectionX
        val worldY = playerWorldY + distance * rayDirectionY

        // Fast texture coordinate calculation using bit operations where possible
        val worldXFrac = worldX - worldX.toInt() // Faster than floor for positive values
        val worldYFrac = worldY - worldY.toInt()

        val textureX = (worldXFrac * textureSize).toInt() and textureSizeMask
        val textureY = (worldYFrac * textureSize).toInt() and textureSizeMask
        val textureIndex = (textureY * textureSize + textureX) * 3

        // Direct array access and optimized darkening
        val baseIndex = textureIndex
        val red = texture[baseIndex] shr darkenShift
        val green = texture[baseIndex + 1] shr darkenShift
        val blue = texture[baseIndex + 2] shr darkenShift

        // Pre-calculate depth to avoid function call overhead
        val depthValue = if (distance >= 10f) 255 else (distance * 25.5f).toInt()

        screen.setRGB(
            screenColumn,
            screenY,
            red,
            green,
            blue,
            depth = depthValue
        )
    }
}

fun castWallColumn(
    textureIndex: Int,
    wallSide: Int,
    player: Player,
    wallDistance: Float,
    rayDirectionY: Float,
    rayDirectionX: Float,
    columnHeight: Int,
    drawStartY: Int,
    drawEndY: Int,
    screen: Screen,
    screenColumn: Int,
    walls: Walls,
    cellSize: Int,
    height: Int,
) {
    val wallTexture = walls.wallTextures[textureIndex]
        ?: error("Wall texture not found for index $textureIndex")
    val textureSize = sqrt((wallTexture.size / 3).toDouble()).toInt()

    val wallHitPosition = if (wallSide == 0) {
        player.y / cellSize + wallDistance * rayDirectionY
    } else {
        player.x / cellSize + wallDistance * rayDirectionX
    }
    val wallHitOffset = wallHitPosition - floor(wallHitPosition)

    val textureX =
        calculateTextureX(wallSide, rayDirectionX, rayDirectionY, wallHitOffset, textureSize)

    drawWallColumn(
        wallTexture,
        textureX,
        textureSize,
        columnHeight,
        drawStartY,
        drawEndY,
        screen,
        screenColumn,
        wallDistance,
        wallSide,
        height
    )
}

private fun calculateTextureX(
    wallSide: Int,
    rayDirectionX: Float,
    rayDirectionY: Float,
    wallHitOffset: Float,
    textureSize: Int
): Int {
    var textureX = ((wallHitOffset * textureSize).toInt()) % textureSize
    if ((wallSide == 0 && rayDirectionX > 0) || (wallSide == 1 && rayDirectionY < 0)) {
        textureX = textureSize - textureX - 1
    }
    return textureX
}

private fun drawWallColumn(
    wallTexture: IntArray,
    textureX: Int,
    textureSize: Int,
    columnHeight: Int,
    drawStartY: Int,
    drawEndY: Int,
    screen: Screen,
    screenColumn: Int,
    wallDistance: Float,
    wallSide: Int,
    height: Int
) {
    val textureStep = textureSize.toFloat() / columnHeight
    var texturePosition = (drawStartY - height / 2 + columnHeight / 2) * textureStep

    val lightIntensity = 1.0f - ((wallDistance / 20.0f) + 0.4f * wallSide).coerceAtMost(1f)

    for (screenY in drawStartY until drawEndY) {
        val textureY = (texturePosition.toInt() and (textureSize - 1))
        texturePosition += textureStep

        val textureIndex = (textureY * textureSize * 3) + (textureX * 3)
        val red = wallTexture[textureIndex]
        val green = wallTexture[textureIndex + 1]
        val blue = wallTexture[textureIndex + 2]

        screen.setRGB(
            screenColumn, screenY,
            red.darkenColor(lightIntensity),
            green.darkenColor(lightIntensity),
            blue.darkenColor(lightIntensity),
            depth = ((wallDistance / 10f).coerceIn(0f, 1f) * 255).toInt()
        )
    }
}