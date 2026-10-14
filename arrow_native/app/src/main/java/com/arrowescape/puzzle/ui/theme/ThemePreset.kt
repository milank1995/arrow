package com.arrowescape.puzzle.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemePreset(
    val name: String,
    val background: Color,
    val surface: Color,
    val gridDot: Color,
    val gridLine: Color,
    val arrowDefault: Color,
    val arrowColors: List<Color>,
    val arrowHint: Color,
    val arrowWrong: Color,
    val arrowClear: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val boosterBg: Color,
    val isDark: Boolean
)

object ThemePresets {
    val CleanLight = ThemePreset(
        name = "Clean Light",
        background = Color(0xFFF1F5F9), // Slate 100
        surface = Color(0xFFFFFFFF),
        gridDot = Color(0xFFCBD5E1),
        gridLine = Color(0xFFE2E8F0),
        arrowDefault = Color(0xFF2563EB), // Blue 600
        arrowColors = listOf(
            Color(0xFF2563EB), // Blue
            Color(0xFF0D9488), // Teal
            Color(0xFF7C3AED), // Violet
            Color(0xFFD97706)  // Amber
        ),
        arrowHint = Color(0xFFF59E0B), // Golden yellow
        arrowWrong = Color(0xFFEF4444), // Crimson red
        arrowClear = Color(0xFF10B981), // Emerald green
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF64748B),
        boosterBg = Color(0xFFFFFFFF),
        isDark = false
    )

    val MidnightDark = ThemePreset(
        name = "Midnight Dark",
        background = Color(0xFF0F172A), // Slate 900
        surface = Color(0xFF1E293B),
        gridDot = Color(0xFF334155),
        gridLine = Color(0xFF1E293B),
        arrowDefault = Color(0xFF38BDF8), // Sky 400
        arrowColors = listOf(
            Color(0xFF38BDF8), // Electric cyan
            Color(0xFF34D399), // Mint
            Color(0xFFA78BFA), // Lavender
            Color(0xFFFBBF24)  // Gold
        ),
        arrowHint = Color(0xFFFBBF24),
        arrowWrong = Color(0xFFF87171),
        arrowClear = Color(0xFF34D399),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        boosterBg = Color(0xFF1E293B),
        isDark = true
    )

    val CalmPastel = ThemePreset(
        name = "Calm Pastel",
        background = Color(0xFFFAF5FF), // Soft purple tint
        surface = Color(0xFFFFFFFF),
        gridDot = Color(0xFFE9D5FF),
        gridLine = Color(0xFFF3E8FF),
        arrowDefault = Color(0xFF9333EA),
        arrowColors = listOf(
            Color(0xFF8B5CF6),
            Color(0xFFEC4899),
            Color(0xFF06B6D4),
            Color(0xFFF59E0B)
        ),
        arrowHint = Color(0xFFFBBF24),
        arrowWrong = Color(0xFFF43F5E),
        arrowClear = Color(0xFF10B981),
        textPrimary = Color(0xFF3B0764),
        textSecondary = Color(0xFF7E22CE),
        boosterBg = Color(0xFFFFFFFF),
        isDark = false
    )

    val all = listOf(CleanLight, MidnightDark, CalmPastel)
}
