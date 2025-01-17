package com.example.fps_raytrace.engine

import aStar
import android.util.Log
import com.example.fps_raytrace.Const.ENEMIES_CAN_SHOOT
import com.example.fps_raytrace.Const.ENEMIES_WALK_TO_PLAYER
import com.example.fps_raytrace.Const.PLAYER_A_START_WALK
import com.example.fps_raytrace.Const.SHOOT_ENEMY_DAMAGE
import com.example.fps_raytrace.R
import com.example.fps_raytrace.engine.PlayerState.DEAD
import com.example.fps_raytrace.engine.PlayerState.DYING
import com.example.fps_raytrace.engine.PlayerState.IDLE
import com.example.fps_raytrace.engine.PlayerState.SHOOTING
import com.example.fps_raytrace.engine.PlayerState.WALKING
import com.example.fps_raytrace.engine.utils.PI
import com.example.fps_raytrace.engine.utils.Sound
import com.example.fps_raytrace.engine.utils.WallType
import com.example.fps_raytrace.engine.utils.isWall
import com.example.fps_raytrace.engine.utils.normalizeAngle
import com.example.fps_raytrace.engine.utils.toRadian
import com.example.fps_raytrace.maps.Map
import com.example.fps_raytrace.maps.convertMapTo2DArrayForA_Star
import com.example.fps_raytrace.maps.findArrayIndexesFromPosition
import com.example.fps_raytrace.maps.findPositionFromArrayIndexes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

private const val SHOOT_DISTANCE = 5f

enum class PlayerState {
    IDLE,
    WALKING,
    SHOOTING,
    DYING,
    DEAD
}

data class Player(
    val isMainPlayer: Boolean,
    var mainPlayerShootingFrame: Int = 0,
    var state: PlayerState = IDLE,
    var health: Int = 100,
    var timer: Int = 0,
    var x: Float,
    var y: Float,
    var rotationRad: Float = 0f,
    var walkingFrame: Int = 0,
    var dyingFrame: Int = 0,
    var shootingFrame: Int = 0,
    val sound: Sound? = null,
    var goToPosition: Pair<Float, Float>? = null
)

fun Player.animate(
    newState: PlayerState? = null,
    map: Map,
    cellSize: Int,
    mainPlayer: Player? = null,
    isWallBetween: (() -> Boolean)? = null,
) {
    timer++

    if (isMainPlayer) {
        health = health.coerceIn(0..100)
        if (timer % 50 == 0 && health < 100) health++ // health regeneration

    }

    newState?.let {
        this.state = it
    }

    when (this.state) {
        WALKING -> {
            walk()
            if (!isMainPlayer) {
                walkRandom(map, cellSize, mainPlayer = mainPlayer)
            } else {

                if (PLAYER_A_START_WALK) {
                    if (goToPosition == null) {
                        aStar(map, cellSize, callBack = { player, currentPosition, nextPosition ->
                            player.goToPosition = nextPosition
                        })
                    }

                    goToPosition?.let { position ->
                        if (walkToPosition(map, cellSize, position)) {
                            aStar(map, cellSize, callBack = { player, currentPosition, nextPosition ->
                                player.goToPosition = nextPosition
                            })
                        }
                    }
                }
            }
        }

        SHOOTING -> {
            if (isMainPlayer) {
                mainPlayerShoot()
            } else {
                shoot()
            }
        }

        DYING -> dying(5)
        DEAD -> this.dead()
        IDLE -> {}//TODO()
    }


    if (ENEMIES_CAN_SHOOT) {
        if (!this.isMainPlayer && this.state != DYING && this.state != DEAD) {
            distanceTo(mainPlayer!!).let { distance ->
                if (distance < SHOOT_DISTANCE && isWallBetween?.invoke() == false) {
                    this.state = SHOOTING
                    if (shootingFrame == 2) {
                        mainPlayer.health -= SHOOT_ENEMY_DAMAGE
                    }
                } else {
                    this.state = WALKING
                }
            }
        }
    }


    if (isMainPlayer) {
        if (timer % 10 == 0) {
//            aStar(map, cellSize, callBack = { player, currentPosition, nextPosition ->
//                Log.d("Player", "currentPosition: $currentPosition, nextPosition: $nextPosition")
//                val nextRotationRad = angleTo(Player(false, x = nextPosition.first, y = nextPosition.second))
//
//                rotationRad = nextRotationRad
//                val dx = 0.1f * cos(player.rotationRad)
//                val dy = 0.1f * sin(player.rotationRad)
//                player.x += dx
//                player.y += dy
//                player.walkToPosition(map, cellSize, nextPosition)
//            })
        }
    }


}

private var mapForAStar: Array<Array<Int>>? = null
private var aStarJob: Job? = null
private val aStarScope = CoroutineScope(Dispatchers.IO)

