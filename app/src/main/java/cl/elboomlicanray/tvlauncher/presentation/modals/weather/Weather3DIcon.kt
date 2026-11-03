package cl.elboomlicanray.tvlauncher.presentation.modals.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ilustraciones 3D de Clima renderizadas nativamente en Canvas de Compose.
 *
 * Replica con fidelidad milimétrica el diseño de la referencia oficial (media_1791476401028.png):
 * - Sol con resplandor dorado y rayos radiales
 * - Sol detrás de nubes volumétricas blancas con sombreado suave
 * - Nube de tormenta con relámpago púrpura/dorado y lluvia
 * - Nube lluviosa con gotas diagonales
 * - Nube esponjosa de cielo nublado
 */
@Composable
fun Weather3DIcon(
    weatherCode: Int,
    isDay: Boolean = true,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        when (weatherCode) {
            0 -> {
                // Sol puro radiante con rayos (Despejado)
                drawVibrantSun(w, h, hasRays = true)
            }
            1, 2 -> {
                // Sol detrás de nube (Parcialmente nublado)
                drawSunBehindCloud(w, h)
            }
            3 -> {
                // Nube esponjosa (Nublado)
                drawFluffyCloud(w, h, cloudColorTop = Color(0xFFF8FAFC), cloudColorBottom = Color(0xFF94A3B8))
            }
            45, 48 -> {
                // Niebla (Líneas suaves difuminadas)
                drawFog(w, h)
            }
            51, 53, 55 -> {
                // Llovizna (Nube suave con gotas finas)
                drawRainCloud(w, h, rainIntensity = 2, isHeavy = false)
            }
            61, 63, 65, 80, 81, 82 -> {
                // Lluvia / Chubascos
                val isHeavy = weatherCode == 65 || weatherCode == 82
                drawRainCloud(w, h, rainIntensity = if (isHeavy) 5 else 4, isHeavy = isHeavy)
            }
            71, 73, 75 -> {
                // Nieve
                drawSnowCloud(w, h)
            }
            95, 96, 99 -> {
                // Tormenta eléctrica con rayo violeta/dorado
                drawStormCloud(w, h)
            }
            else -> {
                drawSunBehindCloud(w, h)
            }
        }
    }
}

/**
 * Dibuja un sol 3D vibrante con esfera de degradado cálido y rayos radiales.
 */
private fun DrawScope.drawVibrantSun(w: Float, h: Float, hasRays: Boolean) {
    val cx = w * 0.5f
    val cy = h * 0.5f
    val sunRadius = w * 0.26f

    if (hasRays) {
        val rayLength = w * 0.12f
        val rayInner = sunRadius + w * 0.04f
        val rayOuter = rayInner + rayLength
        val rayStroke = w * 0.055f

        for (i in 0 until 10) {
            val angle = (i * 36.0) * (Math.PI / 180.0)
            val startX = cx + (rayInner * cos(angle)).toFloat()
            val startY = cy + (rayInner * sin(angle)).toFloat()
            val endX = cx + (rayOuter * cos(angle)).toFloat()
            val endY = cy + (rayOuter * sin(angle)).toFloat()

            drawLine(
                color = Color(0xFFF59E0B),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = rayStroke,
                cap = StrokeCap.Round
            )
        }
    }

    // Esfera del sol con iluminación 3D esférica
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFEF08A), // Centro blanco-amarillo brillante
                Color(0xFFFACC15), // Amarillo intenso
                Color(0xFFF59E0B), // Naranja cálido
                Color(0xFFD97706)  // Borde sombreado suave
            ),
            center = Offset(cx - sunRadius * 0.25f, cy - sunRadius * 0.25f),
            radius = sunRadius * 1.3f
        ),
        radius = sunRadius,
        center = Offset(cx, cy)
    )
}

/**
 * Dibuja el sol dorado asomándose detrás de una nube blanca 3D (media_1791476401028.png día 1 y día 3).
 */
