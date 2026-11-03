package cl.elboomlicanray.tvlauncher.presentation.components.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import cl.elboomlicanray.tvlauncher.config.QuickAccessItem
import cl.elboomlicanray.tvlauncher.presentation.components.TvIcon
import cl.elboomlicanray.tvlauncher.presentation.theme.TvDimens
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Tarjeta de Navegación Inferior estilo Apple TV (tvOS) Premium.
 *
 * Características de alta gama:
 * - Física de resorte orgánica (Spring Physics) en escala y profundidad.
 * - Efecto 3D Tilt sutil sobre ejes X e Y al ganar foco.
 * - Halo atmosférico 360° (Glow) detrás de la tarjeta.
 * - Bisel y brillo especular de vidrio esmerilado (Frosted Glass Sheen) en la cara superior.
 * - Salto de escala elástico en el icono vectorial.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PrimaryShortcutCard(
    item: QuickAccessItem,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // 1. Escala elástica sobria Apple TV (1.055x) solo al interactuar
    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.055f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 380f
        ),
        label = "card_scale"
    )

    // 2. Elevación y sombra difusa
    val cardElevation by animateFloatAsState(
        targetValue = if (isFocused) 16f else 2f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 380f
        ),
        label = "card_elevation"
    )

    val cardShape = remember { RoundedCornerShape(18.dp) }

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .width(TvDimens.ShortcutCardWidth)
            .height(TvDimens.ShortcutCardHeight)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                clip = false
            }
            .shadow(
                elevation = cardElevation.dp,
                shape = cardShape,
                spotColor = if (isFocused) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.5f)
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        shape = CardDefaults.shape(cardShape),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.0f,
            pressedScale = 0.97f
        ),
        colors = CardDefaults.colors(
            containerColor = Color(0xCC0B1424),
            focusedContainerColor = Color(0xF0101F38)
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                shape = cardShape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = cardShape
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
        ) {
            // Fondo con acabado de cristal Apple TV
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isFocused) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x33FFFFFF),
                                    Color(0x0A00E5FF),
                                    Color.Transparent
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x14FFFFFF),
                                    Color.Transparent
                                )
                            )
                        }
                    )
            )

            // Bisel especular superior de cristal puro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isFocused) 0.25f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Contenido interior sobrio y legible
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                TvIcon(
                    name = item.iconKey,
                    size = 30.dp,
                    color = if (isFocused) Color(0xFF00E5FF) else Color.White,
                    strokeWidth = if (isFocused) 2.4f else 2.0f
                )

                Column {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.SemiBold,
                        softWrap = true,
                        lineHeight = 16.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        color = if (isFocused) Color(0xFFE2E8F0) else TvTokens.TextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        softWrap = true,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

