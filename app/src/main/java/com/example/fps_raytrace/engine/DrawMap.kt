package engine

import maps.Map
import models.Player
import kotlin.math.cos
import kotlin.math.sin

private val filledColor = Screen.Color(255, 255, 255)
private val emptyColor = Screen.Color(0, 0, 0)
private val mapPlayerColor = Screen.Color(0, 255, 255)
private val enemyMapColor = Screen.Color(255, 0, 0)

fun drawMap(
    screen: Screen,
    map: Map,
    xOffset: Int,
    yOffset: Int,
    cellSize: Int,
    player: Player,
    playerSize: Float,
    enemies: List<Player>
) {
    for (y: Int in 0 until map.MAP_Y) {
        for (x in 0 until map.MAP_X) {

            val r = if (map.MAP[y * map.MAP_X + x] > 0) {
                filledColor.red
            } else {
                emptyColor.red
            }

            val g = if (map.MAP[y * map.MAP_X + x] > 0) {
                filledColor.green
            } else {
                emptyColor.green
            }

            val b = if (map.MAP[y * map.MAP_X + x] > 0) {
                filledColor.blue
            } else {
                emptyColor.blue
            }

            screen.drawFilledRect(
                x = xOffset + x * cellSize,
                y = yOffset + y * cellSize,
                w = cellSize,
                h = cellSize,
                r, g, b
            )
        }
    }

    drawPlayerOnMap(
        screen,
        player,
        xOffset = xOffset,
        yOffset = yOffset,
        r = mapPlayerColor.red,
        g = mapPlayerColor.green,
        b = mapPlayerColor.blue,
        playerSize = playerSize
    )

    enemies.forEach { enemy ->
        drawPlayerOnMap(
            screen,
            enemy,
            xOffset = xOffset,
            yOffset = yOffset,
            r = enemyMapColor.red,
            g = enemyMapColor.green,
            b = enemyMapColor.blue,
            playerSize = playerSize
        )
    }
}

private fun drawPlayerOnMap(
    bitmap: Screen,
    player: Player,
    xOffset: Int,
    yOffset: Int,
    r: Int,
    g: Int,
    b: Int,
    playerSize: Float
) {
    bitmap.drawFilledRect(
        xOffset + (player.x - playerSize / 2).toInt(),
        yOffset + (player.y - playerSize / 2).toInt(),
        playerSize.toInt(),
        playerSize.toInt(),
        r, g, b
    )
    bitmap.drawLine(
        xOffset + player.x.toInt(),
        yOffset + player.y.toInt(),
        (xOffset + player.x + cos(player.rotationRad.toDouble()).toFloat() * playerSize * 2).toInt(),
        (yOffset + player.y + sin(player.rotationRad.toDouble()).toFloat() * playerSize * 2).toInt(),
        r, g, b
    )
}
