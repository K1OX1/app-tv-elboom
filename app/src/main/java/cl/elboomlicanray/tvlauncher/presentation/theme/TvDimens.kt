package cl.elboomlicanray.tvlauncher.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimensiones Centralizadas del Sistema Visual 10-Foot UI.
 *
 * Configurado exactamente para la composición de la referencia visual:
 * - 5 tarjetas de navegación inferiores del mismo tamaño
 * - Paneles superiores de bienvenida y TV en vivo alineados y de altura balanceada
 */
object TvDimens {

    // ==========================================
    // MÁRGENES SEGUROS DE TELEVISOR (OVERSCAN SAFE AREA)
    // ==========================================
    val SafeMarginHorizontal: Dp = 36.dp
    val SafeMarginTop: Dp = 18.dp
    val SafeMarginBottom: Dp = 20.dp

    // ==========================================
    // SECCIÓN HEADER
    // ==========================================
    val HeaderHeight: Dp = 46.dp
    val HeaderSpacing: Dp = 14.dp

    // ==========================================
    // SECCIÓN HERO (BIENVENIDA + EXOPLAYER TV)
    // ==========================================
    val HeroRowHeight: Dp = 268.dp
    val HeroRowSpacing: Dp = 18.dp

    // ==========================================
    // SECCIÓN ACCESOS DIRECTOS (EXACTAMENTE 5 TARJETAS)
    // ==========================================
    val ShortcutCardWidth: Dp = 164.dp
    val ShortcutCardHeight: Dp = 136.dp
    val ShortcutCardPadding: Dp = 12.dp
    val ShortcutRowSpacing: Dp = 16.dp

    // ==========================================
    // DIÁLOGOS MODALES
    // ==========================================
    val ModalDefaultWidth: Dp = 820.dp
    val ModalWideWidth: Dp = 960.dp
    val ModalMaxHeight: Dp = 560.dp
}
