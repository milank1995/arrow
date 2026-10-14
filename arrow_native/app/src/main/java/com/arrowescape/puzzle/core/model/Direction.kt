package com.arrowescape.puzzle.core.model

enum class Direction(val dx: Int, val dy: Int, val angleDegrees: Float) {
    UP(0, -1, 0f),
    RIGHT(1, 0, 90f),
    DOWN(0, 1, 180f),
    LEFT(-1, 0, 270f);

    val opposite: Direction
        get() = when (this) {
            UP -> DOWN
            RIGHT -> LEFT
            DOWN -> UP
            LEFT -> RIGHT
        }

    val isHorizontal: Boolean
        get() = this == LEFT || this == RIGHT

    val isVertical: Boolean
        get() = this == UP || this == DOWN

    fun perpendiculars(): List<Direction> = when (this) {
        UP, DOWN -> listOf(LEFT, RIGHT)
        LEFT, RIGHT -> listOf(UP, DOWN)
    }

    companion object {
        fun fromDelta(dx: Int, dy: Int): Direction? {
            return entries.firstOrNull { it.dx == dx && it.dy == dy }
        }
    }
}
