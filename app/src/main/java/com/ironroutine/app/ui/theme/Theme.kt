package com.ironroutine.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Палитра IRON ROUTINE. */
object IronColors {
    val Background = Color(0xFF101010)
    val Card = Color(0xFF1C1C1C)
    val Surface2 = Color(0xFF252525)
    val Outline = Color(0xFF383838)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA0A0A0)
    val Accent = Color(0xFFB6F36B)
    val OnAccent = Color(0xFF101010)
    val Warning = Color(0xFFFF9F43)
    val Danger = Color(0xFFFF5C5C)
}

private val IronColorScheme = darkColorScheme(
    primary = IronColors.Accent,
    onPrimary = IronColors.OnAccent,
    primaryContainer = IronColors.Surface2,
    onPrimaryContainer = IronColors.Accent,
    secondary = IronColors.Accent,
    onSecondary = IronColors.OnAccent,
    background = IronColors.Background,
    onBackground = IronColors.TextPrimary,
    surface = IronColors.Card,
    onSurface = IronColors.TextPrimary,
    surfaceVariant = IronColors.Surface2,
    onSurfaceVariant = IronColors.TextSecondary,
    surfaceContainer = IronColors.Card,
    surfaceContainerHigh = IronColors.Surface2,
    surfaceContainerHighest = IronColors.Surface2,
    surfaceContainerLow = IronColors.Card,
    surfaceContainerLowest = IronColors.Background,
    error = IronColors.Danger,
    onError = IronColors.TextPrimary,
    outline = IronColors.Outline,
    outlineVariant = IronColors.Outline
)

private val IronTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

private val IronShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun IronRoutineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IronColorScheme,
        typography = IronTypography,
        shapes = IronShapes,
        content = content
    )
}
