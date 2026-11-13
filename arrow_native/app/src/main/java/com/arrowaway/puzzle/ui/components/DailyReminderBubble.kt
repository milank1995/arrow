package com.arrowaway.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val BubbleBrown = Color(0xFF5A3825)
private val PlayGreen = Color(0xFF10B981)

/**
 * Home-screen reminder attached under the Daily Challenge button: a short message, a Play
 * button that starts today's challenge, and a close button. Same speech-bubble style as the
 * in-game hint tooltip, with the pointer centred on the button above it.
 */
@Composable
fun DailyReminderBubble(
    streakDays: Int,
    width: Dp,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = remember(width) {
        SpeechBubbleShape(
            cornerRadius = 16.dp,
            pointerWidth = 18.dp,
            pointerHeight = 9.dp,
            pointerOffsetX = width / 2,
            pointerAtBottom = false
        )
    }
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { enter.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
    }
    val playPulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        // Two gentle nudges on the Play button once the bubble has landed, then still
        kotlinx.coroutines.delay(500)
        repeat(2) {
            playPulse.animateTo(1.08f, tween(220))
            playPulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    val title = if (streakDays > 0) "Keep your $streakDays-day streak!" else "Today's puzzle is ready!"

    Surface(
        shape = shape,
        color = BubbleBrown,
        shadowElevation = 8.dp,
        modifier = modifier
            .width(width)
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * -8.dp.toPx()
                scaleX = 0.94f + 0.06f * enter.value
                scaleY = 0.94f + 0.06f * enter.value
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 9.dp + 10.dp, bottom = 10.dp)
        ) {
            Text(text = if (streakDays > 0) "🔥" else "📅", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Solve today's Daily Challenge",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = PlayGreen,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = playPulse.value
                        scaleY = playPulse.value
                    }
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onPlay)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Play",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White.copy(alpha = 0.16f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close reminder",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
