package com.example.fps_raytrace.engine.raycaster

import android.content.Context
import com.example.fps_raytrace.Const.DRAW_MAP
import com.example.fps_raytrace.Const.SHOOT_PLAYER_DAMAGE
import com.example.fps_raytrace.R
import com.example.fps_raytrace.engine.Moves
import com.example.fps_raytrace.engine.Player
import com.example.fps_raytrace.engine.PlayerState
import com.example.fps_raytrace.engine.animate
import com.example.fps_raytrace.engine.distanceTo
import com.example.fps_raytrace.engine.inShotAngle
import com.example.fps_raytrace.engine.map.drawMap
import com.example.fps_raytrace.textures.Walls
import com.example.fps_raytrace.engine.utils.normalizeAngle
import com.example.fps_raytrace.engine.utils.toRadian
import com.example.fps_raytrace.engine.utils.readPpmImage
import com.example.fps_raytrace.engine.utils.Screen
import com.example.fps_raytrace.engine.utils.Sound
import com.example.fps_raytrace.engine.utils.WallType
import com.example.fps_raytrace.engine.utils.isWall
import com.example.fps_raytrace.maps.Map
import com.example.fps_raytrace.maps.Map1
import com.example.fps_raytrace.maps.MapObject
import com.example.fps_raytrace.maps.findPositionBasedOnMapIndex
import com.example.fps_raytrace.maps.getEnemiesFromMap
import com.example.fps_raytrace.sprites.GuardSprite
import com.example.fps_raytrace.sprites.PistolSprite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class RaytracerEngine(
    context: Context,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val fovRad: Float = 60.toRadian(),
    private val moveStep: Float = 0.2f,
    private val rotationStepRad: Float = 2f.toRadian(),
    private val cellSize: Int = 2
) {
    private val currentMap: Map = Map1
    private val wallTextures = Walls(context)
    private val pistolSprite = PistolSprite(context)
    private val guardSprite = GuardSprite(context)
    private val floorTexture: IntArray = readPpmImage(context, R.raw.floor)
    private val ceilingTexture: IntArray = readPpmImage(context, R.raw.celling)

    private val wallDepths = FloatArray(screenWidth) // depth buffer

    private val screen = Screen(screenWidth, screenHeight)
    private val sound = Sound(context)

    private var cpuCount: Int = Runtime.getRuntime().availableProcessors()

    private val playerPosition = findPositionBasedOnMapIndex(
        mapX = currentMap.MAP_X,
        mapY = currentMap.MAP_Y,
        cellSize = cellSize,
        index = currentMap.MAP.indexOf(MapObject.player.id)
    )

    // create player
    private val player = Player(
        x = playerPosition[0],
        y = playerPosition[1],
        rotationRad = 0f.toRadian().normalizeAngle(),
        isMainPlayer = true,
        sound = sound,
        state = PlayerState.WALKING
    )

    //create enemies
    private val enemies = currentMap.getEnemiesFromMap(
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
            enemy.animate(map = currentMap,
                cellSize = cellSize,
                mainPlayer = player,
                isWallBetween = {
                    isWallBetween(player, enemy)
                })
        }

        player.animate(newState = player.state, map = currentMap, cellSize = cellSize)
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
                    fovRad = fovRad
                )
            }
        }

        val drawMapTime = measureTime {
            if (DRAW_MAP) {
                drawMap(
                    screen = screen,
                    map = currentMap,
                    xOffset = 10,
                    yOffset = screenHeight - cellSize * currentMap.MAP_Y - 10,
                    player = player,
                    enemies = enemies,
                    cellSize = cellSize,
                    playerSize = 5f
                )
            }
        }


        // draw pistol
        val drawPistolTime = measureTime {
            screen.drawBitmap(
                bitmap = pistolSprite.getFrame(player.mainPlayerShootingFrame)!!,
                x = 2 * screen.width / 3 + (20 * sin(player.x)).toInt(),
                y = (screen.height - 175 * 0.8f + 20 - 10 * sin(player.y)).toInt(),
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
            player.animate(map = currentMap, cellSize = cellSize)
        }


//    drawText(bitmap, 10, 10, renderTime.toString(unit = DurationUnit.MILLISECONDS, decimals = 2), Color.White)

        val totalTime =
            castRayTime + drawSpriteTime + drawMapTime + drawPistolTime + drawCrossTime + animateTime
        if (true) {
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
            val rayStep = fovRad / rayCount

            // Precompute values to avoid recalculating in the loop
            val sinCache = FloatArray(rayCount)
            val cosCache = FloatArray(rayCount)
            val deltaDistXCache = FloatArray(rayCount)
            val deltaDistYCache = FloatArray(rayCount)

            for (x in 0 until rayCount) {
                val rayAngle = player.rotationRad + (x - rayCount / 2) * rayStep

                sinCache[x] = sin(rayAngle)
                cosCache[x] = cos(rayAngle)
                deltaDistXCache[x] = abs(1 / cosCache[x])
                deltaDistYCache[x] = abs(1 / sinCache[x])
            }

            // Chunking the rays to leverage parallel processing
            val chunkSize = rayCount / cpuCount
            val rayChunks = (0 until rayCount step chunkSize).toList()

            coroutineScope {
                val deferredResults = rayChunks.map { startX ->
                    async(Dispatchers.Default) {
                        //don't run on chunk 1
//                        if(startX >= chunkSize*2) return@async

                        for (x in startX until (startX + chunkSize).coerceAtMost(rayCount)) {
                            // Use cached values
                            val rayDirX = cosCache[x]
                            val rayDirY = sinCache[x]
                            val deltaDistX = deltaDistXCache[x]
                            val deltaDistY = deltaDistYCache[x]

                            // Local variables to avoid repeated array access
                            var mapX = floor(player.x / cellSize).toInt()
                            var mapY = floor(player.y / cellSize).toInt()
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
                            var side = 0
                            var wallTextureIndex = 0

                            // Early exit upon hit detection
                            while (!hit) {
                                if (sideDistX < sideDistY) {
                                    sideDistX += deltaDistX
                                    mapX += stepX
                                    side = 0
                                } else {
                                    sideDistY += deltaDistY
                                    mapY += stepY
                                    side = 1
                                }

                                if (mapX < 0 || mapX >= currentMap.MAP_X || mapY < 0 || mapY >= currentMap.MAP_Y) {
                                    hit = true
                                } else if (currentMap.MAP[mapY * currentMap.MAP_X + mapX] > 0) {
                                    hit = true
                                    wallTextureIndex =
                                        currentMap.MAP[mapY * currentMap.MAP_X + mapX]
                                }
                            }

                            // Calculate perpendicular wall distance
                            val perpWallDist: Float = if (side == 0) {
                                (mapX - player.x / cellSize + (1 - stepX) / 2) / rayDirX
                            } else {
                                (mapY - player.y / cellSize + (1 - stepY) / 2) / rayDirY
                            }

                            wallDepths[x] = perpWallDist

                            // Fish-eye correction
                            val correctedWallDist = perpWallDist// * cos(x * rayStep - fovRad / 2)

                            // Calculate height of the line to draw on screen
                            val lineHeight = (screenHeight / correctedWallDist).toInt()

                            // Calculate lowest and highest pixel to fill in current stripe
                            val drawStart = max(0, -lineHeight / 2 + screenHeight / 2)
                            val drawEnd = min(screenHeight - 1, lineHeight / 2 + screenHeight / 2)


                            // Wall casting
                            if (castWalls) {
                                castWallColumn(
                                    textureIndex = wallTextureIndex,
                                    wallSide = side,
                                    player = player,
                                    wallDistance = correctedWallDist,
                                    rayDirectionY = rayDirY,
                                    rayDirectionX = rayDirX,
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

                            // Ceiling casting
                            if (castCeiling) {
                                renderCeiling(
                                    drawStartY = drawStart,
                                    player = player,
                                    rayDirectionX = rayDirX,
                                    rayDirectionY = rayDirY,
                                    screen = bitmap,
                                    screenColumn = x
                                )
                            }

                            // Floor casting
                            if (castFloor) {
                                renderFloor(
                                    drawEndY = drawEnd,
                                    player = player,
                                    rayDirectionX = rayDirX,
                                    rayDirectionY = rayDirY,
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


    fun movePlayer(x: Float, y: Float) {
        var dx = 0f
        var dy = 0f
        var dr = 0f

        dx += y * cos(player.rotationRad)
        dy += y * sin(player.rotationRad)

        dr = x

        player.rotationRad += dr.normalizeAngle()

        val newX = player.x + dx
        val newY = player.y + dy

        if (isWall(
                newX,
                player.y,
                currentMap.MAP,
                currentMap.MAP_X,
                currentMap.MAP_Y,
                cellSize
            ) == WallType.NONE
        ) {
            player.x = newX
        }

        if (isWall(
                player.x,
                newY,
                currentMap.MAP,
                currentMap.MAP_X,
                currentMap.MAP_Y,
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

    private fun movePlayer(pressedKeys: Set<Moves>) {
        var dx = 0f
        var dy = 0f
        var dr = 0f

        if (Moves.UP in pressedKeys) {
            dx += moveStep * cos(player.rotationRad)
            dy += moveStep * sin(player.rotationRad)
        }
        if (Moves.DOWN in pressedKeys) {
            dx -= moveStep * cos(player.rotationRad)
            dy -= moveStep * sin(player.rotationRad)
        }

        if (Moves.MOVE_LEFT in pressedKeys) {
            dx -= moveStep * cos(player.rotationRad + 90.toRadian())
            dy -= moveStep * sin(player.rotationRad + 90.toRadian())
        }

        if (Moves.MOVE_RIGHT in pressedKeys) {
            dx += moveStep * cos(player.rotationRad + 90.toRadian())
            dy += moveStep * sin(player.rotationRad + 90.toRadian())
        }

        if (Moves.LEFT in pressedKeys) {
            dr -= rotationStepRad
        }
        if (Moves.RIGHT in pressedKeys) {
            dr += rotationStepRad
        }

        if (Moves.SHOOT in pressedKeys) {
            shootAndCheckHits()
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
                currentMap.MAP,
                currentMap.MAP_X,
                currentMap.MAP_Y,
                cellSize
            ) == WallType.NONE
        ) {
            player.x = newX
        }

        if (isWall(
                player.x,
                newY,
                currentMap.MAP,
                currentMap.MAP_X,
                currentMap.MAP_Y,
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
            currentMap.MAP,
            currentMap.MAP_X,
            currentMap.MAP_Y,
            cellSize
        ) == WallType.EXIT ||
                isWall(
                    newY,
                    player.y,
                    currentMap.MAP,
                    currentMap.MAP_X,
                    currentMap.MAP_Y,
                    cellSize
                ) == WallType.EXIT
    }

    private fun openDoor(newX: Float, newY: Float) {
        // Open door
        if (isWall(
                newX,
                player.y,
                currentMap.MAP,
                currentMap.MAP_X,
                currentMap.MAP_Y,
                cellSize
            ) == WallType.DOOR
        ) {
            currentMap.MAP[currentMap.MAP_X * (player.y.toInt() / cellSize) + (newX.toInt() / cellSize)] =
                0
        }

        if (isWall(
                player.x,
                newY,
                currentMap.MAP,
                currentMap.MAP_X,
                currentMap.MAP_Y,
                cellSize
            ) == WallType.DOOR
        ) {
            currentMap.MAP[currentMap.MAP_X * (newY.toInt() / cellSize) + (player.x.toInt() / cellSize)] =
                0
        }
    }

    private fun shootAndCheckHits() {
        player.animate(newState = PlayerState.SHOOTING, map = currentMap, cellSize = cellSize)

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
                    currentMap.MAP,
                    currentMap.MAP_X,
                    currentMap.MAP_Y,
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






