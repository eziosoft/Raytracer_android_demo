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

            screen.drawFilledRect(
                x = xOffset + x * cellSize,
                y = yOffset + y * cellSize,
                w = cellSize,
                h = cellSize,
                color = if (map.MAP[y * map.MAP_X + x] > 0) {
                    filledColor
                } else {
                    emptyColor
                }
            )
        }
    }

    drawPlayerOnMap(
        screen,
        player,
        xOffset = xOffset,
        yOffset = yOffset,
        color = mapPlayerColor,
        playerSize = playerSize
    )

    enemies.forEach { enemy ->
        drawPlayerOnMap(
            screen,
            enemy,
            xOffset = xOffset,
            yOffset = yOffset,
            color = enemyMapColor,
            playerSize = playerSize
        )
    }
}

private fun drawPlayerOnMap(
    bitmap: Screen,
    player: Player,
    xOffset: Int,
    yOffset: Int,
    color: Screen.Color,
    playerSize: Float
) {
    bitmap.drawFilledRect(
        xOffset + (player.x - playerSize / 2).toInt(),
        yOffset + (player.y - playerSize / 2).toInt(),
        playerSize.toInt(),
        playerSize.toInt(),
        color
    )
    bitmap.drawLine(
        xOffset + player.x.toInt(),
        yOffset + player.y.toInt(),
        (xOffset + player.x + cos(player.rotationRad.toDouble()).toFloat() * playerSize * 2).toInt(),
        (yOffset + player.y + sin(player.rotationRad.toDouble()).toFloat() * playerSize * 2).toInt(),
        color
    )
}
