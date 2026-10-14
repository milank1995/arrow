package com.arrowescape.puzzle.core.model

data class GridCoordinate(val x: Int, val y: Int) {
    fun step(direction: Direction, distance: Int = 1): GridCoordinate {
        return GridCoordinate(x + direction.dx * distance, y + direction.dy * distance)
    }

    fun toIndex(width: Int): Int = y * width + x

    companion object {
        fun fromIndex(index: Int, width: Int): GridCoordinate {
            return GridCoordinate(x = index % width, y = index / width)
        }
    }
}
