package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyCalendarDialog(
    completedDates: Set<String>,
    streakDays: Int,
    theme: ThemePreset,
    onPlayDate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now() }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }

    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfMonth = currentYearMonth.atDay(1)
    // Sunday = 0, Monday = 1, ... Saturday = 6 (Unity calendar standard)
    val startDayOffset = firstDayOfMonth.dayOfWeek.value % 7

    // Completed count in displayed month
    val completedThisMonth = (1..daysInMonth).count { day ->
        val date = currentYearMonth.atDay(day).toString()
        completedDates.contains(date)
    }

    val monthTitle = remember(currentYearMonth) {
        currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).uppercase()
    }

    val selectedDateCompleted = completedDates.contains(selectedDate.toString())
    val isSelectedFuture = selectedDate.isAfter(today)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .border(1.dp, theme.gridLine, RoundedCornerShape(28.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. TOP HEADER: Title + Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trophy",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "DAILY CHALLENGE",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = theme.textPrimary
                            )
                            Text(
                                text = "Solve daily puzzles for rewards",
                                fontSize = 11.sp,
                                color = theme.textSecondary
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = theme.boosterBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        modifier = Modifier.size(34.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = theme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. STATS PILL ROW: Streak + Month Cleared
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Streak Pill
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$streakDays Day Streak",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // Monthly Cleared Pill
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Cleared",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$completedThisMonth/$daysInMonth Solved",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. MONTH NAVIGATOR BAR: < [MONTH YEAR] >
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.background)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = theme.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = monthTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = theme.textPrimary
                    )

                    val canGoForward = currentYearMonth.isBefore(YearMonth.from(today))
                    IconButton(
                        onClick = {
                            if (canGoForward) currentYearMonth = currentYearMonth.plusMonths(1)
                        },
                        enabled = canGoForward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = if (canGoForward) theme.textPrimary else theme.textSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. WEEKDAY LABELS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. CALENDAR DAYS GRID (7 columns)
                val totalSlots = startDayOffset + daysInMonth
                val totalRows = (totalSlots + 6) / 7

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (row in 0 until totalRows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 0..6) {
                                val slotIndex = row * 7 + col
                                val dayNumber = slotIndex - startDayOffset + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = currentYearMonth.atDay(dayNumber)
                                    val isCompleted = completedDates.contains(cellDate.toString())
                                    val isToday = cellDate == today
                                    val isSelected = cellDate == selectedDate
                                    val isFuture = cellDate.isAfter(today)

                                    DayCell(
                                        dayNumber = dayNumber,
                                        isCompleted = isCompleted,
                                        isToday = isToday,
                                        isSelected = isSelected,
                                        isFuture = isFuture,
                                        theme = theme,
                                        onClick = {
                                            if (!isFuture) {
                                                selectedDate = cellDate
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 6. SELECTED DAY DETAILS & 3D ACTION BUTTON
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = theme.background,
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        val formattedDate = remember(selectedDate) {
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = formattedDate,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = if (selectedDate == today) "Today's Puzzle" else "Daily Puzzle",
                                    fontSize = 11.sp,
                                    color = theme.textSecondary
                                )
                            }

                            // Reward or Status Pill
                            if (selectedDateCompleted) {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Done",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Completed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = Color(0xFFFEF3C7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = "Coins",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+100 Coins",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3D Play Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clickable(
                                    enabled = !isSelectedFuture,
                                    onClick = {
                                        onPlayDate(selectedDate.toString())
                                        onDismiss()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isSelectedFuture) {
                                // 3D Bottom Bevel
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .align(Alignment.BottomCenter)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selectedDateCompleted) Color(0xFF1D4ED8) else Color(0xFF047857)
                                        )
                                )
                                // Top Surface
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .align(Alignment.TopCenter)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                if (selectedDateCompleted) listOf(
                                                    Color(0xFF3B82F6),
                                                    Color(0xFF2563EB)
                                                ) else listOf(
                                                    Color(0xFF34D399),
                                                    Color(0xFF059669)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (selectedDateCompleted) "REPLAY LEVEL" else "PLAY CHALLENGE",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                // Disabled State
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = theme.textSecondary.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "LOCKED (FUTURE DATE)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isCompleted: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    theme: ThemePreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellShape = RoundedCornerShape(10.dp)

    val backgroundBrush = when {
        isSelected -> Brush.verticalGradient(
            listOf(theme.arrowDefault, theme.arrowDefault)
        )
        isCompleted -> Brush.verticalGradient(
            listOf(Color(0xFF10B981), Color(0xFF059669))
        )
        isToday -> Brush.verticalGradient(
            listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))
        )
        else -> Brush.verticalGradient(
            listOf(theme.surface, theme.surface)
        )
    }

    val borderColor = when {
        isSelected -> theme.arrowDefault
        isToday -> Color(0xFF6366F1)
        isCompleted -> Color(0xFF047857)
        else -> theme.gridLine
    }

    val textColor = when {
        isSelected -> Color.White
        isCompleted -> Color.White
        isFuture -> theme.textSecondary.copy(alpha = 0.35f)
        isToday -> Color(0xFF4F46E5)
        else -> theme.textPrimary
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cellShape)
            .background(backgroundBrush)
            .border(
                width = if (isSelected || isToday) 1.5.dp else 1.dp,
                color = borderColor,
                shape = cellShape
            )
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dayNumber",
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected || isCompleted) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor
            )

            // Completion check indicator dot/icon
            if (isCompleted && !isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            } else if (isToday && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4F46E5))
                )
            }
        }
    }
}
