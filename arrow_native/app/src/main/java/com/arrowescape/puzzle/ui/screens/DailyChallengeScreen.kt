package com.arrowescape.puzzle.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.components.AppPrimaryButton
import com.arrowescape.puzzle.ui.theme.ThemePreset
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dedicated, full-screen Daily Challenge page.
 * Features:
 * - Dedicated top bar with back navigation and streak indicator.
 * - Month calendar grid with completed dates tracking.
 * - Dynamic 100% theme adaptation.
 * - Bottom challenge action card with tactile CTA button.
 */
@Composable
fun DailyChallengeScreen(
    completedDates: Set<String>,
    streakDays: Int,
    theme: ThemePreset,
    onPlayDate: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val today = remember { LocalDate.now() }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }

    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfMonth = currentYearMonth.atDay(1)
    val startDayOffset = firstDayOfMonth.dayOfWeek.value % 7

    val completedThisMonth = (1..daysInMonth).count { day ->
        val date = currentYearMonth.atDay(day).toString()
        completedDates.contains(date)
    }

    val monthTitle = remember(currentYearMonth) {
        currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).uppercase()
    }

    val selectedDateCompleted = completedDates.contains(selectedDate.toString())
    val isSelectedFuture = selectedDate.isAfter(today)

    // Theme-based CTA button gradient and bevel
    val (ctaGrad1, ctaGrad2, ctaBevel) = when (theme.name) {
        "Warm Sepia" -> Triple(Color(0xFFE09F3E), Color(0xFFC57B22), Color(0xFF8C5314))
        "Midnight Dark" -> Triple(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1))
        "Calm Pastel" -> Triple(Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF6D28D9))
        "Mint Fresh" -> Triple(Color(0xFF059669), Color(0xFF047857), Color(0xFF064E3B))
        "Sunset Rose" -> Triple(Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFF9F1239))
        else -> Triple(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)) // Clean Light
    }

    Surface(
        color = theme.background,
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableHeight = maxHeight
            val requiresScroll = availableHeight < 670.dp
            val isCompact = availableHeight < 740.dp
            val scrollState = rememberScrollState()

            val contentModifier = if (requiresScroll) {
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(scrollState)
            } else {
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 20.dp, vertical = if (isCompact) 10.dp else 16.dp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (requiresScroll) Arrangement.Top else Arrangement.SpaceBetween,
                modifier = contentModifier
            ) {
                // 1. TOP APP BAR: Back Button + Title + Streak Pill
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .padding(top = 4.dp, bottom = 10.dp)
                ) {
                    // Back Button
                    Surface(
                        shape = CircleShape,
                        color = theme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, theme.gridLine),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = theme.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                // Screen Title
                Text(
                    text = "DAILY CHALLENGE",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = theme.textPrimary
                )

                // Streak Pill Indicator
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = theme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                    shadowElevation = 1.dp,
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(text = "🔥", fontSize = 14.sp)
                        Text(
                            text = "$streakDays",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (theme.isDark) Color(0xFFFDE047) else Color(0xFFD97706)
                        )
                    }
                }
            }

            // 2. MAIN SCROLLABLE BODY
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Stats Card: Solved count & Trophy
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = theme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, theme.gridLine),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(theme.arrowHint.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trophy",
                                    tint = theme.arrowHint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Monthly Progress",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "$completedThisMonth of $daysInMonth puzzles solved",
                                    fontSize = 12.sp,
                                    color = theme.textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = theme.background,
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine)
                        ) {
                            val badgeText = if (completedThisMonth == daysInMonth && daysInMonth > 0) "✓ $completedThisMonth/$daysInMonth" else "$completedThisMonth/$daysInMonth"
                            Text(
                                text = badgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (completedThisMonth > 0) theme.arrowClear else theme.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CALENDAR CARD
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = theme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, theme.gridLine),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 18.dp)
                    ) {
                        // Month Selector Header
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 4.dp, bottom = 12.dp)
                        ) {
                            IconButton(
                                onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Month",
                                    tint = theme.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = monthTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = theme.textPrimary
                            )

                            IconButton(
                                onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Month",
                                    tint = theme.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Day of Week Header: S M T W T F S
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                                Text(
                                    text = dayLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Calendar Grid (6 rows x 7 days)
                        val totalCells = 42
                        val rows = totalCells / 7

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (rowIndex in 0 until rows) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    for (colIndex in 0 until 7) {
                                        val cellIndex = rowIndex * 7 + colIndex
                                        val dayNumber = cellIndex - startDayOffset + 1

                                        if (dayNumber in 1..daysInMonth) {
                                            val thisDate = currentYearMonth.atDay(dayNumber)
                                            val isToday = (thisDate == today)
                                            val isSelected = (thisDate == selectedDate)
                                            val isCompleted = completedDates.contains(thisDate.toString())
                                            val isFuture = thisDate.isAfter(today)

                                            val cellBackground = when {
                                                isSelected -> Brush.verticalGradient(listOf(ctaGrad1, ctaGrad2))
                                                isCompleted -> Brush.verticalGradient(listOf(theme.arrowClear.copy(alpha = 0.16f), theme.arrowClear.copy(alpha = 0.10f)))
                                                isToday -> Brush.verticalGradient(listOf(ctaGrad1.copy(alpha = 0.18f), ctaGrad1.copy(alpha = 0.10f)))
                                                else -> Brush.verticalGradient(listOf(theme.background.copy(alpha = 0.5f), theme.background.copy(alpha = 0.35f)))
                                            }

                                            val cellBorder = when {
                                                isSelected -> androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.40f))
                                                isCompleted -> androidx.compose.foundation.BorderStroke(1.dp, theme.arrowClear.copy(alpha = 0.40f))
                                                isToday -> androidx.compose.foundation.BorderStroke(1.5.dp, ctaGrad1)
                                                else -> null
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(10.dp)) else Modifier)
                                                    .background(cellBackground)
                                                    .clickable(enabled = !isFuture) {
                                                        selectedDate = thisDate
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$dayNumber",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.SemiBold,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isFuture -> theme.textSecondary.copy(alpha = 0.35f)
                                                        isCompleted -> theme.arrowClear
                                                        else -> theme.textPrimary
                                                    }
                                                )
                                                if (isCompleted) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Completed",
                                                        tint = if (isSelected) Color.White.copy(alpha = 0.90f) else theme.arrowClear,
                                                        modifier = Modifier
                                                            .size(11.dp)
                                                            .align(Alignment.BottomEnd)
                                                            .padding(end = 2.dp, bottom = 2.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. BOTTOM ACTION PANEL (Selected Day Details & Tactile CTA)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = theme.surface,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, theme.gridLine),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Header for selected day
                    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
                    val formattedSelected = selectedDate.format(formatter)

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = formattedSelected,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textPrimary
                            )
                            Text(
                                text = if (selectedDate == today) "Today's Puzzle" else "Daily Puzzle",
                                fontSize = 12.sp,
                                color = theme.textSecondary
                            )
                        }

                        if (selectedDateCompleted) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = theme.arrowClear.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.arrowClear)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = theme.arrowClear,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Completed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.arrowClear
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Play / Replay Flat Modern CTA Button
                    if (!isSelectedFuture) {
                        AppPrimaryButton(
                            text = if (selectedDateCompleted) "Replay Level" else "Play Daily Puzzle",
                            onClick = {
                                onPlayDate(selectedDate.toString())
                            },
                            theme = theme,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
}
