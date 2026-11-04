package com.arrowaway.puzzle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Authentic (#) Grid Guide Icon matching the gameplay area grid booster button.
 */
@Composable
fun GridGuideIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val strokeW = 1.8.dp.toPx()
        val pad = 1.5.dp.toPx()
        val w = size.width
        val h = size.height

        // 2 horizontal grid bars
        drawLine(
            color = tint,
            start = Offset(pad, h * 0.36f),
            end = Offset(w - pad, h * 0.36f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(pad, h * 0.64f),
            end = Offset(w - pad, h * 0.64f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // 2 vertical grid bars
        drawLine(
            color = tint,
            start = Offset(w * 0.36f, pad),
            end = Offset(w * 0.36f, h - pad),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.64f, pad),
            end = Offset(w * 0.64f, h - pad),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}
