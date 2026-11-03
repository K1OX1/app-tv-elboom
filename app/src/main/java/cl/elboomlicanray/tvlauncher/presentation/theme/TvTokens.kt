package cl.elboomlicanray.tvlauncher.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design Tokens del Sistema Visual Android TV "Alojamientos El Boom".
 *
 * Basado en paleta Midnight Navy, Neón Turquesa discreto (#00E5FF),
 * texto secundario gris azulado y bordes finos de cristal biselado.
 */
object TvTokens {

    // ==========================================
    // PALETA DE COLOR Y CRISTAL (GLASSMORPHISM)
    // ==========================================
    val CyanNeon = Color(0xFF00E5FF)
    val CyanBrand = CyanNeon
    val CyanGlow = Color(0x3300E5FF)
    val CyanSubtle = Color(0xFF38BDF8)
    val BlueSky = Color(0xFF38BDF8)

    val CanvasBackground = Color(0xFF030712)
    val BgDeepDark = Color(0xFF030816)

    val CardBackgroundRest = Color(0xCC091224)
    val CardBackgroundFocus = Color(0xEB0C1A33)
    val CardSurface = CardBackgroundRest
    val CardSurfaceFocused = CardBackgroundFocus
    val ModalSurface = Color(0xFF091426)
    val BgNavyPanel = ModalSurface

    val GlassBorderStroke = Color(0x2EFFFFFF) // Borde fino 1dp sutil
    val GlassBorderSubtle = Color(0x1FFFFFFF)

    val EmeraldSuccess = Color(0xFF10B981)
    val EmeraldSuccessText = Color(0xFF34D399)
    val AmberPending = Color(0xFFFFB800)
    val RedAlert = Color(0xFFDC2626)

    // ==========================================
    // TEXTO Y JERARQUÍA
    // ==========================================
    val TextWhite = Color(0xFFFFFFFF)
    val TextSlate = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)
    val TextSubtle = Color(0xFF64748B)

    // ==========================================
    // BORDES Y FOCO D-PAD
    // ==========================================
    val BorderIdle = GlassBorderStroke
    val BorderFocused = CyanNeon
    val FocusGlowColor = CyanGlow
    val FocusBorderWidthIdle = 1.dp
    val FocusBorderWidthActive = 2.dp

    // ==========================================
    // RADIOS DE ESQUINAS
    // ==========================================
    val RadiusCard = RoundedCornerShape(18.dp)
    val RadiusCardSmall = RoundedCornerShape(12.dp)
    val RadiusPill = RoundedCornerShape(50)
    val RadiusModal = RoundedCornerShape(22.dp)

    // ==========================================
    // GRADIENTES DE FONDO (ATMOSPHERE SCRIM)
    // ==========================================
    val AtmosphereScrim = Brush.horizontalGradient(
        0.0f to Color(0xF2030816),
        0.45f to Color(0xCC050E22),
        1.0f to Color(0xFF030816)
    )

    // ==========================================
    // MOTION D-PAD
    // ==========================================
    const val MotionDurationMs = 180
    val MotionEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    const val ScaleFocusedCard = 1.04f
}
