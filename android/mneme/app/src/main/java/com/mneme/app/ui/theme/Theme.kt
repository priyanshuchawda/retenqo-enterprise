package com.mneme.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Retenqo is intentionally dark: a calm, private cockpit with a small amount
// of signal color reserved for actions and live state.
private val Ink = Color(0xFFEAF2EF)
private val Paper = Color(0xFF0B1011)
private val Sage = Color(0xFF72E0BE)
private val SageSoft = Color(0xFF153A31)
private val Mist = Color(0xFF151D1B)
private val Ember = Color(0xFFFFB38A)

private val DarkColors = darkColorScheme(
    primary = Sage,
    onPrimary = Color(0xFF06231B),
    primaryContainer = SageSoft,
    onPrimaryContainer = Color(0xFFB9F5DF),
    secondary = Ember,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A211A),
    onSecondaryContainer = Color(0xFFFFDCCB),
    background = Paper,
    onBackground = Ink,
    surface = Color(0xFF111819),
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF9BAFA8),
    outline = Color(0xFF33443E),
    outlineVariant = Color(0xFF22312C),
)

private val BrandTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
)

@Composable
fun MnemeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = BrandTypography,
    ) {
        Surface(
            color = DarkColors.background,
            contentColor = DarkColors.onBackground,
            content = content,
        )
    }
}
