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

    // 1. Escala con física elástica de Apple TV (Spring Physics)
    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.085f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 340f
        ),
        label = "card_scale"
    )

    // 2. Elevación y sombra flotante 3D
    val cardElevation by animateFloatAsState(
        targetValue = if (isFocused) 24f else 2f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 340f
        ),
        label = "card_elevation"
    )

    // 3. Inclinación 3D (Tilt Parallax)
    val tiltRotationX by animateFloatAsState(
        targetValue = if (isFocused) -1.8f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
        label = "tilt_x"
    )
    val tiltRotationY by animateFloatAsState(
        targetValue = if (isFocused) 1.2f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
        label = "tilt_y"
    )

    // 4. Escala dinámica del icono interior
    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        label = "icon_scale"
    )

    // 5. Animación de brillo especular continuo en foco (Shimmer diagonal sutil)
    val infiniteTransition = rememberInfiniteTransition(label = "card_sheen")
    val sheenProgress by infiniteTransition.animateFloat(
        initialValue = -0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen_anim"
    )

    val cardShape = remember { RoundedCornerShape(20.dp) }

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .width(TvDimens.ShortcutCardWidth)
            .height(TvDimens.ShortcutCardHeight)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                rotationX = tiltRotationX
                rotationY = tiltRotationY
                cameraDistance = 14f * density
                clip = false
            }
            .then(
                if (isFocused) {
                    Modifier.drawBehind {
                        // Halo de luz difusa 360° detrás de la tarjeta
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    TvTokens.CyanNeon.copy(alpha = 0.40f),
                                    TvTokens.CyanNeon.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.maxDimension * 0.90f
                            ),
                            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .shadow(
                elevation = cardElevation.dp,
                shape = cardShape,
                spotColor = if (isFocused) TvTokens.CyanNeon.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.6f)
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        shape = CardDefaults.shape(cardShape),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.0f,
            pressedScale = 0.96f
        ),
        colors = CardDefaults.colors(
            containerColor = Color(0xCC091428),
            focusedContainerColor = Color(0xFA0B1A35)
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                shape = cardShape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.2.dp, TvTokens.CyanNeon),
                shape = cardShape
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
        ) {
            // Fondo con degradado reactivo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isFocused) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x4D00E5FF),
                                    Color(0x1A00E5FF),
                                    Color(0x0500E5FF)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x1FFFFFFF),
                                    Color.Transparent
                                )
                            )
                        }
                    )
            )

            // Bisel especular superior tipo cristal Apple TV
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isFocused) 0.30f else 0.12f),
                                Color.White.copy(alpha = if (isFocused) 0.08f else 0.02f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Reflejo diagonal de luz (Sheen sweep) cuando tiene foco
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val startX = size.width * (sheenProgress - 0.4f)
                            val endX = size.width * (sheenProgress + 0.4f)
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.14f),
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.14f),
                                        Color.Transparent
                                    ),
                                    start = Offset(startX, 0f),
                                    end = Offset(endX, size.height)
                                )
                            )
                        }
                )
            }

            // Contenido interior
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Icono vectorial con salto elástico
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                ) {
                    TvIcon(
                        name = item.iconKey,
                        size = 32.dp,
                        color = if (isFocused) TvTokens.CyanNeon else Color.White,
                        strokeWidth = if (isFocused) 2.6f else 2.2f
                    )
                }

                // Textos con tipografía Apple TV nítida
                Column {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                        softWrap = true,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(2.5.dp))
                    Text(
                        text = item.subtitle,
                        color = if (isFocused) Color(0xFFF1F5F9) else TvTokens.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        softWrap = true,
                        lineHeight = 13.5.sp
                    )
                }
            }
        }
    }
}

