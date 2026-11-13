package com.arrowaway.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BubbleAmber = Color(0xFFF59E0B)
private val BubbleOrange = Color(0xFFEA580C)
private val BubbleDeep = Color(0xFFC2410C)

/** Height of the speech-bubble pointer; the placement code leaves room for it. */
val DailyReminderPointerHeight = 10.dp

/**
 * Home-screen reminder attached to the Daily Challenge button.
 *
 * Warm amber-to-orange card (stands out from the brown Play button and sepia background) with
 * a title that wraps instead of cropping, a short subtitle, a white Play button and a close button.
 *
 * Motion: springs in, then a slow float, a gentle Play-button pulse and a light sweep every few
 * seconds so it stays noticeable without being noisy. [pointerAtBottom] flips the pointer when the bubble
 * has to sit above the button on very short screens.
 */
@Composable
fun DailyReminderBubble(
    streakDays: Int,
    width: Dp,
    pointerAtBottom: Boolean,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** One-row layout (no subtitle, Play pill beside the text) for short screens. */
    dense: Boolean = false,
    /** false = static copy used only to measure the bubble's height (no animations run). */
    animated: Boolean = true
) {
    val compact = dense || width < 320.dp
    val shape = remember(width, pointerAtBottom) {
        SpeechBubbleShape(
            cornerRadius = 18.dp,
            pointerWidth = 20.dp,
            pointerHeight = DailyReminderPointerHeight,
            pointerOffsetX = width / 2,
            pointerAtBottom = pointerAtBottom
        )
    }

    val motion = if (animated) reminderMotion() else ReminderMotion(1f, 0.5f, 1f, -1f)
    val enter = motion.enter
    val float = motion.float
    val breathe = motion.breathe
    val sweep = motion.sweep

    val title = if (streakDays > 0) "Keep your $streakDays-day streak!" else "Today's puzzle is ready!"
    val subtitle = if (streakDays > 0) "Today's challenge is waiting" else "A new challenge every day"

    val pointerPad = DailyReminderPointerHeight
    Box(
        modifier = modifier
            .width(width)
            .graphicsLayer {
                val e = enter
                alpha = e.coerceIn(0f, 1f)
                val dir = if (pointerAtBottom) 1f else -1f
                translationY = (1f - e) * dir * 12.dp.toPx() + (float - 0.5f) * 4.dp.toPx()
                scaleX = 0.9f + 0.1f * e
                scaleY = 0.9f + 0.1f * e
                transformOrigin = TransformOrigin(0.5f, if (pointerAtBottom) 1f else 0f)
            }
            .shadow(10.dp, shape, ambientColor = BubbleDeep, spotColor = BubbleDeep)
            .background(
                Brush.linearGradient(listOf(BubbleAmber, BubbleOrange)),
                shape
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .clip(shape)
            .drawWithContent {
                drawContent()
                if (sweep in 0f..1f) {
                    val band = size.width * 0.28f
                    val x = sweep * (size.width + band * 2) - band
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                            start = Offset(x - band, 0f),
                            end = Offset(x + band, size.height)
                        )
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(
                start = if (compact) 12.dp else 14.dp,
                end = if (compact) 8.dp else 10.dp,
                top = (if (compact) 10.dp else 12.dp) + if (pointerAtBottom) 0.dp else pointerPad,
                bottom = (if (compact) 10.dp else 12.dp) + if (pointerAtBottom) pointerPad else 0.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = if (compact) 14.sp else 15.5.sp,
                        lineHeight = if (compact) 17.sp else 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!dense) Spacer(modifier = Modifier.height(2.dp))
                    if (!dense) Text(
                        text = subtitle,
                        fontSize = if (compact) 11.5.sp else 12.5.sp,
                        lineHeight = if (compact) 14.sp else 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (dense) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ReminderPlayButton(fullWidth = false, compact = true, pulse = breathe, onPlay = onPlay)
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Close
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(if (dense) Alignment.CenterVertically else Alignment.Top)
                        .size(34.dp)
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
                            .size(22.dp)
                            .background(Color.Black.copy(alpha = 0.16f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close reminder",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (!dense) {
                Spacer(modifier = Modifier.height(if (compact) 9.dp else 11.dp))
                ReminderPlayButton(fullWidth = true, compact = compact, pulse = breathe, onPlay = onPlay)
            }
        }
    }
}

/** White-on-orange Play button: full width under the text, or a pill beside it in the dense layout. */
@Composable
private fun ReminderPlayButton(fullWidth: Boolean, compact: Boolean, pulse: Float, onPlay: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = (if (fullWidth) Modifier.fillMaxWidth().padding(end = 4.dp) else Modifier)
            .height(if (compact) 40.dp else 44.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .shadow(3.dp, RoundedCornerShape(percent = 50))
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .clickable(onClick = onPlay)
            .padding(horizontal = if (fullWidth) 0.dp else 12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = BubbleOrange,
            modifier = Modifier.size(if (compact) 20.dp else 22.dp)
        )
        Spacer(modifier = Modifier.width(if (fullWidth) 4.dp else 2.dp))
        Text(
            text = when {
                !fullWidth -> "Play"
                compact -> "Play Now"
                else -> "Play Today's Challenge"
            },
            fontSize = if (compact) 14.sp else 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BubbleDeep,
            maxLines = 1
        )
    }
}

private data class ReminderMotion(
    val enter: Float,
    val float: Float,
    val breathe: Float,
    val sweep: Float
)

/** Entrance (spring rise), then idle float, a gentle Play-button pulse and a light sweep every few seconds. */
@Composable
private fun reminderMotion(): ReminderMotion {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
    }
    val idle = rememberInfiniteTransition(label = "dailyReminderIdle")
    val float by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )
    val breathe by idle.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val sweep by idle.animateFloat(
        initialValue = -0.6f, targetValue = 2.4f, // visible part is 0..1; the rest is a pause
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )
    return ReminderMotion(enter.value, float, breathe, sweep)
}