private fun Player.aStar(
    map: Map,
    cellSize: Int,
    gotoCell: Pair<Int, Int> = Pair(42, 22),
    callBack: (
        player: Player,
        currentPosition: Pair<Float, Float>,
        nextPosition: Pair<Float, Float>
    ) -> Unit
) {
    val player = this
    aStarJob = aStarScope.launch {
        if (mapForAStar == null) {
            mapForAStar = map.convertMapTo2DArrayForA_Star()
        }

        val playerArrayPosition = findArrayIndexesFromPosition(player.x, player.y, cellSize)
        val start = Pair(playerArrayPosition.first, playerArrayPosition.second)

        val path = aStar(start, gotoCell, mapForAStar!!)

        val nextPosition = path?.get(1)?.findPositionFromArrayIndexes(cellSize)
        nextPosition?.let {
            callBack(player, Pair(x, y), it)
        }
    }
}

private fun Player.walk() {
    if (this.timer % 7 == 0) {
        this.walkingFrame = (this.walkingFrame + 1) % 4
    }
}

private fun Player.mainPlayerShoot(frameCount: Int = 6) {
    state = SHOOTING

    if (mainPlayerShootingFrame >= frameCount - 1) {
        state = WALKING
        mainPlayerShootingFrame = 0
        return
    }

    if (this.timer % 10 == 0) {
        this.mainPlayerShootingFrame++
    }
}

private fun Player.shoot() {
    state = SHOOTING

    if (this.timer % 10 == 0) {
        shootingFrame = (shootingFrame + 1) % 3

        if (shootingFrame == 2) {
            sound?.playSound(R.raw.gunshot1)
        }
    }
}

private fun Player.dying(frameCount: Int) {
    state = DYING

    if (this.dyingFrame == 0) {
        sound?.playSound(R.raw.mandeathscream)
    }
    if (this.dyingFrame >= frameCount - 1) {
        this.dead()
        return
    }
    if (this.timer % 7 == 0) {
        this.dyingFrame++
    }
}

private fun Player.dead() {
    this.state = DEAD
    dyingFrame = 4
}

fun Player.distanceTo(player: Player): Float {
    return kotlin.math.sqrt((this.x - player.x) * (this.x - player.x) + (this.y - player.y) * (this.y - player.y))
}

fun Player.angleTo(player: Player): Float {
    return kotlin.math.atan2(player.y - this.y, player.x - this.x)
}

fun Player.inShotAngle(player: Player): Boolean {
    val angle = this.angleTo(player)
    var diff = this.rotationRad - angle

    // Normalize the difference to the range [-π, π]
    diff = (diff + PI).rem(2 * PI)
    if (diff < 0) diff += 2 * PI
    diff -= PI

    return kotlin.math.abs(diff) < 10.toRadian()
}


fun Player.walkRandom(map: Map, cellSize: Int, padding: Float = 0.2f, mainPlayer: Player? = null) {
    // Calculate movement deltas based on current rotation
    val dx = 0.1f * cos(this.rotationRad)
    val dy = 0.1f * sin(this.rotationRad)

    // Calculate new positions with buffer applied
    val newX = this.x + dx
    val newY = this.y + dy

    // Initialize rotation to current rotation
    var rotation = this.rotationRad

    // Check for wall collisions and apply buffer zone
    if (isWall(
            newX + dx.sign * padding,
            this.y,
            map.MAP,
            map.MAP_X,
            map.MAP_Y,
            cellSize
        ) == WallType.NONE
    ) {
        this.x = newX
    } else {
        rotation = (rotation + 10.toRadian()).normalizeAngle()
    }

    if (isWall(
            this.x,
            newY + dy.sign * padding,
            map.MAP,
            map.MAP_X,
            map.MAP_Y,
            cellSize
        ) == WallType.NONE
    ) {
        this.y = newY
    } else {
        rotation = (rotation + 10.toRadian()).normalizeAngle()
    }

    // walk towards the main player
    if (ENEMIES_WALK_TO_PLAYER) {
        mainPlayer?.let {
            if (this.distanceTo(mainPlayer) < 10f) {
                rotation = this.angleTo(mainPlayer).normalizeAngle()
            }
        }
    }

    // Update rotation
    this.rotationRad = rotation
}

/**
 * Walk in straight line to a specific position on the map
 */
private fun Player.walkToPosition(
    map: Map,
    cellSize: Int,
    goToPosition: Pair<Float, Float>,
    padding: Float = 0.2f,
): Boolean {
    val dx = goToPosition.first - this.x
    val dy = goToPosition.second - this.y
    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

    if (distance > 0.1f) {
        this.rotationRad = kotlin.math.atan2(dy, dx)

        val stepX = (dx / distance) * 0.1f
        val stepY = (dy / distance) * 0.1f

        val newX = this.x + stepX
        val newY = this.y + stepY

        if (!isMainPlayer) {
            if (isWall(newX + stepX.sign * padding, this.y, map.MAP, map.MAP_X, map.MAP_Y, cellSize) == WallType.NONE) {
                this.x = newX
            }

            if (isWall(this.x, newY + stepY.sign * padding, map.MAP, map.MAP_X, map.MAP_Y, cellSize) == WallType.NONE) {
                this.y = newY
            }
        } else {
            this.x = newX
            this.y = newY
        }
        return false
    } else {
        return true
    }
}

