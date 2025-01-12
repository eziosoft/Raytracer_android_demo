import com.example.fps_raytrace.maps.Map1
import com.example.fps_raytrace.maps.convertMapTo2DArrayForA_Star
import com.example.fps_raytrace.maps.printMap
import java.util.PriorityQueue

data class Node(val x: Int, val y: Int, var g: Int, var h: Int, var parent: Node? = null) : Comparable<Node> {
    private val f: Int
        get() = g + h

    override fun compareTo(other: Node): Int {
        return this.f - other.f
    }
}

fun aStar(start: Pair<Int, Int>, end: Pair<Int, Int>, grid: Array<Array<Int>>): List<Pair<Int, Int>>? {
    val rows = grid.size
    val cols = grid[0].size

    val openSet = PriorityQueue<Node>()
    val closedSet = mutableSetOf<Node>()

    val startNode = Node(start.first, start.second, 0, heuristic(start, end))
    openSet.add(startNode)

    while (openSet.isNotEmpty()) {
        val current = openSet.poll() ?: return null

        if (current.x == end.first && current.y == end.second) {
            return reconstructPath(current)
        }

        closedSet.add(current)

        for (neighbor in getNeighbors(current, grid, rows, cols)) {
            if (closedSet.any { it.x == neighbor.x && it.y == neighbor.y }) continue

            val tentativeG = current.g + 1

            val openNeighbor = openSet.find { it.x == neighbor.x && it.y == neighbor.y }
            if (openNeighbor == null || tentativeG < neighbor.g) {
                neighbor.g = tentativeG
                neighbor.h = heuristic(Pair(neighbor.x, neighbor.y), end)
                neighbor.parent = current

                if (openNeighbor == null) {
                    openSet.add(neighbor)
                }
            }
        }
    }

    return null // No path found
}

private fun heuristic(start: Pair<Int, Int>, end: Pair<Int, Int>): Int {
    return Math.abs(start.first - end.first) + Math.abs(start.second - end.second) // Manhattan distance
}

private fun getNeighbors(node: Node, grid: Array<Array<Int>>, rows: Int, cols: Int): List<Node> {
    val neighbors = mutableListOf<Node>()
    val directions = listOf(Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0))

    for (dir in directions) {
        val newX = node.x + dir.first
        val newY = node.y + dir.second

        if (newX in 0 until rows && newY in 0 until cols && grid[newX][newY] == 0) {
            neighbors.add(Node(newX, newY, 0, 0))
        }
    }

    return neighbors
}

private fun reconstructPath(node: Node): List<Pair<Int, Int>> {
    val path = mutableListOf<Pair<Int, Int>>()
    var current: Node? = node

    while (current != null) {
        path.add(Pair(current.x, current.y))
        current = current.parent
    }

    return path.reversed()
}

// Example usage
fun main() {
    val map = Map1.convertMapTo2DArrayForA_Star()

    val start = Pair(55, 29)
    val end = Pair(42, 22)

    val path = aStar(start, end, map)

    path?.forEach {
        map[it.first][it.second] = 99
    }

    map[start.first][start.second] = 100
    map[end.first][end.second] = 101

    println(map.printMap())

    if (path != null) {
        println("Path found: $path")
    } else {
        println("No path found")
    }
}
