package com.example.fps_raytrace.engine.raycaster

import aStar
import android.content.Context
import com.example.fps_raytrace.R
import com.example.fps_raytrace.engine.Const.AI_RECORD_DATA
import com.example.fps_raytrace.engine.Const.DRAW_MAP
import com.example.fps_raytrace.engine.Const.ENABLE_A_STAR
import com.example.fps_raytrace.engine.Const.LOG_STATS
import com.example.fps_raytrace.engine.Const.MAP_CELL_SIZE
import com.example.fps_raytrace.engine.Const.PLAYER_FOV
import com.example.fps_raytrace.engine.Const.PLAYER_ROTATION_SPEED_RAD
import com.example.fps_raytrace.engine.Const.PLAYER_SPEED
import com.example.fps_raytrace.engine.Const.SHOOT_PLAYER_DAMAGE
import com.example.fps_raytrace.engine.Moves
import com.example.fps_raytrace.engine.Player
import com.example.fps_raytrace.engine.PlayerState
import com.example.fps_raytrace.engine.animate
import com.example.fps_raytrace.engine.distanceTo
import com.example.fps_raytrace.engine.inShotAngle
import com.example.fps_raytrace.engine.map.drawMap
import com.example.fps_raytrace.engine.utils.LogFileHelper
import com.example.fps_raytrace.engine.utils.Screen
import com.example.fps_raytrace.engine.utils.Sound
import com.example.fps_raytrace.engine.utils.WallType
import com.example.fps_raytrace.engine.utils.isWall
import com.example.fps_raytrace.engine.utils.normalizeAngle
import com.example.fps_raytrace.engine.utils.readPpmImage
import com.example.fps_raytrace.engine.utils.toRadian
import com.example.fps_raytrace.maps.GameMap
import com.example.fps_raytrace.maps.GameMap1
import com.example.fps_raytrace.maps.MapObject
import com.example.fps_raytrace.maps.convertMapTo2DArrayForA_Star
import com.example.fps_raytrace.maps.findArrayIndexesFromPosition
import com.example.fps_raytrace.maps.findPositionBasedOnMapIndex
import com.example.fps_raytrace.maps.findPositionFromArrayIndexes
import com.example.fps_raytrace.maps.getEnemiesFromMap
import com.example.fps_raytrace.sprites.GuardSprite
import com.example.fps_raytrace.sprites.OtherSprites
import com.example.fps_raytrace.sprites.PistolSprite
import com.example.fps_raytrace.textures.Walls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class RaytracerEngine(
    context: Context,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val cellSize: Int = MAP_CELL_SIZE
) {
    private val screen = Screen(screenWidth, screenHeight)
    private val sound = Sound(context)

    private var cpuCount: Int = Runtime.getRuntime().availableProcessors()
    private val wallDepths = FloatArray(screenWidth) // depth buffer

    private val currentGameMap: GameMap = GameMap1

    // Textures
    private val wallTextures = Walls(context)
    private val ceilingTexture: IntArray = readPpmImage(context, R.raw.celling)
    private val floorTexture: IntArray = readPpmImage(context, R.raw.floor)

    // Sprites
    private val pistolSprite = PistolSprite(context)
    private val guardSprite = GuardSprite(context)
    private val otherSprites = OtherSprites(context)

    private val path: MutableList<Pair<Float, Float>> = mutableListOf()

    private val logs: LogFileHelper = LogFileHelper(context)

    private val playerPosition = findPositionBasedOnMapIndex(
        mapX = currentGameMap.MAP_X,
        mapY = currentGameMap.MAP_Y,
        cellSize = cellSize,
        index = currentGameMap.MAP.indexOf(MapObject.player.id)
    )

    // create main player
    private val player = Player(
        x = playerPosition[0],
        y = playerPosition[1],
        rotationRad = 0f.toRadian().normalizeAngle(),
        isMainPlayer = true,
        sound = sound,
        state = PlayerState.WALKING
    )

    //create enemies
    private val enemies = currentGameMap.getEnemiesFromMap(
        cellSize = cellSize,
        state = PlayerState.WALKING,
        sound = sound
    )

    fun init() {
        sound.loadSound(R.raw.gunshot1)
        sound.loadSound(R.raw.mandeathscream)
        sound.loadSound(R.raw.step)


    }

    fun playNewMusic(resId: Int, isLooping: Boolean) {
        sound.stopMusic()
        sound.playMusic(resId, isLooping)
    }

    fun gameLoop(pressedKeys: Set<Moves>, effects: (Screen) -> Screen, onFrame: (Screen) -> Unit) {
        enemies.forEach { enemy ->
            enemy.animate(
                gameMap = currentGameMap,
                cellSize = cellSize,
                mainPlayer = player,
                isWallBetween = {
                    isWallBetween(player, enemy)
                })
        }

        player.animate(newState = player.state, gameMap = currentGameMap, cellSize = cellSize)
        movePlayer(pressedKeys)
        onFrame(effects(generateFrame()))
    }


    private fun generateFrame(): Screen {
        // draw 3d
        val castRayTime = measureTime {
            castRays(
                player = player,
                bitmap = screen,
                castWalls = true,
                castFloor = true,
                castCeiling = true
            )
        }

        // draw enemies based on distance
        val drawSpriteTime = measureTime {
            enemies.sortedByDescending { it.distanceTo(player) }.forEach { enemy ->
                drawEnemy(
                    screen = screen,
                    cellSize = cellSize,
                    player = player,
                    enemy = enemy,
                    wallDepths = wallDepths,
                    textureSet = guardSprite,
                    fovRad = PLAYER_FOV
                )
            }
        }

        val drawMapTime = measureTime {
            if (DRAW_MAP) {
                drawMap(
                    screen = screen,
                    gameMap = currentGameMap,
                    xOffset = 10,
                    yOffset = screenHeight - cellSize * currentGameMap.MAP_Y - 10,
                    player = player,
                    enemies = enemies,
                    cellSize = cellSize,
                    playerSize = 5f
                )
            }
        }


        if (ENABLE_A_STAR && System.currentTimeMillis() % 100 == 0L) {
//            Log.d("aaa", "aStar")
            val playerArrayPosition = findArrayIndexesFromPosition(player.x, player.y, cellSize)
            val start = Pair(playerArrayPosition.first, playerArrayPosition.second)
            val end = Pair(42, 22) // exit position

            val newPath = aStar(start, end, currentGameMap.convertMapTo2DArrayForA_Star(), addStartNode = false)?.map { it.findPositionFromArrayIndexes(cellSize) }

            path.clear()
            path.addAll(newPath ?: emptyList())
        }

        val drawSprites = measureTime {
            for (i in path.size - 1 downTo 1) {
                drawSprite(
                    screen = screen,
                    cellSize = 2,
                    fovRad = PLAYER_FOV,
                    player = player,
                    spriteX = path[i].first,
                    spriteY = path[i].second,
                    texture = otherSprites.light,
                    spriteBitmapSize = 56,
                    wallDepths = wallDepths,
                    transparentColor = otherSprites.transparentColor,
                    scaleFactor = 1f
                )
            }
        }

        // draw pistol
        val drawPistolTime = measureTime {
            screen.drawBitmap(
                bitmap = pistolSprite.getFrame(player.mainPlayerShootingFrame)!!,
                x = 2 * screen.width / 3 + (20 * fastSin(player.x)).toInt(),
                y = (screen.height - 175 * 0.8f + 20 - 10 * fastSin(player.y)).toInt(),
                bitmapSizeX = 128,
                bitmapSizeY = 128,
                transparentColor = pistolSprite.TRANSPARENT_COLOR
            )
        }

        // draw cross when enemy is in range
        val drawCrossTime = measureTime {
            enemies.forEach { enemy ->
                if (player.distanceTo(enemy) < 10 && player.inShotAngle(enemy)) {
                    drawCross(screen, screenWidth, screenHeight)
                }
            }
        }


        val animateTime = measureTime {
            player.animate(gameMap = currentGameMap, cellSize = cellSize)
        }


//    drawText(bitmap, 10, 10, renderTime.toString(unit = DurationUnit.MILLISECONDS, decimals = 2), Color.White)

        val totalTime =
            castRayTime + drawSpriteTime + drawMapTime + drawPistolTime + drawCrossTime + animateTime
        if (LOG_STATS) {
            if (System.currentTimeMillis() % 3 == 0L) {
                println(
                    "total: ${totalTime.toInt(DurationUnit.MILLISECONDS)}ms, castRayTime=${
                        castRayTime.toInt(
                            DurationUnit.MICROSECONDS
                        )
                    } , " +
                            "drawSpriteTime=${drawSpriteTime.toInt(DurationUnit.MICROSECONDS)}, " +
                            "drawMapTime=${drawMapTime.toInt(DurationUnit.MICROSECONDS)}, " +
                            "drawPistolTime=${drawPistolTime.toInt(DurationUnit.MICROSECONDS)}, " +
                            "drawCrossTime=${drawCrossTime.toInt(DurationUnit.MICROSECONDS)}, " +
                            "animateTime=${animateTime.toInt(DurationUnit.MICROSECONDS)}"
                )
            }
        }

        return screen
    }


    private fun get360Distances(player: Player): Array<Float> {
        val distances = Array(36) { 0f } // 360 degrees / 10 degrees per step = 36 steps
        val types = Array(36) { 0f }
        val rayStep = 10.toRadian() // Convert 10 degrees to radians

        for (i in distances.indices step 2) {
            val rayAngle = player.rotationRad + i * rayStep
            val rayDirX = fastCos(rayAngle)
            val rayDirY = fastSin(rayAngle)

            var mapX = floor(player.x / cellSize).toInt()
            var mapY = floor(player.y / cellSize).toInt()

            val deltaDistX = abs(1 / rayDirX)
            val deltaDistY = abs(1 / rayDirY)

            val stepX: Int
            val stepY: Int
            var sideDistX: Float
            var sideDistY: Float

            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (player.x / cellSize - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0f - player.x / cellSize) * deltaDistX
            }

            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (player.y / cellSize - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0f - player.y / cellSize) * deltaDistY
            }

            var hit = false
            var textureIndex = 0
            while (!hit) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                }

                if (mapX < 0 || mapX >= currentGameMap.MAP_X || mapY < 0 || mapY >= currentGameMap.MAP_Y) {
                    hit = true
                } else if (currentGameMap.MAP[mapY * currentGameMap.MAP_X + mapX] > 0) {
                    hit = true
                    textureIndex = currentGameMap.MAP[mapY * currentGameMap.MAP_X + mapX]
                }
            }

            distances[i] = if (sideDistX < sideDistY) {
                (mapX - player.x / cellSize + (1 - stepX) / 2) / rayDirX
            } else {
                (mapY - player.y / cellSize + (1 - stepY) / 2) / rayDirY
            }
            types[i] = textureIndex.toFloat()
        }

        val normalizedDistances = distances.map { (it / 10f).coerceIn(0f..1f) }
        return normalizedDistances.toTypedArray() + types
    }

    fun getDistances(): FloatArray {
        return get360Distances(player).toFloatArray()
    }


    // Ray casting using DDA algorithm. Cover walls with wall texture. Add fish-eye correction.
    private fun castRays(
        player: Player,
        bitmap: Screen,
        castWalls: Boolean,
        castFloor: Boolean,
        castCeiling: Boolean
    ) =
        runBlocking {
            val rayCount = screenWidth
            val rayStep = PLAYER_FOV / rayCount
            val halfRayCount = rayCount / 2
            val halfScreenHeight = screenHeight / 2

            // Pre-calculate player position in grid coordinates to avoid repeated division
            val playerGridX = player.x / cellSize
            val playerGridY = player.y / cellSize
            val playerMapX = floor(playerGridX).toInt()
            val playerMapY = floor(playerGridY).toInt()

            // Pre-calculate map bounds for faster boundary checks
            val mapXMax = currentGameMap.MAP_X
            val mapYMax = currentGameMap.MAP_Y
            val mapWidth = currentGameMap.MAP_X

            // Precompute all ray directions and related values
            val rayData = Array(rayCount) { x ->
                val rayAngle = player.rotationRad + (x - halfRayCount) * rayStep
                val sinVal = fastSin(rayAngle)
                val cosVal = fastCos(rayAngle)
                RayData(
                    dirX = cosVal,
                    dirY = sinVal,
                    deltaDistX = abs(1f / cosVal),
                    deltaDistY = abs(1f / sinVal)
                )
            }

            // Optimize chunk size for better CPU cache utilization
            val optimalChunkSize = max(64, rayCount / (cpuCount * 2))
            val rayChunks = (0 until rayCount step optimalChunkSize).toList()

            coroutineScope {
                val deferredResults = rayChunks.map { startX ->
                    async(Dispatchers.Default) {
                        val endX = (startX + optimalChunkSize).coerceAtMost(rayCount)

                        for (x in startX until endX) {
                            val ray = rayData[x]

                            // Calculate step direction and initial side distances
                            val stepX: Int
                            val stepY: Int
                            var sideDistX: Float
                            var sideDistY: Float
                            var mapX = playerMapX
                            var mapY = playerMapY

                            if (ray.dirX < 0) {
                                stepX = -1
                                sideDistX = (playerGridX - mapX) * ray.deltaDistX
                            } else {
                                stepX = 1
                                sideDistX = (mapX + 1f - playerGridX) * ray.deltaDistX
                            }

                            if (ray.dirY < 0) {
                                stepY = -1
                                sideDistY = (playerGridY - mapY) * ray.deltaDistY
                            } else {
                                stepY = 1
                                sideDistY = (mapY + 1f - playerGridY) * ray.deltaDistY
                            }

                            // Optimized DDA algorithm with reduced branching
                            var side = 0
                            var wallTextureIndex = 0

                            // Unrolled DDA loop for better performance
                            while (true) {
                                if (sideDistX < sideDistY) {
                                    sideDistX += ray.deltaDistX
                                    mapX += stepX
                                    side = 0
                                } else {
                                    sideDistY += ray.deltaDistY
                                    mapY += stepY
                                    side = 1
                                }

                                // Single boundary check with early exit
                                if (mapX < 0 || mapX >= mapXMax || mapY < 0 || mapY >= mapYMax) {
                                    break
                                }

                                // Direct array access without function call
                                val mapValue = currentGameMap.MAP[mapY * mapWidth + mapX]
                                if (mapValue > 0) {
                                    wallTextureIndex = mapValue
                                    break
                                }
                            }

                            // Calculate perpendicular wall distance with optimized math
                            val perpWallDist: Float = if (side == 0) {
                                (mapX - playerGridX + (1 - stepX) * 0.5f) / ray.dirX
                            } else {
                                (mapY - playerGridY + (1 - stepY) * 0.5f) / ray.dirY
                            }

                            wallDepths[x] = perpWallDist

                            // Skip fish-eye correction if not needed (commented out in original)
                            val correctedWallDist = perpWallDist

                            // Calculate rendering bounds with faster integer operations
                            val lineHeight = (screenHeight / correctedWallDist).toInt()
                            val drawStart = max(0, halfScreenHeight - (lineHeight shr 1))
                            val drawEnd = min(screenHeight - 1, halfScreenHeight + (lineHeight shr 1))

                            // Render components only if needed
                            if (castWalls) {
                                castWallColumn(
                                    textureIndex = wallTextureIndex,
                                    wallSide = side,
                                    player = player,
                                    wallDistance = correctedWallDist,
                                    rayDirectionY = ray.dirY,
                                    rayDirectionX = ray.dirX,
                                    columnHeight = lineHeight,
                                    drawStartY = drawStart,
                                    drawEndY = drawEnd,
                                    screen = bitmap,
                                    screenColumn = x,
                                    walls = wallTextures,
                                    cellSize = cellSize,
                                    height = screenHeight
                                )
                            }

                            if (castCeiling) {
                                renderCeiling(
                                    drawStartY = drawStart,
                                    player = player,
                                    rayDirectionX = ray.dirX,
                                    rayDirectionY = ray.dirY,
                                    screen = bitmap,
                                    screenColumn = x
                                )
                            }

                            if (castFloor) {
                                renderFloor(
                                    drawEndY = drawEnd,
                                    player = player,
                                    rayDirectionX = ray.dirX,
                                    rayDirectionY = ray.dirY,
                                    screen = bitmap,
                                    screenColumn = x
                                )
                            }
                        }
                    }
                }

                deferredResults.awaitAll()
            }
        }


    private fun renderFloor(
        drawEndY: Int,
        player: Player,
        rayDirectionX: Float,
        rayDirectionY: Float,
        screen: Screen,
        screenColumn: Int
    ) {
        renderSurface(
            drawEndY until screenHeight,
            player,
            rayDirectionX,
            rayDirectionY,
            floorTexture,
            screen,
            screenColumn,
            isCeiling = false,
            height = screenHeight,
            cellSize = cellSize
        )
    }

    private fun renderCeiling(
        drawStartY: Int,
        player: Player,
        rayDirectionX: Float,
        rayDirectionY: Float,
        screen: Screen,
        screenColumn: Int
    ) {
        renderSurface(
            0 until drawStartY,
            player,
            rayDirectionX,
            rayDirectionY,
            ceilingTexture,
            screen,
            screenColumn,
            isCeiling = true,
            height = screenHeight,
            cellSize = cellSize
        )
    }


    fun movePlayer(x: Float, y: Float, lr: Float) {
//        if (AI_RECORD_DATA) recordGameForAi(x, y, lr)

        var dx = 0f
        var dy = 0f
        var dr = 0f

        dx = lr * fastCos(player.rotationRad + 90f)
        dy = lr * fastSin(player.rotationRad + 90f)

        dx += y * fastCos(player.rotationRad) // delta x
        dy += y * fastSin(player.rotationRad) // delta y

        dr = x // delta rotation
        player.rotationRad += dr.normalizeAngle()

        val newX = player.x + dx
        val newY = player.y + dy

        if (isWall(
                newX,
                player.y,
                currentGameMap.MAP,
                currentGameMap.MAP_X,
                currentGameMap.MAP_Y,
                cellSize
            ) == WallType.NONE
        ) {
            player.x = newX
        }

        if (isWall(
                player.x,
                newY,
                currentGameMap.MAP,
                currentGameMap.MAP_X,
                currentGameMap.MAP_Y,
                cellSize
            ) == WallType.NONE
        ) {
            player.y = newY
        }

        openDoor(newX, newY)

        // Exit
        if (isExitTouched(newX, newY)) {
            error("You win!")
        }
    }

    private var lastInput: FloatArray = floatArrayOf(0f, 0f, 0f, 0f, 0f)
    private fun recordGameForAi(up: Float, down: Float, left: Float, right: Float, shoot: Float) {
        val data = get360Distances(player)

        val normalisedPlayerX = player.x / (currentGameMap.MAP_X * cellSize)
        val normalisedPlayerY = player.y / (currentGameMap.MAP_Y * cellSize)
        val normalizedPlayerRotation = player.rotationRad / (2 * Math.PI)

        val out = data.joinToString(";") + ";$normalisedPlayerX;$normalisedPlayerY;$normalizedPlayerRotation;$up;$down;$left;$right;$shoot"
//        Log.d("aaa", out)
        logs.writeLog(out)

        lastInput[0] = up
        lastInput[1] = down
        lastInput[2] = left
        lastInput[3] = right
        lastInput[4] = shoot
    }

    fun getDataForAi(): FloatArray {
        val distance = get360Distances(player).toFloatArray()
        val normalisedPlayerX = player.x / (currentGameMap.MAP_X * cellSize)
        val normalisedPlayerY = player.y / (currentGameMap.MAP_Y * cellSize)
        val normalizedPlayerRotation = (player.rotationRad / (2 * Math.PI)).toFloat()
        return distance + floatArrayOf(normalisedPlayerX, normalisedPlayerY, normalizedPlayerRotation)
    }

    fun shareLogFile() {
        logs.shareLogFile()
    }

    private fun movePlayer(pressedKeys: Set<Moves>) {
        var up = 0
        var down = 0
        var left = 0
        var right = 0
        var shoot = 0


        var dx = 0f
        var dy = 0f
        var dr = 0f

        if (Moves.UP in pressedKeys) {
            dx += PLAYER_SPEED * fastCos(player.rotationRad)
            dy += PLAYER_SPEED * fastSin(player.rotationRad)
            up = 1
        }
        if (Moves.DOWN in pressedKeys) {
            dx -= PLAYER_SPEED * fastCos(player.rotationRad)
            dy -= PLAYER_SPEED * fastSin(player.rotationRad)
            down = 1
        }

        if (Moves.MOVE_LEFT in pressedKeys) {
            dx -= PLAYER_SPEED * fastCos(player.rotationRad + 90.toRadian())
            dy -= PLAYER_SPEED * fastSin(player.rotationRad + 90.toRadian())
        }

        if (Moves.MOVE_RIGHT in pressedKeys) {
            dx += PLAYER_SPEED * fastCos(player.rotationRad + 90.toRadian())
            dy += PLAYER_SPEED * fastSin(player.rotationRad + 90.toRadian())
        }

        if (Moves.LEFT in pressedKeys) {
            dr -= PLAYER_ROTATION_SPEED_RAD
            left = 1
        }
        if (Moves.RIGHT in pressedKeys) {
            dr += PLAYER_ROTATION_SPEED_RAD
            right = 1
        }


        if (Moves.SHOOT in pressedKeys) {
            shootAndCheckHits()
            shoot = 1
        }

        if (AI_RECORD_DATA && pressedKeys.isNotEmpty()) {
            recordGameForAi(up.toFloat(), down.toFloat(), left.toFloat(), right.toFloat(), shoot.toFloat())
        }


        // Apply rotation
        val newRotation = player.rotationRad + dr
        player.rotationRad = newRotation.normalizeAngle()

        // Apply movement with collision detection
        val newX = player.x + dx
        val newY = player.y + dy

        if (isWall(
                newX,
                player.y,
                currentGameMap.MAP,
                currentGameMap.MAP_X,
                currentGameMap.MAP_Y,
                cellSize
            ) == WallType.NONE
        ) {
            player.x = newX
        }

        if (isWall(
                player.x,
                newY,
                currentGameMap.MAP,
                currentGameMap.MAP_X,
                currentGameMap.MAP_Y,
                cellSize
            ) == WallType.NONE
        ) {
            player.y = newY
        }

        openDoor(newX, newY)

        // Exit
        if (isExitTouched(newX, newY)) {
            error("You win!")
        }


    }

    private fun isExitTouched(newX: Float, newY: Float): Boolean {
        return isWall(
            newX,
            player.y,
            currentGameMap.MAP,
            currentGameMap.MAP_X,
            currentGameMap.MAP_Y,
            cellSize
        ) == WallType.EXIT ||
                isWall(
                    newY,
                    player.y,
                    currentGameMap.MAP,
                    currentGameMap.MAP_X,
                    currentGameMap.MAP_Y,
                    cellSize
                ) == WallType.EXIT
    }

    private fun openDoor(newX: Float, newY: Float) {
        // Open door
        if (isWall(
                newX,
                player.y,
                currentGameMap.MAP,
                currentGameMap.MAP_X,
                currentGameMap.MAP_Y,
                cellSize
            ) == WallType.DOOR
        ) {
            currentGameMap.MAP[currentGameMap.MAP_X * (player.y.toInt() / cellSize) + (newX.toInt() / cellSize)] =
                0
        }

        if (isWall(
                player.x,
                newY,
                currentGameMap.MAP,
                currentGameMap.MAP_X,
                currentGameMap.MAP_Y,
                cellSize
            ) == WallType.DOOR
        ) {
            currentGameMap.MAP[currentGameMap.MAP_X * (newY.toInt() / cellSize) + (player.x.toInt() / cellSize)] =
                0
        }
    }

    private fun shootAndCheckHits() {
        player.animate(newState = PlayerState.SHOOTING, gameMap = currentGameMap, cellSize = cellSize)

        if (player.mainPlayerShootingFrame == 0) {
            sound.playSound(R.raw.gunshot1)
        }

        enemies.forEach { enemy ->
            if (player.distanceTo(enemy) < 10 && player.inShotAngle(enemy)) {
                if (isWallBetween(
                        enemy,
                        player
                    )
                ) return // check if there is a wall between player and enemy

                enemy.health -= SHOOT_PLAYER_DAMAGE

                if (enemy.health <= 0 && enemy.state != PlayerState.DEAD) {
                    enemy.state = PlayerState.DYING
                }
            }
        }
    }


    /**
     * Check if there is a wall between player and enemy
     */
    private fun isWallBetween(player: Player, enemy: Player): Boolean {
        val dx = enemy.x - player.x
        val dy = enemy.y - player.y

        val rayDirX = dx / sqrt(dx * dx + dy * dy)
        val rayDirY = dy / sqrt(dx * dx + dy * dy)

        val stepX = if (rayDirX < 0) -1 else 1
        val stepY = if (rayDirY < 0) -1 else 1

        val deltaDistX = abs(1 / rayDirX)
        val deltaDistY = abs(1 / rayDirY)

        var sideDistX =
            if (rayDirX < 0) (player.x - floor(player.x)) * deltaDistX else (ceil(player.x) - player.x) * deltaDistX
        var sideDistY =
            if (rayDirY < 0) (player.y - floor(player.y)) * deltaDistY else (ceil(player.y) - player.y) * deltaDistY

        var mapX = floor(player.x).toInt()
        var mapY = floor(player.y).toInt()

        while (true) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX
                mapX += stepX
            } else {
                sideDistY += deltaDistY
                mapY += stepY
            }

            if (mapX == floor(enemy.x).toInt() && mapY == floor(enemy.y).toInt()) {
                return false
            }

            if (isWall(
                    mapX.toFloat(),
                    mapY.toFloat(),
                    currentGameMap.MAP,
                    currentGameMap.MAP_X,
                    currentGameMap.MAP_Y,
                    cellSize
                ) != WallType.NONE
            ) {
                return true
            }
        }
    }

    fun dispose() {
        sound.stopMusic()
        sound.release()
    }


    fun getAliveEnemiesCount(): Int {
        return enemies.count { it.state != PlayerState.DEAD }
    }

    fun getPlayerHealth(): Int {
        return player.health
    }
}

private data class RayData(
    val dirX: Float,
    val dirY: Float,
    val deltaDistX: Float,
    val deltaDistY: Float
)






