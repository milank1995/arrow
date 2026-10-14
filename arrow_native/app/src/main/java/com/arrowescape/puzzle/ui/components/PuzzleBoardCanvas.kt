package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun PuzzleBoardCanvas(
    xSize: Int,
    ySize: Int,
    arrows: List<Arrow>,
    theme: ThemePreset,
    isGridLinesActive: Boolean,
    previewRayArrowId: String?,
    onArrowClick: (Arrow) -> Unit,
    onArrowLongPress: (Arrow, isPressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val hintPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulse"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .aspectRatio(xSize.toFloat() / ySize.toFloat())
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val cellSize = minOf(width / xSize, height / ySize)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(xSize.toFloat() / ySize.toFloat())
                .pointerInput(arrows, cellSize) {
                    detectTapGestures(
                        onPress = { offset ->
                            val clickedCoord = GridCoordinate(
                                x = (offset.x / cellSize).toInt().coerceIn(0, xSize - 1),
                                y = (offset.y / cellSize).toInt().coerceIn(0, ySize - 1)
                            )
                            val tappedArrow = arrows.firstOrNull { 
                                it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT && it.isPointInside(clickedCoord) 
                            }
                            if (tappedArrow != null) {
                                onArrowLongPress(tappedArrow, true)
                                tryAwaitRelease()
                                onArrowLongPress(tappedArrow, false)
                            }
                        },
                        onTap = { offset ->
                            val clickedCoord = GridCoordinate(
                                x = (offset.x / cellSize).toInt().coerceIn(0, xSize - 1),
                                y = (offset.y / cellSize).toInt().coerceIn(0, ySize - 1)
                            )
                            val tappedArrow = arrows.firstOrNull { 
                                it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT && it.isPointInside(clickedCoord) 
                            }
                            if (tappedArrow != null) {
                                onArrowClick(tappedArrow)
                            }
                        }
                    )
                }
        ) {
            // Draw background
            drawRect(color = Color.White)

            // 1. Grid Lines / Dots only if user turned them on via the '#' Booster button
            if (isGridLinesActive) {
                drawGridLines(xSize, ySize, cellSize, theme.gridLine)
                drawGridDots(xSize, ySize, cellSize, theme.gridDot)
            }

            // 2. Draw Preview Ray if an arrow is long-pressed
            val previewArrow = arrows.firstOrNull { it.id == previewRayArrowId }
            if (previewArrow != null) {
                drawPreviewRay(previewArrow, xSize, ySize, cellSize, theme.arrowHint)
            }

            // 3. Draw All Arrows (Dark slate stroke + sharp arrowhead)
            for (arrow in arrows) {
                if (arrow.state == ArrowState.CLEARED) continue

                drawArrowEntity(
                    arrow = arrow,
                    cellSize = cellSize,
                    theme = theme,
                    hintPulseScale = hintPulseScale
                )
            }
        }
    }
}

private fun DrawScope.drawGridLines(xSize: Int, ySize: Int, cellSize: Float, color: Color) {
    for (x in 0..xSize) {
        drawLine(
            color = color,
            start = Offset(x * cellSize, 0f),
            end = Offset(x * cellSize, ySize * cellSize),
            strokeWidth = 1.2f
        )
    }
    for (y in 0..ySize) {
        drawLine(
            color = color,
            start = Offset(0f, y * cellSize),
            end = Offset(xSize * cellSize, y * cellSize),
            strokeWidth = 1.2f
        )
    }
}

private fun DrawScope.drawGridDots(xSize: Int, ySize: Int, cellSize: Float, color: Color) {
    for (x in 0..xSize) {
        for (y in 0..ySize) {
            drawCircle(
                color = color,
                radius = 3.0f,
                center = Offset(x * cellSize, y * cellSize)
            )
        }
    }
}

private fun DrawScope.drawPreviewRay(
    arrow: Arrow,
    xSize: Int,
    ySize: Int,
    cellSize: Float,
    color: Color
) {
    val headCenter = Offset(
        x = (arrow.head.x + 0.5f) * cellSize,
        y = (arrow.head.y + 0.5f) * cellSize
    )

    var endOffset = headCenter
    when (arrow.direction) {
        Direction.UP -> endOffset = Offset(headCenter.x, -cellSize)
        Direction.DOWN -> endOffset = Offset(headCenter.x, (ySize + 1) * cellSize)
        Direction.LEFT -> endOffset = Offset(-cellSize, headCenter.y)
        Direction.RIGHT -> endOffset = Offset((xSize + 1) * cellSize, headCenter.y)
    }

    drawLine(
        color = color.copy(alpha = 0.5f),
        start = headCenter,
        end = endOffset,
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawArrowEntity(
    arrow: Arrow,
    cellSize: Float,
    theme: ThemePreset,
    hintPulseScale: Float
) {
    val baseColor = when (arrow.state) {
        ArrowState.HINTED -> theme.arrowHint
        ArrowState.WRONG_FLASH -> theme.arrowWrong
        ArrowState.FLYING_OUT -> theme.arrowClear
        else -> theme.arrowDefault
    }

    val strokeWidth = cellSize * 0.16f * (if (arrow.state == ArrowState.HINTED) hintPulseScale else 1.0f)
    val headCenter = Offset(
        x = (arrow.head.x + 0.5f) * cellSize,
        y = (arrow.head.y + 0.5f) * cellSize
    )

    // Draw body path connecting all coordinates in arrow.body
    if (arrow.body.size > 1) {
        val path = Path()
        val first = arrow.body.first()
        path.moveTo((first.x + 0.5f) * cellSize, (first.y + 0.5f) * cellSize)

        for (i in 1 until arrow.body.size) {
            val pt = arrow.body[i]
            path.lineTo((pt.x + 0.5f) * cellSize, (pt.y + 0.5f) * cellSize)
        }
        path.lineTo(headCenter.x, headCenter.y)

        drawPath(
            path = path,
            color = baseColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // Draw sharp solid arrowhead matching original game
    val headSize = cellSize * 0.44f
    rotate(
        degrees = arrow.direction.angleDegrees,
        pivot = headCenter
    ) {
        val arrowHeadPath = Path().apply {
            moveTo(headCenter.x, headCenter.y - headSize * 0.55f) // Forward tip
            lineTo(headCenter.x + headSize * 0.40f, headCenter.y + headSize * 0.32f) // Right base
            lineTo(headCenter.x - headSize * 0.40f, headCenter.y + headSize * 0.32f) // Left base
            close()
        }

        drawPath(
            path = arrowHeadPath,
            color = baseColor
        )
    }
}
