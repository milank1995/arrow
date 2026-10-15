package com.arrowescape.puzzle.core.model

enum class ArrowState {
    IDLE,
    HINTED,
    WRONG_FLASH,
    FLYING_OUT,
    CLEARED
}

data class Arrow(
    val id: String,
    val head: GridCoordinate,
    val direction: Direction,
    val body: List<GridCoordinate>,
    val bendCount: Int = 0,
    val colorIndex: Int = 0,
    val state: ArrowState = ArrowState.IDLE,
    val flightProgress: Float = 0f,
    val isHighlighted: Boolean = false
) {
    /**
     * All occupied grid coordinates (tail + bend segments + head).
     */
    val allPoints: List<GridCoordinate>
        get() = if (body.contains(head)) body else body + head

    fun isPointInside(coord: GridCoordinate): Boolean {
        return allPoints.contains(coord)
    }

    fun copyWithState(
        newState: ArrowState,
        progress: Float = 0f,
        highlighted: Boolean = isHighlighted
    ): Arrow {
        return copy(state = newState, flightProgress = progress, isHighlighted = highlighted)
    }
}
