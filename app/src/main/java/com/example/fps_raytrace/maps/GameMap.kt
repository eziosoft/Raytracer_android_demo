package com.example.fps_raytrace.maps

import com.example.fps_raytrace.engine.Player
import com.example.fps_raytrace.engine.PlayerState
import com.example.fps_raytrace.engine.utils.Sound

interface GameMap {
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


fun GameMap.getEnemiesFromMap(
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

fun GameMap.convertMapTo2DArray(): Array<IntArray> {
    return Array(this.MAP_Y) { y ->
        IntArray(this.MAP_X) { x ->
            this.MAP[y * this.MAP_X + x]
        }
    }
}

fun GameMap.convertMapTo2DArrayForA_Star(): Array<Array<Int>> {
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

fun Array<Array<Int>>.printMap(playerX: Int, playerY: Int): String {
    // Determine the width of each cell and the row number width
    val cellWidth = 3 // Ensure a minimum width of 3 for proper spacing
    val rowNumberWidth = this.size.toString().length // Width of row numbers

    // Create the grid header with row and column numbers
    val columnHeader = " ".repeat(rowNumberWidth + 1) + // Padding for row numbers
            (0 until this[0].size).joinToString("") { it.toString().padStart(cellWidth, ' ') } + "\n"

    val gridString = StringBuilder(columnHeader)

    // Generate each row with aligned row numbers
    for (i in this.indices) {
        gridString.append(i.toString().padStart(rowNumberWidth, ' ')).append(" ") // Align row number
        for (j in this[i].indices) {
            gridString.append(
                when {
                    i == playerY && j == playerX -> "P".padStart(cellWidth, ' ') // Player position
                    this[i][j] == 0 -> " ".padStart(cellWidth, ' ') // Empty space
                    this[i][j] == 1 -> "\u2588".padStart(cellWidth, ' ') // Wall
                    this[i][j] == 100 -> "P".padStart(cellWidth, ' ') // Start
                    this[i][j] == 101 -> "E".padStart(cellWidth, ' ') // End
                    this[i][j] == 99 -> " ".padStart(cellWidth, ' ') // Path (empty space)
                    else -> " ".padStart(cellWidth, ' ') // Default empty
                }
            )
        }
        gridString.append("\n")
    }

    // Append the legend at the bottom
    gridString.append("\nLegend:\n")
    gridString.append("P = Player/Start\n")
    gridString.append("E = End\n")
    gridString.append("\u2588 = Wall\n")
    gridString.append("  = Empty space\n")

    return gridString.toString()
}


fun Array<Array<Int>>.toJsonMap(): String {
    // Initialize a map to store the grid data
    val gridData = mutableListOf<Map<String, Any>>()

    // Iterate through each cell in the grid
    for (i in this.indices) {
        for (j in this[i].indices) {
            // Create a map for each cell with row, column, and value
            val cellData = mutableMapOf<String, Any>(
                "row" to i,
                "column" to j,
                "value" to when (this[i][j]) {
                    0 -> " " // Empty cell
                    1 -> "WALL" // Wall
                    100 -> "START" // Start
                    101 -> "END" // End
                    99 -> "PATH" // Path
                    else -> "UNKNOWN" // Default empty
                }
            )
            // Add the cell data to the grid
            gridData.add(cellData)
        }
    }

    // Convert the grid data into JSON format
    return """
        {
            "grid": $gridData
        }
    """.trimIndent()
}






fun findPositionBasedOnMapIndex(mapX: Int, mapY: Int, cellSize: Int, index: Int): Array<Float> {
    val x = (index % mapX) * cellSize + cellSize / 2f
    val y = (index / mapY) * cellSize + cellSize / 2f
    return arrayOf(x, y)
}