package com.example.agendei_pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Agendei_PROTheme(
    selectedTheme: AgendeiTheme = ThemeLuxury, // Nome corrigido!
    content: @Composable () -> Unit
) {
    val colorScheme = if (selectedTheme.isDark) {
        darkColorScheme(
            primary = selectedTheme.primary,
            onPrimary = selectedTheme.onPrimary,
            background = selectedTheme.background,
            surface = selectedTheme.surface,
            primaryContainer = selectedTheme.primary.copy(alpha = 0.2f),
            onPrimaryContainer = selectedTheme.primary
        )
    } else {
        lightColorScheme(
            primary = selectedTheme.primary,
            onPrimary = selectedTheme.onPrimary,
            background = selectedTheme.background,
            surface = selectedTheme.surface,
            primaryContainer = selectedTheme.primary.copy(alpha = 0.1f),
            onPrimaryContainer = selectedTheme.primary
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
