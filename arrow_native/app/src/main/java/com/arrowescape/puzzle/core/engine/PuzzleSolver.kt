package com.arrowescape.puzzle.core.engine

import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.GridCoordinate

object PuzzleSolver {

    /**
     * Checks if the arrow has a clear ray from its head along its direction to the grid boundary.
     */
    fun canMoveOut(arrow: Arrow, occupancy: GridOccupancyMap): Boolean {
        if (arrow.state == ArrowState.CLEARED || arrow.state == ArrowState.FLYING_OUT) {
            return false
        }

        var rayCoord = arrow.head.step(arrow.direction)
        while (occupancy.isInside(rayCoord)) {
            val blockingArrow = occupancy.get(rayCoord)
            // If the ray hits a cell that belongs to another uncleared arrow, the path is blocked
            if (blockingArrow != null && blockingArrow.id != arrow.id && 
                blockingArrow.state != ArrowState.CLEARED && 
                blockingArrow.state != ArrowState.FLYING_OUT) {
                return false
            }
            rayCoord = rayCoord.step(arrow.direction)
        }
        return true
    }

    /**
     * Returns the first arrow blocking the given arrow's path, or null if free.
     */
    fun findBlockingArrow(arrow: Arrow, occupancy: GridOccupancyMap): Arrow? {
        var rayCoord = arrow.head.step(arrow.direction)
        while (occupancy.isInside(rayCoord)) {
            val blocker = occupancy.get(rayCoord)
            if (blocker != null && blocker.id != arrow.id &&
                blocker.state != ArrowState.CLEARED &&
                blocker.state != ArrowState.FLYING_OUT) {
                return blocker
            }
            rayCoord = rayCoord.step(arrow.direction)
        }
        return null
    }

    /**
     * Finds all arrows that can currently fly out without collisions.
     */
    fun findAllMovableArrows(arrows: List<Arrow>, occupancy: GridOccupancyMap): List<Arrow> {
        return arrows.filter { canMoveOut(it, occupancy) }
    }

    /**
     * Returns the recommended arrow for the Hint booster.
     */
    fun getBestHint(arrows: List<Arrow>, occupancy: GridOccupancyMap): Arrow? {
        val movable = findAllMovableArrows(arrows, occupancy)
        if (movable.isEmpty()) return null

        // Pick the arrow that unlocks the most other arrows once cleared
        return movable.maxByOrNull { candidate ->
            val simulatedOccupancy = GridOccupancyMap(occupancy.width, occupancy.height).apply {
                rebuild(arrows)
                release(candidate)
            }
            findAllMovableArrows(arrows.filter { it.id != candidate.id }, simulatedOccupancy).size
        } ?: movable.firstOrNull()
    }

    /**
     * Finds the best arrow to clear with the Magic Wand booster (the blocker holding back the most arrows).
     */
    fun getMagicWandTarget(arrows: List<Arrow>, occupancy: GridOccupancyMap): Arrow? {
        // Collect all blockers
        val uncleared = arrows.filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
        val blockerCounts = mutableMapOf<String, Int>()

        for (arrow in uncleared) {
            val blocker = findBlockingArrow(arrow, occupancy)
            if (blocker != null) {
                blockerCounts[blocker.id] = (blockerCounts[blocker.id] ?: 0) + 1
            }
        }

        val topBlockerId = blockerCounts.maxByOrNull { it.value }?.key
        return uncleared.firstOrNull { it.id == topBlockerId } ?: uncleared.firstOrNull()
    }
}
