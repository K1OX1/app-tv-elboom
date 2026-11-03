package cl.elboomlicanray.tvlauncher.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sistema Responsive Centralizado para Android TV.
 *
 * Responsabilidad:
 * Gestionar breakpoints de resolución y densidad para Smart TVs:
 * - 720p (HD Ready: 1280x720)
 * - 1080p (Full HD de referencia: 1920x1080)
 * - 4K (Ultra HD: 3840x2160)
 *
 * REGLA:
 * Responsive no significa encoger fuentes hasta que sean ilegibles.
 * Permite adaptar espaciados y dimensiones mínimas preservando legibilidad 10-foot.
 */
object TvResponsive {

    enum class TvResolutionCategory {
        HD_720P,
        FULL_HD_1080P,
        ULTRA_HD_4K
    }

    @Composable
    fun getResolutionCategory(): TvResolutionCategory {
        val config = LocalConfiguration.current
        val widthDp = config.screenWidthDp
        return when {
            widthDp < 900 -> TvResolutionCategory.HD_720P
            widthDp > 1600 -> TvResolutionCategory.ULTRA_HD_4K
            else -> TvResolutionCategory.FULL_HD_1080P
        }
    }

    @Composable
    fun adaptiveHorizontalPadding(): Dp {
        return when (getResolutionCategory()) {
            TvResolutionCategory.HD_720P -> 32.dp
            TvResolutionCategory.FULL_HD_1080P -> TvDimens.SafeMarginHorizontal
            TvResolutionCategory.ULTRA_HD_4K -> 54.dp
        }
    }
}
