package cl.elboomlicanray.tvlauncher.presentation.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.domain.model.AppItem

/**
 * Pintor nativo de alta fidelidad para recursos Drawable de Android.
 * Dibuja directamente sobre el canvas de hardware sin depender de red ni cachés externas.
 */
class AndroidDrawablePainter(private val drawable: Drawable) : Painter() {
    override val intrinsicSize: Size
        get() = if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
            Size(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        } else {
            Size.Unspecified
        }

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

@Composable
fun rememberNativeDrawablePainter(drawable: Drawable?): Painter? {
    return remember(drawable) {
        drawable?.let { AndroidDrawablePainter(it.mutate()) }
    }
}

/**
 * Tarjeta Oficial de Aplicación (Diseño idéntico a Apple TV tvOS y Projectivy Launcher).
 *
 * Utiliza los LOGOTIPOS Y BANNERS OFICIALES entregados por cada APK instalada:
 * 1. Banner Oficial 16:9 de Android TV ocupando toda la cápsula (ContentScale.Crop).
 * 2. Si no tiene banner panorámico, proyecta su ÍCONO OFICIAL en grande (68dp) centrado
 *    sobre una cápsula de cristal oscuro con bisel especular 3D de Apple TV.
 * 3. Cero logotipos inventados o textos simulados: 100% recursos originales de la app instalada.
 * 4. Foco fluido a 60 FPS con escala 1.12x, resplandor atmosférico 360° sin recortes y nombre centrado debajo.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppleTvAppCard(
    app: AppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 154.dp,
    cardHeight: Dp = 96.dp,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 380f
        ),
        label = "appletv_scale"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isFocused) 16f else 2f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 380f
        ),
        label = "appletv_elevation"
    )

    val shape = remember { RoundedCornerShape(18.dp) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = false
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            // Padding generoso para que el resplandor de foco y escala NUNCA se corten
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // Cápsula de la Tarjeta con Proporción 16:10 y Resplandor 360°
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .then(
                    if (isFocused) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color(0x3538BDF8),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.maxDimension * 0.85f
                                ),
                                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                            )
                        }
                    } else Modifier
                )
                .shadow(
                    elevation = elevation.dp,
                    shape = shape,
                    spotColor = if (isFocused) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.5f)
                )
                .background(Color(0xFF1E293B), shape = shape)
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.18f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            ) {
                val bannerPainter = rememberNativeDrawablePainter(app.bannerDrawable)
                val iconPainter = rememberNativeDrawablePainter(app.iconDrawable)

                when {
                    // 1. BANNER OFICIAL 16:9 ENTREGADO POR LA APP INSTALADA (ESTILO PROJECTIVY LAUNCHER)
                    bannerPainter != null -> {
                        Image(
                            painter = bannerPainter,
                            contentDescription = app.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // 2. ÍCONO OFICIAL ENTREGADO POR LA APP EN GRANDE (68DP) SOBRE CRISTAL OSCURO
                    iconPainter != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF24334D), Color(0xFF131D2D))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = iconPainter,
                                contentDescription = app.name,
                                modifier = Modifier.size(68.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // 3. FALLBACK DE TEXTO NATIVO SOLO SI NO EXISTE NINGÚN RECURSO GRÁFICO
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bisel Especular Superior de Cristal (Efecto Vidrio 3D Apple TV tvOS)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isFocused) 0.32f else 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Nombre de la App centrado debajo (estilo Apple TV tvOS)
        Text(
            text = app.name,
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.70f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(cardWidth + 12.dp)
        )
    }
}
