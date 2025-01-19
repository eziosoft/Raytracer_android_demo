package com.example.fps_raytrace.engine

import com.example.fps_raytrace.engine.utils.toRadian

object Const {
    const val LOG_STATS = false

    const val DRAW_MAP = false
    const val MAP_CELL_SIZE = 2

    const val SHOOT_ENEMY_DAMAGE = 1
    const val SHOOT_PLAYER_DAMAGE = 100

    const val ENEMIES_CAN_SHOOT = false
    const val ENEMIES_WALK_TO_PLAYER = false
    const val PLAYER_A_START_WALK = false

    const val PLAYER_SPEED = 0.2f
    const val PLAYER_ROTATION_SPEED_RAD = 114.592f // 2 degrees
    const val PLAYER_FOV = 1.0472f // 60 degrees
}