private fun DrawScope.drawSunBehindCloud(w: Float, h: Float) {
    // Sol en la esquina superior derecha
    val sunCx = w * 0.65f
    val sunCy = h * 0.38f
    val sunRadius = w * 0.22f

    // Rayos sutiles en la parte visible del sol
    val rayInner = sunRadius + w * 0.03f
    val rayOuter = rayInner + w * 0.09f
    val rayStroke = w * 0.045f

    for (angleDeg in listOf(-70.0, -35.0, 0.0, 35.0, 70.0, 105.0)) {
        val angle = angleDeg * (Math.PI / 180.0)
        val startX = sunCx + (rayInner * cos(angle)).toFloat()
        val startY = sunCy + (rayInner * sin(angle)).toFloat()
        val endX = sunCx + (rayOuter * cos(angle)).toFloat()
        val endY = sunCy + (rayOuter * sin(angle)).toFloat()

        drawLine(
            color = Color(0xFFF59E0B),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = rayStroke,
            cap = StrokeCap.Round
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFEF08A),
                Color(0xFFFBBF24),
                Color(0xFFEA580C)
            ),
            center = Offset(sunCx - sunRadius * 0.3f, sunCy - sunRadius * 0.3f),
            radius = sunRadius * 1.2f
        ),
        radius = sunRadius,
        center = Offset(sunCx, sunCy)
    )

    // Nube esponjosa volumétrica en primer plano
    drawFluffyCloud(
        w = w,
        h = h,
        offsetX = -w * 0.08f,
        offsetY = h * 0.12f,
        scale = 0.92f,
        cloudColorTop = Color(0xFFFFFFFF),
        cloudColorBottom = Color(0xFFCBD5E1)
    )
}

/**
 * Dibuja una nube volumétrica 3D estilo Apple Weather.
 */
