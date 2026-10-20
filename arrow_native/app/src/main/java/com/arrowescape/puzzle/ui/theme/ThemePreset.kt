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
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        gridDot = Color(0xFFCBD5E1),
        gridLine = Color(0xFFE2E8F0),
        arrowDefault = Color(0xFF1E293B), // Dark navy / slate 800
        arrowColors = listOf(
            Color(0xFF1E293B) // All arrows use uniform dark slate in original
        ),
        arrowHint = Color(0xFFF59E0B), // Golden yellow
        arrowWrong = Color(0xFFEF4444), // Crimson red
        arrowClear = Color(0xFF1E293B), // Dark navy / slate
        textPrimary = Color(0xFF1E293B),
        textSecondary = Color(0xFF64748B),
        boosterBg = Color(0xFFEEF2F6),
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
        boosterBg = Color(0xFFF3E8FF),
        isDark = false
    )

    val WarmSepia = ThemePreset(
        name = "Warm Sepia",
        background = Color(0xFFFAF2E6), // Authentic New Unity One Linen Paper
        surface = Color(0xFFFFFDF9),
        gridDot = Color(0xFFD6C7B2),
        gridLine = Color(0xFFEFE3D3),
        arrowDefault = Color(0xFF7A4A28), // Warm Hazelnut Mocha (matches New Unity One)
        arrowColors = listOf(
            Color(0xFF7A4A28)
        ),
        arrowHint = Color(0xFFE09F3E),
        arrowWrong = Color(0xFFD94E34),
        arrowClear = Color(0xFF7A4A28),
        textPrimary = Color(0xFF6F3F1F),
        textSecondary = Color(0xFF9E7C62),
        boosterBg = Color(0xFFF3E8D8),
        isDark = false
    )

    val MintFresh = ThemePreset(
        name = "Mint Fresh",
        background = Color(0xFFF0FDF4),
        surface = Color(0xFFFFFFFF),
        gridDot = Color(0xFFA7F3D0),
        gridLine = Color(0xFFDCFCE7),
        arrowDefault = Color(0xFF059669),
        arrowColors = listOf(
            Color(0xFF059669)
        ),
        arrowHint = Color(0xFFD97706),
        arrowWrong = Color(0xFFE11D48),
        arrowClear = Color(0xFF10B981),
        textPrimary = Color(0xFF064E3B),
        textSecondary = Color(0xFF047857),
        boosterBg = Color(0xFFDCFCE7),
        isDark = false
    )

    val SunsetRose = ThemePreset(
        name = "Sunset Rose",
        background = Color(0xFFFFF1F2),
        surface = Color(0xFFFFFFFF),
        gridDot = Color(0xFFFECDD3),
        gridLine = Color(0xFFFFE4E6),
        arrowDefault = Color(0xFFE11D48),
        arrowColors = listOf(
            Color(0xFFE11D48)
        ),
        arrowHint = Color(0xFFD97706),
        arrowWrong = Color(0xFFDC2626),
        arrowClear = Color(0xFF059669),
        textPrimary = Color(0xFF881337),
        textSecondary = Color(0xFFBE123C),
        boosterBg = Color(0xFFFFE4E6),
        isDark = false
    )

    val all = listOf(CleanLight, WarmSepia, MidnightDark, CalmPastel, MintFresh, SunsetRose)
}
