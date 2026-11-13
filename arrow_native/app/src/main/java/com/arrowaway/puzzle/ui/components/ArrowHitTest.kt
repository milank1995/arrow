package com.arrowaway.puzzle.ui.components

import androidx.compose.ui.geometry.Offset
import com.arrowaway.puzzle.core.engine.GridOccupancyMap
import com.arrowaway.puzzle.core.engine.PuzzleSolver
import com.arrowaway.puzzle.core.model.Arrow
import com.arrowaway.puzzle.core.model.ArrowState
import com.arrowaway.puzzle.core.model.GridCoordinate
import kotlin.math.floor
import kotlin.math.hypot

/**
 * Decides which arrow a touch point selects on the board (board-local pixels, same space the
 * arrows are drawn in).
 *
 * Order of rules:
 * 1. Cell authority: a touch inside a cell owned by an active arrow selects that arrow.
 *    Exception (tap assist): if that arrow is BLOCKED and the touch sits in the outer
 *    [BORDER_BAND] of the cell right next to an arrow that CAN move, the movable neighbour
 *    wins. A touch in the middle of a blocked arrow still selects it (a deliberate wrong move).
 * 2. Arrowhead triangle that pokes into an empty cell.
 * 3. Empty-space search: nearest arrow geometry within the search radius. With tap assist on,
 *    blocked arrows must be [BLOCKED_DISTANCE_PENALTY]x closer than movable ones to win.
 *
 * Tap assist must be off when the player is choosing an exact arrow (Hammer booster).
 */
internal object ArrowHitTest {

    /** Outer part of a cell (fraction of its size) where a movable neighbour can take the tap. */
    const val BORDER_BAND = 0.25f

    /** In empty space, a blocked arrow's distance counts this much more than a movable one's. */
    const val BLOCKED_DISTANCE_PENALTY = 1.35f