private fun DrawScope.drawFluffyCloud(
    w: Float,
    h: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    scale: Float = 1.0f,
    cloudColorTop: Color = Color(0xFFFFFFFF),
    cloudColorBottom: Color = Color(0xFF94A3B8)
) {
    val cx = (w * 0.5f + offsetX)
    val cy = (h * 0.54f + offsetY)

    val rLeft = w * 0.18f * scale
    val rCenter = w * 0.26f * scale
    val rRight = w * 0.20f * scale

    val leftCenter = Offset(cx - w * 0.22f * scale, cy + h * 0.06f * scale)
    val topCenter = Offset(cx - w * 0.04f * scale, cy - h * 0.04f * scale)
    val rightCenter = Offset(cx + w * 0.20f * scale, cy + h * 0.08f * scale)

    // Base horizontal de la nube
    val baseWidth = w * 0.62f * scale
    val baseHeight = h * 0.28f * scale
    val baseTopLeft = Offset(cx - baseWidth * 0.5f, cy - h * 0.02f * scale)

    val cloudBrush = Brush.verticalGradient(
        colors = listOf(cloudColorTop, cloudColorBottom),
        startY = cy - rCenter,
        endY = cy + baseHeight
    )

    // Esferas compuestas que forman la nube
    drawCircle(cloudBrush, radius = rLeft, center = leftCenter)
    drawCircle(cloudBrush, radius = rCenter, center = topCenter)
    drawCircle(cloudBrush, radius = rRight, center = rightCenter)

    drawRoundRect(
        brush = cloudBrush,
        topLeft = baseTopLeft,
        size = Size(baseWidth, baseHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(baseHeight * 0.5f, baseHeight * 0.5f)
    )
}

/**
 * Dibuja una nube de lluvia con gotas cayendo en ángulo diagonal idénticas a la referencia.
 */
private fun DrawScope.drawRainCloud(w: Float, h: Float, rainIntensity: Int, isHeavy: Boolean) {
    // Nube azulada / gris suave
    val topColor = if (isHeavy) Color(0xFFCBD5E1) else Color(0xFFF1F5F9)
    val botColor = if (isHeavy) Color(0xFF475569) else Color(0xFF64748B)

    drawFluffyCloud(
        w = w,
        h = h,
        offsetX = 0f,
        offsetY = -h * 0.08f,
        scale = 0.95f,
        cloudColorTop = topColor,
        cloudColorBottom = botColor
    )

    // Gotas de lluvia diagonales (de arriba-derecha a abajo-izquierda)
    val dropLength = h * 0.16f
    val dropStroke = w * 0.048f
    val dropColor = Color(0xFF38BDF8) // Azul celeste brillante

    val startXBase = w * 0.28f
    val stepX = w * 0.12f
    val startY = h * 0.68f

    for (i in 0 until rainIntensity) {
        val sx = startXBase + (i * stepX)
        val sy = startY + ((i % 2) * h * 0.04f)
        val ex = sx - w * 0.06f
        val ey = sy + dropLength

        drawLine(
            color = dropColor,
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = dropStroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Dibuja nube de tormenta oscura con relámpago púrpura/amarillo (media_1791476401028.png día 2 TUE).
 */
private fun DrawScope.drawStormCloud(w: Float, h: Float) {
    // Nube oscura tormentosa
    drawFluffyCloud(
        w = w,
        h = h,
        offsetX = 0f,
        offsetY = -h * 0.08f,
        scale = 0.95f,
        cloudColorTop = Color(0xFF64748B),
        cloudColorBottom = Color(0xFF1E293B)
    )

    // Rayo zigzag eléctrico violeta / amarillo en el centro
    val bolt = Path().apply {
        val startX = w * 0.50f
        val startY = h * 0.56f
        moveTo(startX, startY)
        lineTo(startX - w * 0.07f, startY + h * 0.16f)
        lineTo(startX + w * 0.02f, startY + h * 0.16f)
        lineTo(startX - w * 0.06f, startY + h * 0.34f)
        lineTo(startX + w * 0.05f, startY + h * 0.18f)
        lineTo(startX - w * 0.02f, startY + h * 0.18f)
        close()
    }

    drawPath(
        path = bolt,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE879F9), // Rosa / violeta brillante
                Color(0xFFFDE047)  // Amarillo eléctrico
            )
        )
    )

    // Gotas de lluvia a los lados del rayo
    val dropLength = h * 0.14f
    val dropStroke = w * 0.042f
    val dropColor = Color(0xFF60A5FA)

    listOf(w * 0.26f, w * 0.38f, w * 0.68f, w * 0.80f).forEach { sx ->
        val sy = h * 0.70f
        drawLine(
            color = dropColor,
            start = Offset(sx, sy),
            end = Offset(sx - w * 0.05f, sy + dropLength),
            strokeWidth = dropStroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Dibuja copos de nieve suaves cayendo de una nube fresca.
 */
private fun DrawScope.drawSnowCloud(w: Float, h: Float) {
    drawFluffyCloud(
        w = w,
        h = h,
        offsetX = 0f,
        offsetY = -h * 0.08f,
        scale = 0.95f,
        cloudColorTop = Color(0xFFF8FAFC),
        cloudColorBottom = Color(0xFF94A3B8)
    )

    val flakeColor = Color(0xFFE2E8F0)
    val r = w * 0.035f

    listOf(
        Offset(w * 0.32f, h * 0.72f),
        Offset(w * 0.50f, h * 0.78f),
        Offset(w * 0.68f, h * 0.72f),
        Offset(w * 0.40f, h * 0.86f),
        Offset(w * 0.60f, h * 0.86f)
    ).forEach { pos ->
        drawCircle(color = flakeColor, radius = r, center = pos)
    }
}

/**
 * Dibuja líneas de niebla horizontal suave.
 */
private fun DrawScope.drawFog(w: Float, h: Float) {
    val barStroke = h * 0.08f
    val barColor = Color(0xFFCBD5E1).copy(alpha = 0.8f)

    listOf(
        Pair(w * 0.20f, w * 0.80f) to h * 0.35f,
        Pair(w * 0.15f, w * 0.85f) to h * 0.50f,
        Pair(w * 0.25f, w * 0.75f) to h * 0.65f
    ).forEach { (range, y) ->
        drawLine(
            color = barColor,
            start = Offset(range.first, y),
            end = Offset(range.second, y),
            strokeWidth = barStroke,
            cap = StrokeCap.Round
        )
    }
}
