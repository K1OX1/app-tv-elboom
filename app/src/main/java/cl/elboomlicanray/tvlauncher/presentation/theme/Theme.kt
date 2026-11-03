package cl.elboomlicanray.tvlauncher.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val TvDarkColorScheme = darkColorScheme(
    primary = MinimalAccentGold,
    onPrimary = MinimalBackground,
    primaryContainer = MinimalSurface,
    onPrimaryContainer = TextWhite,
    secondary = MinimalAccentTeal,
    onSecondary = MinimalBackground,
    background = MinimalBackground,
    onBackground = TextWhite,
    surface = MinimalSurface,
    onSurface = TextWhite,
    surfaceVariant = MinimalSurfaceFocused,
    onSurfaceVariant = TextMuted,
    border = MinimalBorderIdle
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ElBoomTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = TvTypography,
        content = content
    )
}
