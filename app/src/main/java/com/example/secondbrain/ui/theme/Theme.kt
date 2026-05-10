package com.example.secondbrain.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary        = NavyAccent,
    onPrimary      = NavyText,
    background     = NavyDeep,
    surface        = NavySurface,
    onBackground   = NavyText,
    onSurface      = NavyText,
    outline        = NavyTextFaint,
    error          = UrgentRed,
)

private val LightColorScheme = lightColorScheme(
    primary        = SkyAccent,
    onPrimary      = SkySurface,
    background     = SkyBg,
    surface        = SkySurface,
    onBackground   = SkyText,
    onSurface      = SkyText,
    outline        = SkyTextFaint,
    error          = UrgentRedLight,
)

@Composable
fun SecondBrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
