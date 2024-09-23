import android.content.Context
import com.example.fps_raytrace.R
import engine.*
import engine.textures.readPpmImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import maps.*
import maps.Map
import models.*
import sprites.GuardSprite
import sprites.PistolSprite
import kotlin.math.*
import kotlin.time.DurationUnit
import kotlin.time.measureTime


enum class Moves {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    MOVE_LEFT,
    MOVE_RIGHT,
    SHOOT
}

class RaytracerEngine(
    context: Context,
    private val width: Int,
    private val height: Int,
    private val fovRad: Float = 60.toRadian(),
    private val moveStep: Float = 0.2f,
    private val rotationStepRad: Float = 2f.toRadian(),
    private val worldTextureSize: Int = 64,
    private val cellSize: Int = 2
) {
    private val currentMap: Map = Map1
    private val walls = Walls(context)
    private val pistolSprite = PistolSprite(context)
    private val guardSprite = GuardSprite(context)
    private val floorTexture: IntArray = readPpmImage(context, R.raw.floor)
    private val cellingTexture: IntArray = readPpmImage(context, R.raw.celling)

    private val wallDepths = FloatArray(width) // depth buffer

    private val screen = Screen(width, height)
    private val sound = Sound(context)

    private var cpuCount: Int = Runtime.getRuntime().availableProcessors()

    private val playerPosition =
        findPositionBasedOnMapIndex(
            mapX = currentMap.MAP_X,
            mapY = currentMap.MAP_Y,
            cellSize = cellSize,
            index = currentMap.MAP.indexOf(MapObject.player.id)
        )

    // create player
    private val player =
        Player(
            x = playerPosition[0],
            y = playerPosition[1],
            rotationRad = -90f.toRadian().normalizeAngle(),
            isMainPlayer = true,
            sound = sound,
        )


    //create enemies
    private val enemies = currentMap.getEnemiesFromMap(cellSize = cellSize)


    fun start(){
        sound.loadSound(R.raw.soundtrack)
        sound.loadSound(R.raw.gunshot1)
        sound.loadSound(R.raw.mandeathscream)
        sound.loadSound(R.raw.step)

        sound.playMusic(R.raw.soundtrack, true)
    }

    fun gameLoop(pressedKeys: Set<Moves>, onFrame: (Screen) -> Unit) {
        enemies.forEach { enemy ->
            enemy.animate(map = currentMap, cellSize = cellSize)
        }

        player.animate(state = PlayerState.WALKING, map = currentMap, cellSize = cellSize)
        movePlayer(pressedKeys)
        onFrame(generateFrame())
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
                drawSprite(screen, player, enemy, wallDepths)
            }
        }

        val drawMapTime = measureTime {
            drawMap(
                screen = screen,
                map = currentMap,
                xOffset = 10,
                yOffset = height - cellSize * currentMap.MAP_Y - 10,
                player = player,
                enemies = enemies,
                cellSize = cellSize,
                playerSize = 5f
            )
        }


        // draw pistol
        val drawPistolTime = measureTime {
            screen.drawBitmap(
                bitmap = pistolSprite.getFrame(player.shootingFrame)!!,
                x = 2 * screen.w / 3 + (20 * sin(player.x)).toInt(),
                y = (screen.h - 175 * 0.8f + 20 - 10 * sin(player.y)).toInt(),
                bitmapSizeX = 128,
                bitmapSizeY = 128,
                transparentColor = pistolSprite.TRANSPARENT_COLOR
            )
        }

        // draw cross when enemy is in range
        val drawCrossTime = measureTime {
            enemies.forEach { enemy ->
                if (player.distanceTo(enemy) < 10 && player.inShotAngle(enemy)) {
                    drawCross(screen)
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

    private fun drawCross(screen: Screen) {
        val crossSize = 10
        val x = width / 2 - crossSize / 2
        val y = height / 2 - crossSize / 2

        screen.drawLine(
            x,
            y,
            x + crossSize,
            y + crossSize,
            255,
            255,
            255
        )
        screen.drawLine(
            x + crossSize,
            y,
            x,
            y + crossSize,
            255,
            255,
            255
        )
    }


    // draws square in 3d world using 3d projection mapping
    private fun drawSprite(screen: Screen, player: Player, enemy: Player, wallDepths: FloatArray) {
        // Calculate the angle from the enemy to the player
        val angleToPlayer = atan2(player.y - enemy.y, player.x - enemy.x)

        // Calculate the difference between the enemy's rotation and the angle to the player
        var diff = angleToPlayer - enemy.rotationRad

        // Normalize the angle difference to be between -PI and PI
        diff = (diff + engine.PI) % (2 * engine.PI) - engine.PI

        // Calculate the texture index, ensuring it falls within the valid range
        val numTextures = 8  // Assuming there are 8 textures in the textureSet
        val textureIndex =
            numTextures - ((diff / (2 * engine.PI) * numTextures) + numTextures) % numTextures

        // Fetch the correct texture for rendering
        val texture = guardSprite.getTexture(
            direction = textureIndex.toInt(),
            state = enemy.state,
            walkingFrame = enemy.walkingFrame,
            dyingFrame = enemy.dyingFrame
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
        angle = (angle + engine.PI) % (2 * engine.PI) - engine.PI

        // Check if enemy is within player's FOV
        if (abs(angle) < fovRad / 2) {
            // Calculate screen x-coordinate
            val screenX = ((width / 2) * (1 + angle / (fovRad / 2))).toInt()

            // Calculate perceived height of the square
            val perceivedHeight = (height / distance * pointHeight).toInt()

            // Calculate top and bottom y-coordinates
            val topY = (height / 2 - perceivedHeight / 2).coerceIn(0, height - 1)
            val bottomY = (height / 2 + perceivedHeight / 2).coerceIn(0, height - 1)

            // Calculate perceived width of the square
            val perceivedWidth = perceivedHeight

            // Draw sprite
            for (y in topY..bottomY) {
                for (x in (screenX - perceivedWidth / 2)..(screenX + perceivedWidth / 2)) {
                    if (x >= 0 && x < width) {
                        // Only draw the sprite pixel if it's closer than the wall
                        if (wallDepths[x] - (distance / cellSize) > -0.1) { // -0.1 - padding to avoid wall clipping
                            // Calculate texture coordinates
                            val texX =
                                ((x - (screenX - perceivedWidth / 2)).toFloat() / perceivedWidth * guardSprite.SPRITE_SIZE).toInt() % guardSprite.SPRITE_SIZE
                            val texY =
                                ((y - topY).toFloat() / perceivedHeight * guardSprite.SPRITE_SIZE).toInt() % guardSprite.SPRITE_SIZE

                            val texIndex = (texY * guardSprite.SPRITE_SIZE + texX) * 3

                            val r = texture[texIndex]
                            val g = texture[texIndex + 1]
                            val b = texture[texIndex + 2]

                            if (r != guardSprite.TRANSPARENT_COLOR.red || g != guardSprite.TRANSPARENT_COLOR.green || b != guardSprite.TRANSPARENT_COLOR.blue) {
                                // Apply distance-based shading
                                val intensity = (1.0f - (distance / 30.0f)).coerceIn(0.2f, 1f)

                                screen.setRGB(
                                    x,
                                    y,
                                    r.darkenColor(intensity),
                                    g.darkenColor(intensity),
                                    b.darkenColor(intensity)
                                )
                            }
                        }
                    }
                }
            }
        }
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
            val rayCount = width
            val rayStep = fovRad / rayCount

            // Precompute values to avoid recalculating in the loop
            val sinCache = FloatArray(rayCount)
            val cosCache = FloatArray(rayCount)
            val deltaDistXCache = FloatArray(rayCount)
            val deltaDistYCache = FloatArray(rayCount)

            for (x in 0 until rayCount) {
                val rayAngle = player.rotationRad - fovRad / 2 + x * rayStep
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
                            val correctedWallDist =
                                perpWallDist * cos(player.rotationRad - (player.rotationRad - fovRad / 2 + x * rayStep))

                            // Calculate height of the line to draw on screen
                            val lineHeight = (height / correctedWallDist).toInt()

                            // Calculate lowest and highest pixel to fill in current stripe
                            val drawStart = (-lineHeight / 2 + height / 2).coerceAtLeast(0)
                            val drawEnd = (lineHeight / 2 + height / 2).coerceAtMost(height - 1)


                            if (castWalls) {                                // Texture mapping for walls
                                val wallX: Float = if (side == 0) {
                                    player.y / cellSize + correctedWallDist * rayDirY
                                } else {
                                    player.x / cellSize + correctedWallDist * rayDirX
                                }
                                val finalWallX = wallX - floor(wallX)

                                var texX = (finalWallX * worldTextureSize).toInt()
                                if ((side == 0 && rayDirX > 0) || (side == 1 && rayDirY < 0)) {
                                    texX = worldTextureSize - texX - 1
                                }

                                val step = worldTextureSize.toFloat() / lineHeight
                                var texPos = (drawStart - height / 2 + lineHeight / 2) * step

                                val wallTexture = walls.wallTextures[wallTextureIndex]
                                    ?: error("Wall texture not found $wallTextureIndex")

                                val intensity =
                                    1.0f - ((correctedWallDist / 20.0f) + 0.4f * side).coerceAtMost(
                                        1f
                                    )

                                for (y in drawStart until drawEnd) {
                                    val texY =
                                        (texPos.toInt() and (worldTextureSize - 1))  // No change required here
                                    texPos += step

                                    val texIndex =
                                        (texY * worldTextureSize + texX) * 3  // Move outside if index doesn't change often
                                    val r = wallTexture[texIndex]
                                    val g = wallTexture[texIndex + 1]
                                    val b = wallTexture[texIndex + 2]

                                    bitmap.setRGB(
                                        x, y,
                                        r.darkenColor(intensity),
                                        g.darkenColor(intensity),
                                        b.darkenColor(intensity)
                                    )
                                }

                            }

                            if (castCeiling) {
                                // Ceiling casting
                                if (drawStart > 0) {
                                    for (y in 0 until drawStart) {
                                        val ceilingDistance = height.toFloat() / (height - 2.0f * y)

                                        val ceilingX =
                                            player.x / cellSize + ceilingDistance * rayDirX
                                        val ceilingY =
                                            player.y / cellSize + ceilingDistance * rayDirY

                                        val ceilingTexX =
                                            ((ceilingX - floor(ceilingX)) * worldTextureSize).toInt()
                                        val ceilingTexY =
                                            ((ceilingY - floor(ceilingY)) * worldTextureSize).toInt()

                                        val texIndex =
                                            (ceilingTexY * worldTextureSize + ceilingTexX) * 3
                                        if (texIndex >= 0) {
                                            val r = cellingTexture[texIndex]
                                            val g = cellingTexture[texIndex + 1]
                                            val b = cellingTexture[texIndex + 2]


                                            bitmap.setRGB(
                                                x,
                                                y,
                                                r.darkenColor(0.5f),
                                                g.darkenColor(0.5f),
                                                b.darkenColor(0.5f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Floor casting
                            if (castFloor) {
                                if (drawEnd < height) {
                                    for (y in drawEnd until height) {
                                        val floorDistance = height.toFloat() / (2.0f * y - height)

                                        val floorX = player.x / cellSize + floorDistance * rayDirX
                                        val floorY = player.y / cellSize + floorDistance * rayDirY

                                        val floorTexX =
                                            (floorX * worldTextureSize % worldTextureSize).toInt()
                                        val floorTexY =
                                            (floorY * worldTextureSize % worldTextureSize).toInt()

                                        val texIndex =
                                            (floorTexY * worldTextureSize + floorTexX) * 3
                                        if (texIndex >= 0) {
                                            val r = floorTexture[texIndex]
                                            val g = floorTexture[texIndex + 1]
                                            val b = floorTexture[texIndex + 2]

                                            bitmap.setRGB(
                                                x,
                                                y,
                                                r.darkenColor(0.5f),
                                                g.darkenColor(0.5f),
                                                b.darkenColor(0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                deferredResults.awaitAll()
            }
        }

    private fun movePlayer(pressedKeys: Set<Moves>) {
        var dx = 0f
        var dy = 0f
        var dr = 0f

        if (Moves.UP in pressedKeys) { // Right arrow key
            dx += moveStep * cos(player.rotationRad)
            dy += moveStep * sin(player.rotationRad)
        }
        if (Moves.DOWN in pressedKeys) { // Left arrow key
            dx -= moveStep * cos(player.rotationRad)
            dy -= moveStep * sin(player.rotationRad)
        }

        if (Moves.MOVE_LEFT in pressedKeys) { // A key
            dx -= moveStep * cos(player.rotationRad + 90.toRadian())
            dy -= moveStep * sin(player.rotationRad + 90.toRadian())
        }

        if (Moves.MOVE_RIGHT in pressedKeys) { // D key
            dx += moveStep * cos(player.rotationRad + 90.toRadian())
            dy += moveStep * sin(player.rotationRad + 90.toRadian())
        }

        if (Moves.LEFT in pressedKeys) { // Up arrow key
            dr -= rotationStepRad
        }
        if (Moves.RIGHT in pressedKeys) { // Down arrow key
            dr += rotationStepRad
        }

        if (Moves.SHOOT in pressedKeys) { // Space key
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
        player.animate(state = PlayerState.SHOOTING, map = currentMap, cellSize = cellSize)

        if (player.shootingFrame == 0) {
            sound.playSound(R.raw.gunshot1)
        }

        enemies.forEach { enemy ->
            if (player.distanceTo(enemy) < 10 && player.inShotAngle(enemy)) {
                if (enemy.state != PlayerState.DEAD) {
                    enemy.state = PlayerState.DYING
                    sound.playSound(R.raw.mandeathscream)
                }
            }
        }
    }

    fun dispose() {
        sound.stopMusic()
        sound.release()
    }
}






