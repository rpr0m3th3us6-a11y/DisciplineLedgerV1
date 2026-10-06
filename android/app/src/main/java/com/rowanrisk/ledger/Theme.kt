package com.rowanrisk.ledger

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4A5637),
    onPrimary = Color(0xFFF3F1EA),
    secondary = Color(0xFF8A6D2B),
    background = Color(0xFFF3F1EA),
    onBackground = Color(0xFF1C1E1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1E1A),
    surfaceVariant = Color(0xFFEAE6D9),
    onSurfaceVariant = Color(0xFF5C5F56),
    outline = Color(0xFFD8D2C0),
    error = Color(0xFFA5453A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA564),
    onPrimary = Color(0xFF151710),
    secondary = Color(0xFFCFA956),
    background = Color(0xFF15170F),
    onBackground = Color(0xFFE9E7DA),
    surface = Color(0xFF1E2117),
    onSurface = Color(0xFFE9E7DA),
    surfaceVariant = Color(0xFF262A1D),
    onSurfaceVariant = Color(0xFFA3A693),
    outline = Color(0xFF3A3E2C),
    error = Color(0xFFD97A6C),
)

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
