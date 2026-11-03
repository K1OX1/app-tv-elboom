package cl.elboomlicanray.tvlauncher.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==============================================================
// PALETA MINIMALISTA ELEGANTE PARA ANDROID TV (BOUTIQUE CABINS)
// Máxima legibilidad, baja fatiga visual y cero ruido innecesario
// ==============================================================

// Fondos Mate Profundos (Quiet Luxury / Estilo Nórdico)
val MinimalBackground = Color(0xFF090D14)
val MinimalSurface = Color(0xFF131822)
val MinimalSurfaceFocused = Color(0xFF1C2433)
val MinimalSurfaceSubtle = Color(0xFF0F141E)

// Bordes Nítidos de Alto Contraste
val MinimalBorderIdle = Color(0xFF1F2937)
val MinimalBorderFocused = Color(0xFFFFFFFF)
val MinimalAccentGold = Color(0xFFE5B358)
val MinimalAccentTeal = Color(0xFF00C9A7)

// Tipografía Limpia (10-Foot UI)
val TextWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF94A3B8)
val TextSubtle = Color(0xFF64748B)

val TextHighEmphasis = TextWhite
val TextMediumEmphasis = TextMuted

// Aliases para compatibilidad con el resto de la arquitectura
val LiquidAbyss = MinimalBackground
val LiquidDeepSlate = MinimalSurface
val LiquidAuroraTeal = MinimalAccentTeal
val LiquidAuroraAmber = MinimalAccentGold
val LiquidAuroraCyan = MinimalAccentTeal
val LiquidAuroraViolet = Color(0xFF6366F1)
val LiquidGold = MinimalAccentGold
val TextPureWhite = TextWhite
val TextCrispSilver = Color(0xFFE2E8F0)
val TextMutedGlass = TextMuted
val TextDimGlass = TextSubtle
val GlassSurfaceDark = MinimalSurface
val GlassSurfaceFocused = MinimalSurfaceFocused
val GlassBorderGradientNormal = Brush.linearGradient(
    colors = listOf(
        Color(0x33FFFFFF),
        Color(0x10FFFFFF),
        Color(0x05000000),
        Color(0x20000000)
    )
)
val GlassBorderGradientFocused = Brush.linearGradient(
    colors = listOf(Color.White, MinimalAccentGold)
)
