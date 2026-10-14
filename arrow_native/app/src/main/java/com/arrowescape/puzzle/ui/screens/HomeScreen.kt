package com.arrowescape.puzzle.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.game.GameUiState
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun HomeScreen(
    uiState: GameUiState,
    theme: ThemePreset,
    isSoundOn: Boolean,
    onPlayClick: () -> Unit,
    onDailyChallengeClick: () -> Unit,
    onThemeClick: () -> Unit,
    onSoundToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playPulse")
    val playScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playScale"
    )

    Surface(
        color = Color.White,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // 1. TOP BAR: No Ads (left), Settings (right)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                // No Ads Circle Button
                NoAdsButton(onClick = { /* In-app purchase / remove ads */ })

                // Settings Gear Button
                IconButton(
                    onClick = onSoundToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 2. CENTER HERO: Large Purple Number + "L E V E L"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${uiState.currentLevelNumber}",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6656C3),
                    lineHeight = 100.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "L E V E L",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 10.sp,
                    color = Color(0xFF6656C3)
                )
            }

            // 3. BOTTOM BUTTONS: Green Play Capsule + Gray Daily Challenge Capsule
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                // Primary Play Pill Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(64.dp)
                        .scale(playScale)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFF4CD86F))
                        .clickable(onClick = onPlayClick)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // White circle with green play triangle on the left
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Icon",
                                tint = Color(0xFF4CD86F),
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Centered text "Play"
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Play",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Invisible balancer box on right so text stays perfectly centered
                        Spacer(modifier = Modifier.size(46.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Daily Challenge Pill Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(62.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFF908B88))
                        .clickable(onClick = onDailyChallengeClick)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dark slate disc with lock icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4A4643)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Text column: "Daily Challenge" and "Level 5"
                        Column {
                            Text(
                                text = "Daily Challenge",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Level 5",
                                fontSize = 12.sp,
                                color = Color(0xFFDCDAD7),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimalist circular "No Ads" button matching Screenshot 1
 */
@Composable
private fun NoAdsButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFFE2E8F0), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            // Draw circle with orange slash
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            
            // Draw small "AD" text or inner badge representation
            drawLine(
                color = Color(0xFFF97316),
                start = Offset(4f, size.height - 4f),
                end = Offset(size.width - 4f, 4f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        Text(
            text = "AD",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2563EB)
        )
    }
}
