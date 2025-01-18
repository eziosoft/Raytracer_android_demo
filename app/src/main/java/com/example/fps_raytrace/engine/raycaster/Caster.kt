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

    for (screenY in rowRange) {
        val distance = if (isCeiling) {
            height.toFloat() / (height - 2.0f * screenY)
        } else {
            height.toFloat() / (2.0f * screenY - height)
        }

        val worldX = player.x / cellSize + distance * rayDirectionX
        val worldY = player.y / cellSize + distance * rayDirectionY

        val textureX = (((worldX - floor(worldX)) * textureSize) % textureSize).toInt()
        val textureY = (((worldY - floor(worldY)) * textureSize) % textureSize).toInt()
        val textureIndex = (textureY * textureSize + textureX) * 3

        val red = texture[textureIndex]
        val green = texture[textureIndex + 1]
        val blue = texture[textureIndex + 2]

        screen.setRGB(
            screenColumn,
            screenY,
            red.darkenColor(0.5f),
            green.darkenColor(0.5f),
            blue.darkenColor(0.5f)
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
            blue.darkenColor(lightIntensity)
        )
    }
}