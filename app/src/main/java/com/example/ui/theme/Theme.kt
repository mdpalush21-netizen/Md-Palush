package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightSurveyColorScheme = lightColorScheme(
    primary = SurveyPrimary,
    onPrimary = SurveyOnPrimary,
    primaryContainer = SurveyPrimaryContainer,
    onPrimaryContainer = SurveyOnPrimaryContainer,
    secondary = SurveySecondary,
    onSecondary = SurveyOnSecondary,
    secondaryContainer = SurveySecondaryContainer,
    onSecondaryContainer = SurveyOnSecondaryContainer,
    tertiary = SurveyTertiary,
    onTertiary = SurveyOnTertiary,
    tertiaryContainer = SurveyTertiaryContainer,
    onTertiaryContainer = SurveyOnTertiaryContainer,
    background = SurveyBackground,
    onBackground = SurveyOnBackground,
    surface = SurveySurface,
    onSurface = SurveyOnSurface,
    surfaceVariant = SurveySurfaceVariant,
    onSurfaceVariant = SurveyOnSurfaceVariant,
    outline = SurveyOutline,
    outlineVariant = SurveyOutline,
    error = SurveyError,
    onError = SurveyOnError,
    errorContainer = SurveyErrorContainer,
    onErrorContainer = SurveyOnErrorContainer,
)

private val DarkSurveyColorScheme = darkColorScheme(
    primary = DarkSurveyPrimary,
    onPrimary = DarkSurveyOnPrimary,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFB923C),
    onTertiary = Color(0xFF431407),
    tertiaryContainer = Color(0xFF7C2D12),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = DarkSurveyBackground,
    onBackground = Color(0xFFE2E8F0),
    surface = DarkSurveySurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurveySurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DarkSurveyOutline,
    outlineVariant = DarkSurveyOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)

val SurveyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun HowladerSurveyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkSurveyColorScheme else LightSurveyColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SurveyShapes,
        content = content,
    )
}
