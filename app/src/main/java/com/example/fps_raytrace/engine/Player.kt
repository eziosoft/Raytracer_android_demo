package com.example.fps_raytrace.engine

import android.util.Log
import com.example.fps_raytrace.R
import com.example.fps_raytrace.engine.PlayerState.*
import com.example.fps_raytrace.engine.utils.PI
import com.example.fps_raytrace.engine.utils.Sound
import com.example.fps_raytrace.engine.utils.WallType
import com.example.fps_raytrace.engine.utils.isWall
import com.example.fps_raytrace.engine.utils.normalizeAngle
import com.example.fps_raytrace.engine.utils.toRadian
import com.example.fps_raytrace.maps.Map
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
    val sound: Sound? = null
)

fun Player.animate(
    newState: PlayerState? = null,
    map: Map,
    cellSize: Int,
    mainPlayer: Player? = null,
) {
    timer++

    newState?.let {
        this.state = it
    }

    when (this.state) {
        WALKING -> {
            walk()
            if (!isMainPlayer) {
                mainPlayer?.let {
                    walkRandom(map, cellSize, mainPlayer = it)
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


    if (!this.isMainPlayer && this.state != DYING && this.state != DEAD) {
        distanceTo(mainPlayer!!).let {
            if (it < SHOOT_DISTANCE) {
                this.state = SHOOTING
            } else {
                this.state = WALKING
            }
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


fun Player.walkRandom(map: Map, cellSize: Int, padding: Float = 0.2f, mainPlayer: Player) {
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
    if (this.distanceTo(mainPlayer) < 10f) {
        rotation = this.angleTo(mainPlayer).normalizeAngle()
    }

    // Update rotation
    this.rotationRad = rotation
}

