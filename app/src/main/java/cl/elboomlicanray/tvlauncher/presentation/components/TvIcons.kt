package cl.elboomlicanray.tvlauncher.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Iconografía vectorial nativa y consistente para Android TV (Smart TV Launcher).
 * Sustituye completamente el uso de emojis por iconos limpios de trazo uniforme (estilo Lucide / Leanback TV).
 */
@Composable
fun TvIcon(
    name: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Float = 2.2f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = strokeWidth * density, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (name.lowercase()) {
            "home", "inicio" -> {
                // Casa moderna outline idéntica a la referencia
                val path = Path().apply {
                    moveTo(w * 0.16f, h * 0.44f)
                    lineTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.84f, h * 0.44f)
                    lineTo(w * 0.84f, h * 0.84f)
                    lineTo(w * 0.58f, h * 0.84f)
                    lineTo(w * 0.58f, h * 0.56f)
                    lineTo(w * 0.42f, h * 0.56f)
                    lineTo(w * 0.42f, h * 0.84f)
                    lineTo(w * 0.16f, h * 0.84f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            "apps", "grid" -> {
                // Cuadrícula 4 cuadrados (2x2) idéntica a la referencia
                val padX = w * 0.15f
                val padY = h * 0.15f
                val gap = w * 0.14f
                val boxSize = (w - (padX * 2) - gap) / 2
                val corner = CornerRadius(5f, 5f)
                drawRoundRect(color, Offset(padX, padY), Size(boxSize, boxSize), corner, stroke)
                drawRoundRect(color, Offset(padX + boxSize + gap, padY), Size(boxSize, boxSize), corner, stroke)
                drawRoundRect(color, Offset(padX, padY + boxSize + gap), Size(boxSize, boxSize), corner, stroke)
                drawRoundRect(color, Offset(padX + boxSize + gap, padY + boxSize + gap), Size(boxSize, boxSize), corner, stroke)
            }
            "bed", "stay", "estancia" -> {
                // Cama de hotel outline
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.35f)
                    lineTo(w * 0.15f, h * 0.80f)
                    moveTo(w * 0.85f, h * 0.55f)
                    lineTo(w * 0.85f, h * 0.80f)
                    moveTo(w * 0.15f, h * 0.65f)
                    lineTo(w * 0.85f, h * 0.65f)
                    moveTo(w * 0.25f, h * 0.50f)
                    lineTo(w * 0.45f, h * 0.50f)
                }
                drawPath(path, color, style = stroke)
            }
            "bell", "services", "servicios" -> {
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.78f)
                    lineTo(w * 0.85f, h * 0.78f)
                    moveTo(w * 0.20f, h * 0.78f)
                    cubicTo(w * 0.20f, h * 0.40f, w * 0.80f, h * 0.40f, w * 0.80f, h * 0.78f)
                    moveTo(w * 0.50f, h * 0.40f)
                    lineTo(w * 0.50f, h * 0.28f)
                    moveTo(w * 0.42f, h * 0.28f)
                    lineTo(w * 0.58f, h * 0.28f)
                }
                drawPath(path, color, style = stroke)
            }
            "info", "informacion", "rules" -> {
                // Círculo con 'i' idéntico a la referencia
                drawCircle(color, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(
                    color,
                    Offset(w * 0.5f, h * 0.33f),
                    Offset(w * 0.5f, h * 0.36f),
                    strokeWidth = strokeWidth * density * 1.6f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color,
                    Offset(w * 0.5f, h * 0.46f),
                    Offset(w * 0.5f, h * 0.67f),
                    strokeWidth = strokeWidth * density,
                    cap = StrokeCap.Round
                )
            }
            "map", "mapa", "guide" -> {
                // Marcador pin de mapa idéntico a la referencia
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.84f)
                    lineTo(w * 0.25f, h * 0.46f)
                    cubicTo(w * 0.17f, h * 0.30f, w * 0.83f, h * 0.30f, w * 0.75f, h * 0.46f)
                    close()
                }
                drawPath(path, color, style = stroke)
                drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.38f), style = stroke)
            }
            "tv", "live" -> {
                // Monitor de televisión con pantalla + soporte central + base horizontal (estilo referencia)
                val corner = CornerRadius(5f, 5f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.16f),
                    size = Size(w * 0.76f, h * 0.54f),
                    cornerRadius = corner,
                    style = stroke
                )
                // Cuello soporte central
                drawLine(
                    color = color,
                    start = Offset(w * 0.50f, h * 0.70f),
                    end = Offset(w * 0.50f, h * 0.84f),
                    strokeWidth = strokeWidth * density,
                    cap = StrokeCap.Round
                )
                // Base horizontal inferior
                drawLine(
                    color = color,
                    start = Offset(w * 0.32f, h * 0.84f),
                    end = Offset(w * 0.68f, h * 0.84f),
                    strokeWidth = strokeWidth * density,
                    cap = StrokeCap.Round
                )
            }
            "wifi" -> {
                // Arcos Wi-Fi
                val center = Offset(w * 0.5f, h * 0.80f)
                drawCircle(color, radius = 2.5f * density, center = center)
                drawArc(
                    color = color,
                    startAngle = 215f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(w * 0.32f, h * 0.55f),
                    size = Size(w * 0.36f, h * 0.36f),
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = 215f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.35f),
                    size = Size(w * 0.68f, h * 0.68f),
                    style = stroke
                )
            }
            "sun" -> {
                // Sol para clima
                drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                val len = w * 0.12f
                // Rayos
                drawLine(color, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.12f + len), strokeWidth = strokeWidth * density, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.5f, h * 0.88f), Offset(w * 0.5f, h * 0.88f - len), strokeWidth = strokeWidth * density, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.12f + len, h * 0.5f), strokeWidth = strokeWidth * density, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.88f, h * 0.5f), Offset(w * 0.88f - len, h * 0.5f), strokeWidth = strokeWidth * density, cap = StrokeCap.Round)
            }
            "music" -> {
                // Nota musical
                val path = Path().apply {
                    moveTo(w * 0.35f, h * 0.70f)
                    lineTo(w * 0.35f, h * 0.25f)
                    lineTo(w * 0.75f, h * 0.15f)
                    lineTo(w * 0.75f, h * 0.60f)
                }
                drawPath(path, color, style = stroke)
                drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.28f, h * 0.72f))
                drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.68f, h * 0.62f))
            }
            "globe", "idioma" -> {
                // Globo terráqueo
                drawCircle(color, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(color, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth = strokeWidth * density * 0.9f)
                val ellipsePath = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    cubicTo(w * 0.28f, h * 0.25f, w * 0.28f, h * 0.75f, w * 0.5f, h * 0.88f)
                    cubicTo(w * 0.72f, h * 0.75f, w * 0.72f, h * 0.25f, w * 0.5f, h * 0.12f)
                }
                drawPath(ellipsePath, color, style = stroke)
            }
            "calendar", "calendario" -> {
                // Calendario
                val corner = CornerRadius(4f, 4f)
                drawRoundRect(color, Offset(w * 0.15f, h * 0.22f), Size(w * 0.70f, h * 0.65f), corner, stroke)
                drawLine(color, Offset(w * 0.15f, h * 0.42f), Offset(w * 0.85f, h * 0.42f), strokeWidth = strokeWidth * density * 0.9f)
                drawLine(color, Offset(w * 0.35f, h * 0.12f), Offset(w * 0.35f, h * 0.26f), strokeWidth = strokeWidth * density * 1.2f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.65f, h * 0.12f), Offset(w * 0.65f, h * 0.26f), strokeWidth = strokeWidth * density * 1.2f, cap = StrokeCap.Round)
            }
            "card", "payment", "tarjeta", "saldo" -> {
                // Tarjeta de crédito limpia con banda magnética y chip
                val corner = CornerRadius(4f, 4f)
                drawRoundRect(color, Offset(w * 0.12f, h * 0.24f), Size(w * 0.76f, h * 0.52f), corner, stroke)
                drawLine(color, Offset(w * 0.12f, h * 0.40f), Offset(w * 0.88f, h * 0.40f), strokeWidth = strokeWidth * density * 1.1f)
                drawRoundRect(color, Offset(w * 0.22f, h * 0.52f), Size(w * 0.18f, h * 0.14f), CornerRadius(2f, 2f), stroke)
            }
            "check" -> {
                // Checkmark
                val path = Path().apply {
                    moveTo(w * 0.25f, h * 0.52f)
                    lineTo(w * 0.42f, h * 0.70f)
                    lineTo(w * 0.78f, h * 0.32f)
                }
                drawPath(path, color, style = stroke)
            }
            "chevron_down" -> {
                val path = Path().apply {
                    moveTo(w * 0.25f, h * 0.38f)
                    lineTo(w * 0.50f, h * 0.65f)
                    lineTo(w * 0.75f, h * 0.38f)
                }
                drawPath(path, color, style = stroke)
            }
            "chevron_right" -> {
                val path = Path().apply {
                    moveTo(w * 0.35f, h * 0.25f)
                    lineTo(w * 0.65f, h * 0.50f)
                    lineTo(w * 0.35f, h * 0.75f)
                }
                drawPath(path, color, style = stroke)
            }
            "external" -> {
                // Flecha externa ↗
                val path = Path().apply {
                    moveTo(w * 0.22f, h * 0.45f)
                    lineTo(w * 0.22f, h * 0.78f)
                    lineTo(w * 0.78f, h * 0.78f)
                    lineTo(w * 0.78f, h * 0.45f)
                }
                drawPath(path, color, style = stroke)
                drawLine(color, Offset(w * 0.45f, h * 0.55f), Offset(w * 0.78f, h * 0.22f), strokeWidth = strokeWidth * density, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.58f, h * 0.22f), Offset(w * 0.78f, h * 0.22f), strokeWidth = strokeWidth * density, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.78f, h * 0.42f), strokeWidth = strokeWidth * density, cap = StrokeCap.Round)
            }
            else -> {
                // Default box
                drawCircle(color, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
            }
        }
    }
}
