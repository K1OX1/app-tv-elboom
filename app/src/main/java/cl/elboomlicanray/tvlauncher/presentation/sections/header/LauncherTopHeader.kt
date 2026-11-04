package cl.elboomlicanray.tvlauncher.presentation.sections.header

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.R
import cl.elboomlicanray.tvlauncher.domain.model.TvConnectionState
import cl.elboomlicanray.tvlauncher.presentation.components.TvIcon
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Encabezado Compacto Superior (LauncherTopHeader).
 *
 * Características:
 * 1. Izquierda: Logotipo oficial El Boom + "Lican Ray" con atajo de dueño (Owner Override):
 *    - Mantener presionado OK por 3 segundos o 5 clics rápidos abre la configuración de PIN/Cabaña.
 * 2. Derecha:
 *    - Indicador Visual de Conexión (Punto Verde / Rojo): Focusable con D-Pad, muestra estado y abre diagnóstico.
 *    - Hora y Fecha en tiempo real.
 *    - Cápsula interactiva de clima en vivo de Licán Ray.
 *    - Selector compacto de idioma [ 🌐 ES ⌵ ].
 */
@Composable
fun LauncherTopHeader(
    currentTime: String,
    currentDate: String,
    temperature: String,
    selectedLanguage: String = "ES",
    weatherConditionIcon: String = "☀️",
    weatherDescription: String = "Lican Ray",
    connectionState: TvConnectionState = TvConnectionState.CONNECTED,
    unitName: String = "Cabaña N°1",
    modifier: Modifier = Modifier,
    onWeatherClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onConnectionIndicatorClick: () -> Unit = {},
    onOwnerOverrideRequested: () -> Unit = {},
    onAdminRequested: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // =========================================================================
        // 1. LOGOTIPO OFICIAL EL BOOM (IZQUIERDA) CON ATAJO DE DUEÑO
        // =========================================================================
        val logoInteractionSource = remember { MutableInteractionSource() }
        val isLogoFocused by logoInteractionSource.collectIsFocusedAsState()

        val logoScale by animateFloatAsState(
            targetValue = if (isLogoFocused) 1.04f else 1.0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
            label = "logo_scale"
        )

        var longPressJob by remember { mutableStateOf<Job?>(null) }
        var consecutiveClickCount by remember { mutableIntStateOf(0) }
        var lastClickTime by remember { mutableLongStateOf(0L) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                }
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isLogoFocused) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isLogoFocused) 1.5.dp else 0.dp,
                    color = if (isLogoFocused) TvTokens.CyanNeon else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .focusable(interactionSource = logoInteractionSource)
                .onKeyEvent { keyEvent ->
                    val code = keyEvent.nativeKeyEvent.keyCode
                    if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                        code == android.view.KeyEvent.KEYCODE_ENTER ||
                        code == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
                    ) {
                        if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            if (longPressJob == null) {
                                longPressJob = coroutineScope.launch {
                                    delay(3000L) // 3 segundos continuos para override
                                    onOwnerOverrideRequested()
                                }
                            }
                            return@onKeyEvent true
                        } else if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                            longPressJob?.cancel()
                            longPressJob = null

                            val now = System.currentTimeMillis()
                            if (now - lastClickTime < 2500L) {
                                consecutiveClickCount++
                            } else {
                                consecutiveClickCount = 1
                            }
                            lastClickTime = now

                            if (consecutiveClickCount >= 5) {
                                consecutiveClickCount = 0
                                onOwnerOverrideRequested()
                            } else {
                                onAdminRequested()
                            }
                            return@onKeyEvent true
                        }
                    }
                    false
                }
                .clickable(
                    interactionSource = logoInteractionSource,
                    indication = null,
                    onClick = onAdminRequested
                )
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(TvTokens.CyanNeon, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_logo_horizontal),
                contentDescription = "Alojamientos El Boom",
                modifier = Modifier.height(26.dp),
                contentScale = ContentScale.FillHeight
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Lican Ray",
                color = TvTokens.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // =========================================================================
        // 2. DERECHA: INDICADOR CONEXIÓN, HORA, FECHA, CLIMA Y SELECTOR DE IDIOMA
        // =========================================================================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =====================================================================
            // INDICADOR VISUAL DE CONEXIÓN (PUNTO VERDE 🟢 / ROJO 🔴)
            // =====================================================================
            val isConnected = connectionState == TvConnectionState.CONNECTED
            val isAuthError = connectionState == TvConnectionState.AUTH_ERROR
            val isOffline = connectionState == TvConnectionState.NO_INTERNET

            val statusDotColor by androidx.compose.animation.animateColorAsState(targetValue = when {
                isConnected -> Color(0xFF10B981) // Verde esmeralda vivo
                isAuthError -> Color(0xFFEF4444) // Rojo advertencia
                isOffline -> Color(0xFFEF4444)   // Rojo sin internet
                else -> Color(0xFF94A3B8)
            }, animationSpec = tween(150), label = "connection")

            val statusText = if (isConnected) "Conexión a elboom.cl" else "Conexión al servidor"

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isConnected) Color.White.copy(alpha = 0.07f) else Color(0xFF7F1D1D).copy(alpha = 0.35f),
                        RoundedCornerShape(50)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isConnected) Color.White.copy(alpha = 0.16f) else Color(0xFFEF4444).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                // Punto de estado discreto
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .graphicsLayer {
                            alpha = 1f
                        }
                        .background(statusDotColor)
                )

                Text(
                    text = statusText,
                    color = if (isConnected) Color.White.copy(alpha = 0.85f) else Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Separador fino
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0x33FFFFFF))
            )

            // HORA Y FECHA
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentDate,
                    color = TvTokens.TextMuted,
                    fontSize = 10.sp
                )
            }

            // Separador fino
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0x33FFFFFF))
            )

            // CÁPSULA DE CLIMA INTERACTIVA
            val weatherInteractionSource = remember { MutableInteractionSource() }
            val isWeatherFocused by weatherInteractionSource.collectIsFocusedAsState()

            val weatherScale by animateFloatAsState(
                targetValue = if (isWeatherFocused) 1.05f else 1.0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                label = "weather_scale"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = weatherScale
                        scaleY = weatherScale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isWeatherFocused) Color(0x3300E5FF) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = if (isWeatherFocused) 1.5.dp else 0.dp,
                        color = if (isWeatherFocused) TvTokens.CyanNeon else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .focusable(interactionSource = weatherInteractionSource)
                    .onKeyEvent { keyEvent ->
                        val code = keyEvent.nativeKeyEvent.keyCode
                        if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER || code == android.view.KeyEvent.KEYCODE_ENTER || code == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) {
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                onWeatherClick()
                            }
                            true
                        } else false
                    }
                    .clickable(
                        interactionSource = weatherInteractionSource,
                        indication = null,
                        onClick = onWeatherClick
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = weatherConditionIcon,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = temperature,
                        color = if (isWeatherFocused) TvTokens.CyanNeon else Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = weatherDescription,
                        color = if (isWeatherFocused) Color.White else TvTokens.TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // SELECTOR DE IDIOMA COMPACTO [ 🌐 ES ⌵ ]
            val langInteractionSource = remember { MutableInteractionSource() }
            val isLangFocused by langInteractionSource.collectIsFocusedAsState()

            val langScale by animateFloatAsState(
                targetValue = if (isLangFocused) 1.05f else 1.0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                label = "lang_scale"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = langScale
                        scaleY = langScale
                    }
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isLangFocused) Color(0x3300E5FF) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(50)
                    )
                    .border(
                        width = if (isLangFocused) 1.5.dp else 1.dp,
                        color = if (isLangFocused) TvTokens.CyanNeon else Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50)
                    )
                    .focusable(interactionSource = langInteractionSource)
                    .onKeyEvent { keyEvent ->
                        val code = keyEvent.nativeKeyEvent.keyCode
                        if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER || code == android.view.KeyEvent.KEYCODE_ENTER || code == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) {
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                onLanguageClick()
                            }
                            true
                        } else false
                    }
                    .clickable(
                        interactionSource = langInteractionSource,
                        indication = null,
                        onClick = onLanguageClick
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                TvIcon(
                    name = "globe",
                    size = 14.dp,
                    color = if (isLangFocused) TvTokens.CyanNeon else Color.White
                )
                Text(
                    text = selectedLanguage,
                    color = if (isLangFocused) TvTokens.CyanNeon else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                TvIcon(
                    name = "chevron_down",
                    size = 10.dp,
                    color = if (isLangFocused) TvTokens.CyanNeon else TvTokens.TextMuted
                )
            }
        }
    }
}
