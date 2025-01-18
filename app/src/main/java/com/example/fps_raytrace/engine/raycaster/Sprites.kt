package com.example.fps_raytrace.engine.raycaster

import androidx.compose.ui.graphics.Color
import com.example.fps_raytrace.engine.Player
import com.example.fps_raytrace.engine.utils.PI
import com.example.fps_raytrace.engine.utils.Screen
import com.example.fps_raytrace.engine.utils.Sprite
import com.example.fps_raytrace.engine.utils.darkenColor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

fun drawEnemy(
    screen: Screen,
    player: Player,
    enemy: Player,
    wallDepths: FloatArray,
    textureSet: Sprite,
    cellSize: Int,
    screenWidth: Int,
    screenHeight: Int,
    fovRad: Float
) {
    val angleToPlayer = atan2(player.y - enemy.y, player.x - enemy.x)
    val normalizedDiff = ((angleToPlayer - enemy.rotationRad + PI) % (2 * PI)) - PI

    val textureIndex = calculateTextureIndex(normalizedDiff, textureSet.numTextures)
    val texture = textureSet.getTexture(
        direction = textureIndex,
        state = enemy.state,
        walkingFrame = enemy.walkingFrame,
        dyingFrame = enemy.dyingFrame,
        shootingFrame = enemy.shootingFrame
    )

    val dx = enemy.x - player.x
    val dy = enemy.y - player.y
    val distance = sqrt(dx * dx + dy * dy)
    val relativeAngle = normalizeAngle(atan2(dy, dx) - player.rotationRad)

    if (abs(relativeAngle) < fovRad / 2) {
        val screenX = calculateScreenX(relativeAngle, screenWidth, fovRad)
        val perceivedHeight = calculatePerceivedSize(screenHeight, distance, cellSize)
        val topY = calculateTopY(screenHeight, perceivedHeight)
        val bottomY = calculateBottomY(screenHeight, perceivedHeight)
        val perceivedWidth = perceivedHeight

        drawSprite(
            screen,
            texture,
            screenX,
            topY,
            bottomY,
            perceivedWidth,
            wallDepths,
            distance,
            cellSize,
            textureSet
        )
    }
}

private fun calculateTextureIndex(angleDiff: Double, numTextures: Int): Int {
    return ((numTextures - ((angleDiff / (2 * PI) * numTextures).toInt() % numTextures)) + numTextures) % numTextures
}

private fun normalizeAngle(angle: Double): Double {
    return ((angle + PI) % (2 * PI)) - PI
}

private fun calculateScreenX(relativeAngle: Double, screenWidth: Int, fovRad: Float): Int {
    return ((screenWidth / 2) * (1 + relativeAngle / (fovRad / 2))).toInt()
}

private fun calculatePerceivedSize(screenHeight: Int, distance: Float, cellSize: Int): Int {
    return (screenHeight / distance * cellSize).toInt()
}

private fun calculateTopY(screenHeight: Int, perceivedHeight: Int): Int {
    return (screenHeight / 2 - perceivedHeight / 2).coerceIn(0, screenHeight - 1)
}

private fun calculateBottomY(screenHeight: Int, perceivedHeight: Int): Int {
    return (screenHeight / 2 + perceivedHeight / 2).coerceIn(0, screenHeight - 1)
}

private fun drawSprite(
    screen: Screen,
    texture: IntArray,
    screenX: Int,
    topY: Int,
    bottomY: Int,
    perceivedWidth: Int,
    wallDepths: FloatArray,
    distance: Float,
    cellSize: Int,
    textureSet: Sprite
) {
    for (y in topY..bottomY) {
        for (x in (screenX - perceivedWidth / 2)..(screenX + perceivedWidth / 2)) {
            if (x in 0 until wallDepths.size && wallDepths[x] - (distance / cellSize) > -0.1) {
                val texX = calculateTextureCoordinate(x, screenX, perceivedWidth, textureSet.SPRITE_SIZE)
                val texY = calculateTextureCoordinate(y, topY, bottomY - topY + 1, textureSet.SPRITE_SIZE)
                val texIndex = (texY * textureSet.SPRITE_SIZE + texX) * 3

                val r = texture[texIndex]
                val g = texture[texIndex + 1]
                val b = texture[texIndex + 2]

                if (!isTransparent(r, g, b, textureSet.TRANSPARENT_COLOR)) {
                    val intensity = calculateShadingIntensity(distance)
                    screen.setRGB(
                        x, y,
                        r.darkenColor(intensity),
                        g.darkenColor(intensity),
                        b.darkenColor(intensity)
                    )
                }
            }
        }
    }
}

private fun calculateTextureCoordinate(pos: Int, start: Int, size: Int, spriteSize: Int): Int {
    return (((pos - (start - size / 2)).toFloat() / size * spriteSize).toInt() % spriteSize)
}

private fun isTransparent(r: Int, g: Int, b: Int, transparentColor: Color): Boolean {
    return r == transparentColor.red && g == transparentColor.green && b == transparentColor.blue
}

private fun calculateShadingIntensity(distance: Float): Float {
    return (1.0f - (distance / 30.0f)).coerceIn(0.2f, 1f)
}
