package com.arrowaway.puzzle

import androidx.compose.ui.geometry.Offset
import com.arrowaway.puzzle.core.engine.GridOccupancyMap
import com.arrowaway.puzzle.core.engine.PuzzleSolver
import com.arrowaway.puzzle.core.model.Arrow
import com.arrowaway.puzzle.core.model.ArrowState
import com.arrowaway.puzzle.core.model.Direction
import com.arrowaway.puzzle.core.model.GridCoordinate
import com.arrowaway.puzzle.ui.components.ArrowHitTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tap targeting on the board (cell = 100 px, origin at 0,0, y grows downward like the canvas).
 */
class ArrowHitTestTest {

    private val cell = 100f
    private val head = 52f
    private val origin = Offset.Zero

    private fun arrow(id: String, x: Int, y: Int, dir: Direction) =
        Arrow(id = id, head = GridCoordinate(x, y), direction = dir, body = listOf(GridCoordinate(x, y)))

    private fun hit(arrows: List<Arrow>, map: GridOccupancyMap, x: Float, y: Float, assist: Boolean, minRadius: Float = 24f) =
        ArrowHitTest.findTarget(Offset(x, y), origin, arrows, cell, head, minRadius, map, assist)

    // Row of three: A (blocked, points at B) | B (free, points up and out) | C (blocked, points at B)
    private val a = arrow("A", 0, 0, Direction.RIGHT)
    private val b = arrow("B", 1, 0, Direction.UP)
    private val c = arrow("C", 2, 0, Direction.LEFT)
    private val row = listOf(a, b, c)
    private val rowMap = GridOccupancyMap(3, 1).apply { rebuild(row) }

    @Test
    fun boardSetupIsAsIntended() {
        assertFalse(PuzzleSolver.canMoveOut(a, rowMap))
        assertTrue(PuzzleSolver.canMoveOut(b, rowMap))
        assertFalse(PuzzleSolver.canMoveOut(c, rowMap))
    }

    @Test
    fun tapInTheMiddleOfAnArrowAlwaysSelectsIt() {
        for (assist in listOf(false, true)) {
            assertEquals("A centre, assist=$assist", "A", hit(row, rowMap, 50f, 50f, assist)?.id)
            assertEquals("B centre, assist=$assist", "B", hit(row, rowMap, 150f, 50f, assist)?.id)
            assertEquals("C centre, assist=$assist", "C", hit(row, rowMap, 250f, 50f, assist)?.id)
        }
    }

    @Test
    fun borderTapOnBlockedArrowGoesToMovableNeighbourOnlyWithAssist() {
        // 10 px from A's right edge, next to movable B
        assertEquals("B", hit(row, rowMap, 90f, 50f, assist = true)?.id)
        // 10 px from C's left edge, next to movable B
        assertEquals("B", hit(row, rowMap, 210f, 50f, assist = true)?.id)
        // Without assist (e.g. Hammer mode) the exact cell wins
        assertEquals("A", hit(row, rowMap, 90f, 50f, assist = false)?.id)
        assertEquals("C", hit(row, rowMap, 210f, 50f, assist = false)?.id)
    }

    @Test
    fun tapOutsideTheBorderBandStaysOnTheBlockedArrow() {
        // 30 px from the edge = 0.30 cell, outside the 0.25 band
        assertEquals("A", hit(row, rowMap, 70f, 50f, assist = true)?.id)
        assertEquals("C", hit(row, rowMap, 230f, 50f, assist = true)?.id)
    }

    @Test
    fun movableArrowIsNeverRedirected() {
        // B's own border next to blocked A and C: B keeps it
        assertEquals("B", hit(row, rowMap, 105f, 50f, assist = true)?.id)
        assertEquals("B", hit(row, rowMap, 195f, 50f, assist = true)?.id)
    }

    @Test
    fun blockedNeighbourNeverStealsATap() {
        // Two blocked arrows side by side (D blocked by E, E blocked by the wall arrow F)
        val d = arrow("D", 0, 0, Direction.RIGHT)
        val e = arrow("E", 1, 0, Direction.RIGHT)
        val f = arrow("F", 2, 0, Direction.LEFT)
        val arrows = listOf(d, e, f)
        val map = GridOccupancyMap(3, 1).apply { rebuild(arrows) }
        assertFalse(PuzzleSolver.canMoveOut(d, map))
        assertFalse(PuzzleSolver.canMoveOut(e, map))
        assertEquals("D", hit(arrows, map, 95f, 50f, assist = true)?.id)
    }

    @Test
    fun emptySpaceTapPrefersMovableArrowOnlyWithAssist() {
        // G (free, points left out of the board) | empty | H (blocked by G, points left)
        val g = arrow("G", 0, 0, Direction.LEFT)
        val h = arrow("H", 2, 0, Direction.LEFT)
        val arrows = listOf(g, h)
        val map = GridOccupancyMap(3, 1).apply { rebuild(arrows) }
        assertTrue(PuzzleSolver.canMoveOut(g, map))
        assertFalse(PuzzleSolver.canMoveOut(h, map))
        // Middle of the empty cell: H's head is slightly closer (65 px) than G's tail (68 px)
        assertEquals("H", hit(arrows, map, 150f, 50f, assist = false)?.id)
        assertEquals("G", hit(arrows, map, 150f, 50f, assist = true)?.id)
    }

    @Test
    fun farFromEverythingSelectsNothing() {
        val g = arrow("G", 0, 0, Direction.LEFT)
        val map = GridOccupancyMap(5, 1).apply { rebuild(listOf(g)) }
        assertNull(hit(listOf(g), map, 450f, 50f, assist = true))
    }

    @Test
    fun clearedAndFlyingArrowsAreIgnored() {
        val gone = b.copyWithState(ArrowState.FLYING_OUT)
        val arrows = listOf(a, gone, c)
        val map = GridOccupancyMap(3, 1).apply { rebuild(arrows) }
        val picked = hit(arrows, map, 150f, 50f, assist = true)
        assertTrue("flying arrow must not be picked", picked?.id != "B")
    }

    @Test
    fun fingerRadiusReachesTinyArrowsInEmptySpace() {
        // Arrow J sits in cell 0; tap 40 px right of its tail end: outside 0.75 cell? no, inside radius 90
        val j = arrow("J", 0, 0, Direction.LEFT)
        val map = GridOccupancyMap(4, 1).apply { rebuild(listOf(j)) }
        // Tail end at x = 82; tap at x = 165 is 83 px away: beyond 0.75 cell (75) ...
        assertNull(hit(listOf(j), map, 165f, 50f, assist = true, minRadius = 24f))
        // ... but within a larger finger radius (board zoomed out)
        assertEquals("J", hit(listOf(j), map, 165f, 50f, assist = true, minRadius = 90f)?.id)
    }
}
