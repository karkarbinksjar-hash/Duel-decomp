package com.yugioh.recomp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MillenniumGold = Color(0xFFFFD700)
val DarkFieldBackground = Color(0xFF0F111A)
val SurfaceDark = Color(0xFF1A1C29)
val CardGold = Color(0xFFD4AF37)
val ZoneBorderGreen = Color(0xFF2E634A)
val ZoneFillGreen = Color(0xFF13281E)
val AttackRed = Color(0xFFEF4444)
val DefenseBlue = Color(0xFF3B82F6)
val LifePointGreen = Color(0xFF22C55E)

private val DarkColorScheme = darkColorScheme(
    primary = MillenniumGold,
    secondary = CardGold,
    tertiary = ZoneBorderGreen,
    background = DarkFieldBackground,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun YuGiOhRecompTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
