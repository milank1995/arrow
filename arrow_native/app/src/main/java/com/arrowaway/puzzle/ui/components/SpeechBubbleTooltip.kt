package com.arrowaway.puzzle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Speech bubble shape matching the reference UI design:
 * Smooth rounded rectangle with a triangular pointer arrow.
 */
class SpeechBubbleShape(
    private val cornerRadius: Dp = 14.dp,
    private val pointerWidth: Dp = 16.dp,
    private val pointerHeight: Dp = 9.dp,
    private val pointerOffsetX: Dp = 38.dp,
    private val pointerAtBottom: Boolean = true
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { cornerRadius.toPx() }
        val pw = with(density) { pointerWidth.toPx() }
        val ph = with(density) { pointerHeight.toPx() }
        val pCenter = with(density) { pointerOffsetX.toPx() }
        val pStart = (pCenter - pw / 2f).coerceAtLeast(r)
        val pEnd = (pCenter + pw / 2f).coerceAtMost(size.width - r)

        val path = Path().apply {
            if (pointerAtBottom) {
                // Top-left corner
                moveTo(r, 0f)
                lineTo(size.width - r, 0f)
                arcTo(Rect(size.width - 2 * r, 0f, size.width, 2 * r), -90f, 90f, false)
                // Right side
                lineTo(size.width, size.height - ph - r)
                arcTo(Rect(size.width - 2 * r, size.height - ph - 2 * r, size.width, size.height - ph), 0f, 90f, false)
                // Bottom side with downward pointer
                lineTo(pEnd, size.height - ph)
                lineTo(pCenter, size.height) // pointer tip
                lineTo(pStart, size.height - ph)
                lineTo(r, size.height - ph)
                arcTo(Rect(0f, size.height - ph - 2 * r, 2 * r, size.height - ph), 90f, 90f, false)
                // Left side
                lineTo(0f, r)
                arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
                close()
            } else {
                // Pointer at top
                moveTo(r, ph)
                lineTo(pStart, ph)
                lineTo(pCenter, 0f) // pointer tip pointing up
                lineTo(pEnd, ph)
                lineTo(size.width - r, ph)
                arcTo(Rect(size.width - 2 * r, ph, size.width, ph + 2 * r), -90f, 90f, false)
                lineTo(size.width, size.height - r)
                arcTo(Rect(size.width - 2 * r, size.height - 2 * r, size.width, size.height), 0f, 90f, false)
                lineTo(r, size.height)
                arcTo(Rect(0f, size.height - 2 * r, 2 * r, size.height), 90f, 90f, false)
                lineTo(0f, ph + r)
                arcTo(Rect(0f, ph, 2 * r, ph + 2 * r), 180f, 90f, false)
                close()
            }
        }
        return Outline.Generic(path)
    }
}

/**
 * Anchored speech-bubble tooltip with rich chocolate-brown styling and
 * clean centered typography matching the reference design.
 */
@Composable
fun SpeechBubbleTooltip(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = remember {
        SpeechBubbleShape(
            cornerRadius = 14.dp,
            pointerWidth = 16.dp,
            pointerHeight = 9.dp,
            pointerOffsetX = 38.dp,
            pointerAtBottom = true
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(tween(200), initialScale = 0.90f),
        exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.90f),
        modifier = modifier
    ) {
        Surface(
            shape = bubbleShape,
            color = Color(0xFF5A3825), // Warm dark chocolate brown matching reference design
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 220.dp, max = 290.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                }
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 23.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
