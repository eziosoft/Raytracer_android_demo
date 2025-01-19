package com.example.fps_raytrace.engine.raycaster

import com.example.fps_raytrace.engine.Player
import com.example.fps_raytrace.engine.utils.PI
import com.example.fps_raytrace.engine.utils.Screen
import com.example.fps_raytrace.engine.utils.Sprite
import com.example.fps_raytrace.engine.utils.darkenColor
import com.example.fps_raytrace.engine.utils.isTransparent
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

fun drawEnemy(
    screen: Screen,
    cellSize: Int,
    fovRad: Float,
    player: Player,
    enemy: Player,
    textureSet: Sprite,
    wallDepths: FloatArray
) {
    // Calculate the angle from the enemy to the player
    val angleToPlayer = atan2(player.y - enemy.y, player.x - enemy.x)

    // Calculate the difference between the enemy's rotation and the angle to the player
    var diff = angleToPlayer - enemy.rotationRad

    // Normalize the angle difference to be between -PI and PI
    diff = (diff + PI) % (2 * PI) - PI

    // Calculate the texture index, ensuring it falls within the valid range
    val numTextures = 8  // Assuming there are 8 textures in the textureSet
    val textureIndex =
        ((numTextures - ((diff / (2 * PI) * numTextures).toInt() % numTextures)) + numTextures) % numTextures


    // Fetch the correct texture for rendering
    val texture = textureSet.getTexture(
        direction = textureIndex,
        state = enemy.state,
        walkingFrame = enemy.walkingFrame,
        dyingFrame = enemy.dyingFrame,
        shootingFrame = enemy.shootingFrame
    )

    val pointHeight = cellSize // Height of the square in world units

    // Calculate vector from player to enemy
    val dx = enemy.x - player.x
    val dy = enemy.y - player.y

    // Calculate distance to enemy
    val distance = sqrt(dx * dx + dy * dy)

    // Calculate angle to enemy relative to player's rotation
    var angle = atan2(dy, dx) - player.rotationRad

    // Normalize angle to be between -PI and PI
    angle = (angle + PI) % (2 * PI) - PI

    // Check if enemy is within player's FOV
    if (abs(angle) < fovRad / 2) {
        // Calculate screen x-coordinate
        val screenX = ((screen.width / 2) * (1 + angle / (fovRad / 2))).toInt()

        // Calculate perceived height of the square
        val perceivedHeight = (screen.height / distance * pointHeight).toInt()

        // Calculate top and bottom y-coordinates
        val topY = (screen.height / 2 - perceivedHeight / 2).coerceIn(0, screen.height - 1)
        val bottomY = (screen.height / 2 + perceivedHeight / 2).coerceIn(0, screen.height - 1)

        // Calculate perceived width of the square
        val perceivedWidth = perceivedHeight

        // Draw sprite
        for (y in topY..bottomY) {
            for (x in (screenX - perceivedWidth / 2)..(screenX + perceivedWidth / 2)) {
                if (x >= 0 && x < screen.width) {
                    // Only draw the sprite pixel if it's closer than the wall
                    if (wallDepths[x] - (distance / cellSize) > -0.2) { // -0.1 - padding to avoid wall clipping
                        // Calculate texture coordinates
                        val texX =
                            ((x - (screenX - perceivedWidth / 2)).toFloat() / perceivedWidth * textureSet.SPRITE_SIZE).toInt() % textureSet.SPRITE_SIZE
                        val texY =
                            ((y - topY).toFloat() / perceivedHeight * textureSet.SPRITE_SIZE).toInt() % textureSet.SPRITE_SIZE

                        val texIndex = (texY * textureSet.SPRITE_SIZE + texX) * 3

                        val color = getTexturePixelColor(texture, texIndex)


                        if (!isTransparent(color = color, transparentColor = textureSet.TRANSPARENT_COLOR)) {
                            // Apply distance-based shading
                            val intensity = (1.0f - (distance / 30.0f)).coerceIn(0.2f, 1f)

                            screen.setRGB(
                                x = x,
                                y = y,
                                color = color.darkenColor(intensity)
                            )
                        }
                    }
                }
            }
        }
    }
}