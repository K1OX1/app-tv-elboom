package cl.elboomlicanray.tvlauncher.presentation.modals.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.elboomlicanray.tvlauncher.presentation.home.ActivityDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.domain.model.DayForecast
import cl.elboomlicanray.tvlauncher.domain.model.WeatherInfo
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Diálogo de Pronóstico Meteorológico de 7 Días (Lican Ray · Lago Calafquén).
 *
 * Réplica exacta y de ultra alta fidelidad visual de la referencia oficial (media_1791476401028.png):
 * 1. Tarjeta Glassmorphic con bisel iridiscente multicolor y brillo especular de vidrio 3D.
 * 2. Cabecera limpia con "7-Day Forecast" y "📍 Lican Ray ◆ Lago Calafquén".
 * 3. 7 columnas perfectamente alineadas: Día (LUN), número de fecha, icono 3D, temp máxima/mínima y viento km/h.
 * 4. Botón de cierre estilo Apple TV con foco inicial para el control remoto.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WeatherWeeklyForecastDialog(
    weather: WeatherInfo,
    onDismiss: () -> Unit
) {
    val closeFocusRequester = remember { FocusRequester() }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        try {
            closeFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(140, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)),
            exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.95f)
        ) {
            val cardShape = remember { RoundedCornerShape(32.dp) }

        // Bisel Iridiscente Multicolor (Rainbow Specular Sheen igual a media_1791476401028.png)
        val iridescentBorderBrush = remember {
            Brush.linearGradient(
                colors = listOf(
                    Color(0x80FFFFFF), // Destello blanco en esquina
                    Color(0x90A5F3FC), // Cian glaciar
                    Color(0xB0C084FC), // Violeta prismático
                    Color(0x90F472B6), // Rosa iridiscente
                    Color(0x7038BDF8), // Azul cielo
                    Color(0x50FFFFFF)  // Reflejo suave
                ),
                start = Offset(0f, 0f),
                end = Offset(1800f, 800f)
            )
        }

        Box(
            modifier = Modifier
                .width(860.dp)
                .wrapContentHeight()
                .shadow(32.dp, cardShape, spotColor = Color(0x600284C7))
                .clip(cardShape)
                // Fondo Cristalino Azul Sapphire Glassmorphism
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDE1E4A7D), // Azul zafiro semi-translúcido
                            Color(0xEA16355E), // Azul profundo
                            Color(0xF40F223D)  // Base marina oscura
                        )
                    )
                )
                .border(2.dp, iridescentBorderBrush, cardShape)
                .padding(horizontal = 32.dp, vertical = 26.dp)
        ) {
            // Reflejo diagonal especular superior (Glossy Glass Glare de Apple)
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val glarePath = Path().apply {
                    moveTo(size.width * 0.40f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.65f)
                    lineTo(size.width * 0.20f, size.height)
                    close()
                }
                drawPath(
                    path = glarePath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                        start = Offset(size.width, 0f),
                        end = Offset(size.width * 0.2f, size.height)
                    )
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // =====================================================================
                // 1. CABECERA: TÍTULO, UBICACIÓN Y BOTÓN DE CIERRE APPLE GLASS
                // =====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pronóstico a 7 Días",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📍 Lican Ray ◆ Lago Calafquén",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // Botón Cerrar Minimalista Estilo Vidrio Apple
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(closeFocusRequester),
                        shape = ButtonDefaults.shape(RoundedCornerShape(20.dp)),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.14f),
                            contentColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color(0xFF0F172A)
                        ),
                        border = ButtonDefaults.border(
                            border = Border(
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(20.dp)
                            ),
                            focusedBorder = Border(
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                shape = RoundedCornerShape(20.dp)
                            )
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✕",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cerrar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // =====================================================================
                // 2. FILA DE 7 COLUMNAS DEL PRONÓSTICO SEMANAL
                // =====================================================================
                val forecastList = remember(weather.weeklyForecast) {
                    if (weather.weeklyForecast.size >= 7) {
                        weather.weeklyForecast.take(7)
                    } else {
                        weather.weeklyForecast
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    forecastList.forEach { day ->
                        ForecastDayColumn(
                            day = day,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Columna individual de pronóstico diario (idéntica a las 7 columnas de media_1791476401028.png).
 */
@Composable
private fun ForecastDayColumn(
    day: DayForecast,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Día de la semana (ej. MON / LUN)
        Text(
            text = day.dayOfWeek,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.95f),
            letterSpacing = 0.5.sp
        )

        // 2. Número del día (ej. 12, 13, 14...)
        Text(
            text = day.dayNumber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.68f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Icono 3D de Clima Ilustrado
        Weather3DIcon(
            weatherCode = day.weatherCode,
            size = 50.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Temperaturas con resplandor suave de fondo
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 54.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x38818CF8), // Destello suave lavanda detrás de la temperatura
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Temp Máxima
                Text(
                    text = "${day.maxTempC}°",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                // Temp Mínima
                Text(
                    text = "${day.minTempC}°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Viento (Icono de brisa y velocidad)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WindBreezeMiniIcon(modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "VIENTO",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 8.sp
                )
                Text(
                    text = "${day.windKmH} km/h",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 11.sp
                )
            }
        }
    }
}

/**
 * Pequeño icono vectorial de brisa de viento estilizado en Canvas.
 */
@Composable
private fun WindBreezeMiniIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.4f * density, cap = StrokeCap.Round)
        val windColor = Color.White.copy(alpha = 0.65f)

        // Línea de viento superior con bucle
        val topPath = Path().apply {
            moveTo(w * 0.10f, h * 0.40f)
            lineTo(w * 0.70f, h * 0.40f)
            cubicTo(w * 0.85f, h * 0.40f, w * 0.90f, h * 0.15f, w * 0.75f, h * 0.15f)
            cubicTo(w * 0.65f, h * 0.15f, w * 0.65f, h * 0.35f, w * 0.75f, h * 0.35f)
        }
        drawPath(topPath, color = windColor, style = stroke)

        // Línea de viento inferior
        val botPath = Path().apply {
            moveTo(w * 0.25f, h * 0.70f)
            lineTo(w * 0.85f, h * 0.70f)
        }
        drawPath(botPath, color = windColor, style = stroke)
    }
}
