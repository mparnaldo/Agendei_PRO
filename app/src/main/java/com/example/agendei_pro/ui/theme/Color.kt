package com.example.agendei_pro.ui.theme

import androidx.compose.ui.graphics.Color

// Definição das cores base
val GoldPrimary = Color(0xFFC5A059)
val DarkBackground = Color(0xFF121212)
val SurfaceGray = Color(0xFF1E1E1E)
val TextLight = Color(0xFFF5F5F5)

data class AgendeiTheme(
    val id: Int,
    val name: String,
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val isDark: Boolean
)

val ThemeLuxury = AgendeiTheme(1, "Luxo Dourado", Color(0xFFC5A059), Color(0xFF121212), Color(0xFF1E1E1E), Color.Black, true)
val ThemeRose = AgendeiTheme(2, "Rosa Quartzo", Color(0xFFEC407A), Color(0xFFFFF1F1), Color(0xFFFCE4EC), Color.White, false)
val ThemeBarber = AgendeiTheme(3, "Barber Shop", Color(0xFF1976D2), Color(0xFF0D1117), Color(0xFF161B22), Color.White, true)
val ThemeNature = AgendeiTheme(4, "Natureza", Color(0xFF4CAF50), Color(0xFFF1F8E9), Color.White, Color.White, false)
val ThemeWine = AgendeiTheme(5, "Vinho Tinto", Color(0xFF880E4F), Color(0xFF1A0010), Color(0xFF2D001B), Color.White, true)
val ThemeOcean = AgendeiTheme(6, "Oceano", Color(0xFF00ACC1), Color(0xFFE0F7FA), Color.White, Color.White, false)
val ThemeLavender = AgendeiTheme(7, "Lavanda", Color(0xFF9575CD), Color(0xFFF3E5F5), Color.White, Color.White, false)
val ThemeIndustrial = AgendeiTheme(8, "Industrial", Color(0xFFFF9800), Color(0xFF263238), Color(0xFF37474F), Color.Black, true)
val ThemeCoffee = AgendeiTheme(9, "Capuccino", Color(0xFF795548), Color(0xFFEFEBE9), Color(0xFFD7CCC8), Color.White, false)
val ThemeMinimalist = AgendeiTheme(10, "Minimalista", Color(0xFF212121), Color.White, Color(0xFFF5F5F5), Color.White, false)

val AllAgendeiThemes = listOf(ThemeLuxury, ThemeRose, ThemeBarber, ThemeNature, ThemeWine, ThemeOcean, ThemeLavender, ThemeIndustrial, ThemeCoffee, ThemeMinimalist)
