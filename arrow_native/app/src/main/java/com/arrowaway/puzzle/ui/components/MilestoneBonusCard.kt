package com.arrowaway.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowaway.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BonusAmber = Color(0xFFF59E0B)
private val BonusAmberDeep = Color(0xFFD97706)
private val BonusAmberLight = Color(0xFFFCD34D)

/**
 * Victory-screen card for the every-5-levels milestone hint.
 *
 * Plays once, after the stars land: the card rises in, the bulb pops with a single halo ring,
 * the "+1" counts in and one light sweep crosses the card. It then stays still, so it never
 * competes with the Next Level button.
 */
@Composable
fun MilestoneBonusCard(
    hints: Int,
    theme: ThemePreset,
    compact: Boolean,
    modifier: Modifier = Modifier,
    startDelayMs: Long = 700L
) {
    val cardAlpha = remember { Animatable(0f) }
    val cardOffset = remember { Animatable(1f) } // 1 = 14dp below, 0 = in place
    val iconScale = remember { Animatable(0f) }
    val haloProgress = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0f) }
    val shimmer = remember { Animatable(-0.3f) }
    val riseDistancePx = with(LocalDensity.current) { 14.dp.toPx() }

    LaunchedEffect(Unit) {
        delay(startDelayMs)
        launch { cardAlpha.animateTo(1f, tween(240, easing = LinearOutSlowInEasing)) }
        cardOffset.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        launch {
            iconScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
        launch { haloProgress.animateTo(1f, tween(650, easing = LinearOutSlowInEasing)) }
        delay(140)
        launch {
            badgeScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
        delay(120)
        shimmer.animateTo(1.3f, tween(850, easing = FastOutSlowInEasing))
    }

    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = BonusAmber.copy(alpha = if (theme.isDark) 0.16f else 0.10f),
        border = BorderStroke(1.dp, BonusAmber.copy(alpha = if (theme.isDark) 0.55f else 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = cardAlpha.value
                translationY = cardOffset.value * riseDistancePx
            }
            .clip(shape)
            .drawWithContent {
                drawContent()
                val p = shimmer.value
                if (p > -0.3f && p < 1.3f) {
                    val band = size.width * 0.35f
                    val x = p * size.width
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0f),
                                Color.White.copy(alpha = if (theme.isDark) 0.18f else 0.45f),
                                Color.White.copy(alpha = 0f)
                            ),
                            start = Offset(x - band, 0f),
                            end = Offset(x + band, size.height)
                        )
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                // One expanding halo ring as the bulb lands
                val h = haloProgress.value
                if (h > 0f && h < 1f) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer {
                                scaleX = 0.8f + 0.6f * h
                                scaleY = 0.8f + 0.6f * h
                                alpha = (1f - h) * 0.55f
                            }
                            .background(BonusAmber, CircleShape)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                        }
                        .background(
                            Brush.linearGradient(listOf(BonusAmberLight, BonusAmberDeep)),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MILESTONE BONUS",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = if (theme.isDark) BonusAmberLight else BonusAmberDeep
                )
                Text(
                    text = if (hints == 1) "Free hint added" else "$hints free hints added",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = BonusAmberDeep,
                modifier = Modifier.graphicsLayer {
                    scaleX = badgeScale.value
                    scaleY = badgeScale.value
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "+$hints",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