    fun findTarget(
        touchPoint: Offset,
        origin: Offset,
        arrows: List<Arrow>,
        cellSize: Float,
        headSize: Float,
        minTouchRadius: Float,
        occupancy: GridOccupancyMap? = null,
        preferMovable: Boolean = false
    ): Arrow? {
        val activeArrows = arrows.filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
        if (activeArrows.isEmpty() || cellSize <= 0f) return null

        val canMove: (Arrow) -> Boolean = { a -> occupancy != null && PuzzleSolver.canMoveOut(a, occupancy) }

        val relX = touchPoint.x - origin.x
        val relY = touchPoint.y - origin.y
        val cellX = floor(relX / cellSize).toInt()
        val cellY = floor(relY / cellSize).toInt()
        val touchedCoord = GridCoordinate(cellX, cellY)

        fun activeAt(coord: GridCoordinate): Arrow? =
            occupancy?.get(coord)?.takeIf { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
                ?: activeArrows.firstOrNull { it.isPointInside(coord) }

        // 1. Cell authority (with border assist toward movable neighbours)
        val cellOccupant = activeAt(touchedCoord)
        if (cellOccupant != null) {
            if (preferMovable && occupancy != null && !canMove(cellOccupant)) {
                val fx = relX / cellSize - cellX
                val fy = relY / cellSize - cellY
                var best: Arrow? = null
                var bestDist = BORDER_BAND
                for (nx in -1..1) for (ny in -1..1) {
                    if (nx == 0 && ny == 0) continue
                    val dx = when (nx) { -1 -> fx; 1 -> 1f - fx; else -> 0f }
                    val dy = when (ny) { -1 -> fy; 1 -> 1f - fy; else -> 0f }
                    val d = hypot(dx, dy) // distance to that neighbour cell, in cell units
                    if (d >= bestDist) continue
                    val neighbour = activeAt(GridCoordinate(cellX + nx, cellY + ny)) ?: continue
                    if (neighbour.id == cellOccupant.id || !canMove(neighbour)) continue
                    best = neighbour
                    bestDist = d
                }
                if (best != null) return best
            }
            return cellOccupant
        }

        val strokeWidth = (cellSize * 0.20f).coerceIn(1.0f, minOf(cellSize * 0.35f, headSize / 2.2f))
        val headWidth = headSize.coerceIn(strokeWidth * 2.2f, strokeWidth * 3.2f)
        val headLength = headWidth * 0.88f
        val tipForward = cellSize * 0.35f
        val tailOffset = (cellSize * 0.32f).coerceAtLeast(strokeWidth * 0.45f)
        val stemOverlap = minOf(headLength * 0.20f, strokeWidth * 0.40f)
        val stemHeadOffset = (tipForward - headLength + stemOverlap).coerceAtLeast(0f)

        fun headPoints(arrow: Arrow): Triple<Offset, Offset, Offset> {
            val headCenter = cellCenter(arrow.head, origin, cellSize)
            val dirX = arrow.direction.dx.toFloat()
            val dirY = arrow.direction.dy.toFloat()
            val perpX = -dirY
            val perpY = dirX
            val tip = Offset(headCenter.x + dirX * tipForward, headCenter.y + dirY * tipForward)
            val wingRight = Offset(
                tip.x - dirX * headLength + perpX * (headWidth * 0.50f),
                tip.y - dirY * headLength + perpY * (headWidth * 0.50f)
            )
            val wingLeft = Offset(
                tip.x - dirX * headLength - perpX * (headWidth * 0.50f),
                tip.y - dirY * headLength - perpY * (headWidth * 0.50f)
            )
            return Triple(tip, wingRight, wingLeft)
        }

        // 2. Arrowhead triangle reaching into empty space
        for (arrow in activeArrows) {
            val (tip, wingRight, wingLeft) = headPoints(arrow)
            if (isPointInTriangle(touchPoint, tip, wingRight, wingLeft)) return arrow
        }

        // 3. Empty-space search
        val maxSearchRadius = maxOf(cellSize * 0.75f, minTouchRadius)
        var bestArrow: Arrow? = null
        var bestScore = Float.MAX_VALUE
        for (arrow in activeArrows) {
            val headCenter = cellCenter(arrow.head, origin, cellSize)
            val (tip, wingRight, wingLeft) = headPoints(arrow)
            val headDist = minOf(
                distanceToSegment(touchPoint, headCenter, tip),
                distanceToSegment(touchPoint, tip, wingLeft),
                distanceToSegment(touchPoint, tip, wingRight),
                distanceToSegment(touchPoint, wingLeft, wingRight)
            )
            val body = arrow.body
            val stemDist = if (body.size <= 1) {
                val dir = Offset(arrow.direction.dx.toFloat(), arrow.direction.dy.toFloat())
                distanceToSegment(touchPoint, headCenter - dir * tailOffset, headCenter + dir * stemHeadOffset)
            } else {
                var minSeg = Float.MAX_VALUE
                for (i in 0 until body.size - 1) {
                    val d = distanceToSegment(touchPoint, cellCenter(body[i], origin, cellSize), cellCenter(body[i + 1], origin, cellSize))
                    if (d < minSeg) minSeg = d
                }
                minSeg
            }
            val dist = minOf(headDist, stemDist)
            if (dist > maxSearchRadius) continue
            val score = if (preferMovable && !canMove(arrow)) dist * BLOCKED_DISTANCE_PENALTY else dist
            if (score < bestScore) {
                bestScore = score
                bestArrow = arrow
            }
        }
        return bestArrow
    }

    private fun cellCenter(c: GridCoordinate, origin: Offset, cellSize: Float) =
        Offset(origin.x + (c.x + 0.5f) * cellSize, origin.y + (c.y + 0.5f) * cellSize)

    fun isPointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
        val cp1 = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
        val cp2 = (c.x - b.x) * (p.y - b.y) - (c.y - b.y) * (p.x - b.x)
        val cp3 = (a.x - c.x) * (p.y - c.y) - (a.y - c.y) * (p.x - c.x)
        val hasNeg = (cp1 < -0.001f) || (cp2 < -0.001f) || (cp3 < -0.001f)
        val hasPos = (cp1 > 0.001f) || (cp2 > 0.001f) || (cp3 > 0.001f)
        return !(hasNeg && hasPos)
    }

    fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
        val vx = b.x - a.x
        val vy = b.y - a.y
        val segLenSq = vx * vx + vy * vy
        if (segLenSq == 0f) return hypot(p.x - a.x, p.y - a.y)
        val t = (((p.x - a.x) * vx + (p.y - a.y) * vy) / segLenSq).coerceIn(0f, 1f)
        return hypot(p.x - (a.x + t * vx), p.y - (a.y + t * vy))
    }
}
