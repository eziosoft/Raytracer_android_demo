package com.example.fps_raytrace.maps

import com.example.fps_raytrace.engine.Player
import com.example.fps_raytrace.engine.PlayerState
import com.example.fps_raytrace.engine.utils.Sound

interface Map {
    val MAP_X: Int
    val MAP_Y: Int
    val MAP: IntArray
}

// 1,2,3 - wall
// 9 - door
// 8 - exit
// 0 - empty
// -1 - player
// -2 - guard

enum class MapObject(val id: Int) {
    guard(-2),
    player(-1),
    empty(0),
    wall1(1),
    wall2(2),
    wall3(3),
}


fun Map.getEnemiesFromMap(
    cellSize: Int,
    state: PlayerState = PlayerState.WALKING,
    sound: Sound
): List<Player> {
    val enemies = mutableListOf<Player>()
    this.MAP.forEachIndexed { index, value ->
        if (value == -2) {
            val position = findPositionBasedOnMapIndex(this.MAP_X, this.MAP_Y, cellSize, index)
            enemies.add(
                Player(
                    x = position[0],
                    y = position[1],
                    rotationRad = 0f,
                    state = state,
                    isMainPlayer = false,
                    sound = sound
                )
            )
        }
    }
    return enemies
}

fun Map.convertMapTo2DArray(): Array<IntArray> {
    return Array(this.MAP_Y) { y ->
        IntArray(this.MAP_X) { x ->
            this.MAP[y * this.MAP_X + x]
        }
    }
}

fun Map.convertMapTo2DArrayForA_Star(): Array<Array<Int>> {
    return Array(this.MAP_Y) { y ->
        Array(this.MAP_X) { x ->
            when (this.MAP[y * this.MAP_X + x]) {
                0 -> 0
                1, 2, 3 -> 1
                else -> 0
            }
        }
    }
}

fun Pair<Int, Int>.findPositionFromArrayIndexes( cellSize: Int): Pair<Float,Float> {
    val x = this.second * cellSize + cellSize / 2f
    val y = this.first * cellSize + cellSize / 2f
    return Pair(x, y)
}

fun findArrayIndexesFromPosition(x: Float, y: Float, cellSize: Int): Pair<Int, Int> {
    val arrayY = (x / cellSize).toInt()
    val arrayX = (y / cellSize).toInt()
    return Pair(arrayX, arrayY)
}


fun Array<IntArray>.printMap(): String {
    var pmap = ""
    for (i in this.indices) {
        for (j in this[i].indices) {
            pmap = pmap.plus(
                when (this[i][j]) {
                    0 -> " " // empty
                    1, 2, 3 -> "\u2588" // wall
                    8 -> "E" // exit
                    9 -> "\u25A1" // door
                    100 -> "S" // start
                    101 -> "E" // end
                    99 -> "P" // path
                    else -> " "
                }
            )
        }
        pmap = pmap.plus("\n")
    }

    return pmap
}

fun Array<Array<Int>>.printMap(): String {
    var pmap = ""
    for (i in this.indices) {
        for (j in this[i].indices) {
            pmap = pmap.plus(
                when (this[i][j]) {
                    0 -> " " // empty
                    1 -> "\u2588" // wall
                    100 -> "S" // start
                    101 -> "E" // end
                    99 -> "P" // path
                    else -> " "
                }
            )
        }
        pmap = pmap.plus("\n")
    }

    return pmap
}

fun findPositionBasedOnMapIndex(mapX: Int, mapY: Int, cellSize: Int, index: Int): Array<Float> {
    val x = (index % mapX) * cellSize + cellSize / 2f
    val y = (index / mapY) * cellSize + cellSize / 2f
    return arrayOf(x, y)
}