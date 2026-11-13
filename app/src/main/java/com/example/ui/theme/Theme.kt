package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ReadingThemeMode

data class BayanSemanticColors(
    val isDark: Boolean,
    val criticalRed: Color,
    val criticalRedBg: Color,
    val minorOrange: Color,
    val minorOrangeBg: Color,
    val masteryGreen: Color,
    val masteryGreenBg: Color,
    val amberAccent: Color,
    val amberAccentBg: Color,
    val cardBorder: Color
)

val LocalBayanColors = staticCompositionLocalOf {
    BayanSemanticColors(
        isDark = false,
        criticalRed = CriticalRed,
        criticalRedBg = CriticalRedLight,
        minorOrange = MinorOrange,
        minorOrangeBg = MinorOrangeLight,
        masteryGreen = MasteryGreen,
        masteryGreenBg = MasteryGreenLight,
        amberAccent = AntiqueAmber,
        amberAccentBg = AntiqueAmberContainer,
        cardBorder = WarmPaperBorder
    )
}

private val WarmPaperColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = Color.White,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = OliveSecondary,
    onSecondary = Color.White,
    secondaryContainer = OliveSecondaryContainer,
    onSecondaryContainer = CharcoalInk,
    tertiary = AntiqueAmber,
    onTertiary = Color.White,
    tertiaryContainer = AntiqueAmberContainer,
    onTertiaryContainer = CharcoalInk,
    background = WarmPaperBg,
    onBackground = CharcoalInk,
    surface = WarmPaperCard,
    onSurface = CharcoalInk,
    surfaceVariant = WarmPaperSurface,
    onSurfaceVariant = MutedInk,
    outline = WarmPaperBorder,
    error = CriticalRed,
    errorContainer = CriticalRedLight
)

private val EyeComfortColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = Color.White,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = OliveSecondary,
    onSecondary = Color.White,
    secondaryContainer = OliveSecondaryContainer,
    onSecondaryContainer = EyeComfortInk,
    tertiary = AntiqueAmber,
    onTertiary = Color.White,
    tertiaryContainer = AntiqueAmberContainer,
    onTertiaryContainer = EyeComfortInk,
    background = EyeComfortBg,
    onBackground = EyeComfortInk,
    surface = EyeComfortCard,
    onSurface = EyeComfortInk,
    surfaceVariant = EyeComfortSurface,
    onSurfaceVariant = Color(0xFF5D503F),
    outline = Color(0xFFD5C7AE),
    error = CriticalRed,
    errorContainer = CriticalRedLight
)

private val CalmDarkColorScheme = darkColorScheme(
    primary = CalmDarkPrimary,
    onPrimary = Color(0xFF142921),
    primaryContainer = Color(0xFF284237),
    onPrimaryContainer = Color(0xFFD4EAE0),
    secondary = Color(0xFFA6B99F),
    onSecondary = Color(0xFF1B2419),
    secondaryContainer = Color(0xFF2E3B2B),
    onSecondaryContainer = CalmDarkInk,
    tertiary = Color(0xFFD7A56B),
    onTertiary = Color(0xFF2E1D07),
    tertiaryContainer = Color(0xFF473116),
    onTertiaryContainer = Color(0xFFF5E3C9),
    background = CalmDarkBg,
    onBackground = CalmDarkInk,
    surface = CalmDarkCard,
    onSurface = CalmDarkInk,
    surfaceVariant = CalmDarkSurface,
    onSurfaceVariant = CalmDarkMuted,
    outline = Color(0xFF38443E),
    error = Color(0xFFEF9A9A),
    errorContainer = CriticalRedDarkBg
)

val BayanShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun BayanEnglishTheme(
    themeMode: ReadingThemeMode = ReadingThemeMode.WARM_PAPER,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ReadingThemeMode.WARM_PAPER -> WarmPaperColorScheme
        ReadingThemeMode.EYE_COMFORT -> EyeComfortColorScheme
        ReadingThemeMode.CALM_DARK -> CalmDarkColorScheme
    }

    val semanticColors = if (themeMode == ReadingThemeMode.CALM_DARK) {
        BayanSemanticColors(
            isDark = true,
            criticalRed = Color(0xFFEF9A9A),
            criticalRedBg = CriticalRedDarkBg,
            minorOrange = Color(0xFFFFB74D),
            minorOrangeBg = MinorOrangeDarkBg,
            masteryGreen = Color(0xFF81C784),
            masteryGreenBg = Color(0xFF1D3B2B),
            amberAccent = Color(0xFFD7A56B),
            amberAccentBg = Color(0xFF43301A),
            cardBorder = Color(0xFF38443E)
        )
    } else {
        BayanSemanticColors(
            isDark = false,
            criticalRed = CriticalRed,
            criticalRedBg = CriticalRedLight,
            minorOrange = MinorOrange,
            minorOrangeBg = MinorOrangeLight,
            masteryGreen = MasteryGreen,
            masteryGreenBg = MasteryGreenLight,
            amberAccent = AntiqueAmber,
            amberAccentBg = AntiqueAmberContainer,
            cardBorder = if (themeMode == ReadingThemeMode.EYE_COMFORT) Color(0xFFD5C7AE) else WarmPaperBorder
        )
    }

    CompositionLocalProvider(LocalBayanColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BayanTypography,
            shapes = BayanShapes,
            content = content
        )
    }
}
