package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LevelVictoryDialog(
    levelNumber: Int,
    coinsEarned: Int,
    streakDays: Int,
    theme: ThemePreset,
    onNextLevelClick: () -> Unit
) {
    val cardScale = remember { Animatable(0.88f) }
    val star1Scale = remember { Animatable(0f) }
    val star2Scale = remember { Animatable(0f) }
    val star3Scale = remember { Animatable(0f) }
    val star2Rotation = remember { Animatable(-25f) }
    val animatedCoins = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            cardScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        delay(120)
        launch {
            star1Scale.animateTo(1.25f, tween(130, easing = FastOutSlowInEasing))
            star1Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        delay(100)
        launch {
            star2Rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        launch {
            star2Scale.animateTo(1.35f, tween(150, easing = FastOutSlowInEasing))
            star2Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        delay(100)
        launch {
            star3Scale.animateTo(1.25f, tween(130, easing = FastOutSlowInEasing))
            star3Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        delay(80)
        launch {
            animatedCoins.animateTo(coinsEarned.toFloat(), tween(400, easing = FastOutSlowInEasing))
        }
    }

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            elevation = CardDefaults.cardElevation(16.dp),
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .scale(cardScale.value)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Celebration Stars Row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(36.dp)
                            .scale(star1Scale.value)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(48.dp)
                            .scale(star2Scale.value)
                            .rotate(star2Rotation.value)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(36.dp)
                            .scale(star3Scale.value)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "LEVEL COMPLETE!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.textPrimary
                )

                Text(
                    text = "Level $levelNumber cleared smoothly",
                    fontSize = 14.sp,
                    color = theme.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Rewards Banner
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.background)
                        .padding(vertical = 12.dp)
                ) {
                    // Coins reward
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+${animatedCoins.value.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = theme.textPrimary
                        )
                    }

                    // Streak Flame
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$streakDays Day Streak",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = theme.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Next Level Button
                Button(
                    onClick = onNextLevelClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.arrowDefault),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "CONTINUE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (theme.isDark) Color(0xFF0F172A) else Color.White
                    )
                }
            }
        }
    }
}
