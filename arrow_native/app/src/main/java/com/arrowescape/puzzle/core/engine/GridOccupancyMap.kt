package com.arrowescape.puzzle.core.engine

import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.GridCoordinate

class GridOccupancyMap(val width: Int, val height: Int) {
    private val grid: Array<Arrow?> = arrayOfNulls(width * height)

    fun isInside(coord: GridCoordinate): Boolean {
        return coord.x in 0 until width && coord.y in 0 until height
    }

    fun get(coord: GridCoordinate): Arrow? {
        if (!isInside(coord)) return null
        return grid[coord.y * width + coord.x]
    }

    fun isCellFree(coord: GridCoordinate): Boolean {
        if (!isInside(coord)) return true
        val arrow = grid[coord.y * width + coord.x]
        return arrow == null || arrow.state == ArrowState.CLEARED || arrow.state == ArrowState.FLYING_OUT
    }

    fun occupy(arrow: Arrow) {
        for (point in arrow.allPoints) {
            if (isInside(point)) {
                grid[point.y * width + point.x] = arrow
            }
        }
    }

    fun release(arrow: Arrow) {
        for (point in arrow.allPoints) {
            if (isInside(point)) {
                val index = point.y * width + point.x
                if (grid[index]?.id == arrow.id) {
                    grid[index] = null
                }
            }
        }
    }

    fun rebuild(arrows: List<Arrow>) {
        grid.fill(null)
        for (arrow in arrows) {
            if (arrow.state != ArrowState.CLEARED && arrow.state != ArrowState.FLYING_OUT) {
                occupy(arrow)
            }
        }
    }
}
