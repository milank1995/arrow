package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun DailyUnlockDialog(
    theme: ThemePreset,
    onPlayTodayClick: (remindersOptIn: Boolean) -> Unit,
    onDismiss: (remindersOptIn: Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dialogWidth = minOf(screenWidth - 36.dp, 360.dp)

    var optInReminders by remember { mutableStateOf(true) }

    // Celebratory Entrance Animations
    val cardScale = remember { Animatable(0.85f) }
    val iconScale = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "unlockGlow")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    LaunchedEffect(Unit) {
        cardScale.animateTo(1.0f, spring(dampingRatio = 0.75f))
        iconScale.animateTo(1.2f, tween(180, easing = FastOutSlowInEasing))
        iconScale.animateTo(1.0f, spring(dampingRatio = 0.65f))
    }

    Dialog(
        onDismissRequest = { onDismiss(optInReminders) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (theme.isDark) Color(0xFF1E293B) else Color.White
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (theme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            ),
            elevation = CardDefaults.cardElevation(20.dp),
            modifier = Modifier
                .width(dialogWidth)
                .scale(cardScale.value)
                .padding(vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 26.dp)
            ) {
                // Feature Unlocked Pill Badge
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = theme.arrowHint.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, theme.arrowHint.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = theme.arrowHint,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "NEW FEATURE UNLOCKED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = theme.arrowHint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hero Trophy Icon with ambient pulsing aura
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(auraScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        theme.arrowHint.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .scale(iconScale.value)
                            .clip(CircleShape)
                            .background(theme.arrowHint.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Daily Challenge",
                            tint = theme.arrowHint,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Subtitle
                Text(
                    text = "Daily Challenge Unlocked!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = theme.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A fresh, handcrafted puzzle awaits you every day. Test your skills, earn rewards, and build your daily streak!",
                    fontSize = 13.sp,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Polite Notification Opt-In Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = theme.surface,
                    border = BorderStroke(1.dp, theme.gridLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = theme.arrowHint,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Daily puzzle reminders",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Friendly alert when a puzzle is ready",
                                    fontSize = 11.sp,
                                    color = theme.textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = optInReminders,
                            onCheckedChange = { optInReminders = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = theme.arrowDefault,
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = if (theme.isDark) Color(0xFF475569) else Color(0xFFE2E8F0),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary CTA: Play Today's Puzzle
                AppPrimaryButton(
                    text = "Play Today's Puzzle",
                    onClick = { onPlayTodayClick(optInReminders) },
                    theme = theme,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary CTA: Maybe Later
                AppSecondaryButton(
                    text = "Maybe Later",
                    onClick = { onDismiss(optInReminders) },
                    theme = theme,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
