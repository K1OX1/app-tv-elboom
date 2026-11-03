package cl.elboomlicanray.tvlauncher.presentation.components.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import cl.elboomlicanray.tvlauncher.R
import cl.elboomlicanray.tvlauncher.config.QuickAccessItem
import cl.elboomlicanray.tvlauncher.presentation.components.TvIcon
import cl.elboomlicanray.tvlauncher.presentation.theme.TvDimens
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Tarjeta de Navegación Inferior (PrimaryShortcutCard).
 *
 * Basada fielmente en la imagen de referencia:
 * - Proporciones exactas de 5 tarjetas simétricas.
 * - Fondo azul marino oscuro y bordes finos.
 * - Iconos limpios: TV, apps grid, alojamiento, ubicación pin e info.
 * - Foco D-Pad sutil con borde y fondo turquesa (#00E5FF), sin brillos exagerados.
 * - Transición suave y breve (180ms).
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

    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = TvTokens.MotionDurationMs),
        label = "card_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) TvTokens.CyanNeon else Color(0x2EFFFFFF),
        animationSpec = tween(durationMillis = TvTokens.MotionDurationMs),
        label = "card_border"
    )

    val cardShape = RoundedCornerShape(16.dp)

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .width(TvDimens.ShortcutCardWidth)
            .height(TvDimens.ShortcutCardHeight)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        shape = CardDefaults.shape(cardShape),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.0f,
            pressedScale = 0.98f
        ),
        colors = CardDefaults.colors(
            containerColor = Color(0xCC091224),
            focusedContainerColor = Color(0xFA0A162B)
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                shape = cardShape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvTokens.CyanNeon),
                shape = cardShape
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFocused) {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x3D00E5FF),
                                    Color(0x1400E5FF)
                                )
                            )
                        )
                    } else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Icono vectorial limpio y visible sin cajas intermedias, idéntico a la referencia
                TvIcon(
                    name = item.iconKey,
                    size = 32.dp,
                    color = if (isFocused) TvTokens.CyanNeon else Color.White,
                    strokeWidth = 2.4f
                )

                // Títulos alineados y legibles desde el sofá
                Column {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = true,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        color = if (isFocused) Color(0xFFE2E8F0) else TvTokens.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        softWrap = true,
                        lineHeight = 13.5.sp
                    )
                }
            }
        }
    }
}
