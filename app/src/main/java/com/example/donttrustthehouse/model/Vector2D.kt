package com.example.donttrustthehouse.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2D(val x: Float = 0f, val y: Float = 0f) {
    fun distanceTo(other: Vector2D): Float {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    fun length(): Float = sqrt(x * x + y * y)

    fun normalized(): Vector2D {
        val len = length()
        return if (len > 0.0001f) Vector2D(x / len, y / len) else Vector2D(0f, 0f)
    }

    fun lerp(target: Vector2D, t: Float): Vector2D {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector2D(
            x + (target.x - x) * clampedT,
            y + (target.y - y) * clampedT
        )
    }

    operator fun plus(other: Vector2D): Vector2D = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D): Vector2D = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vector2D = Vector2D(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vector2D = if (scalar != 0f) Vector2D(x / scalar, y / scalar) else Vector2D(0f, 0f)

    fun dot(other: Vector2D): Float = x * other.x + y * other.y

    fun angleTo(other: Vector2D): Float {
        val angle = atan2(other.y, other.x) - atan2(y, x)
        return angle
    }

    companion object {
        val ZERO = Vector2D(0f, 0f)
    }
}
