package cl.elboomlicanray.tvlauncher.presentation.sections.header

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import cl.elboomlicanray.tvlauncher.R
import cl.elboomlicanray.tvlauncher.presentation.components.TvIcon
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Encabezado Compacto Superior (LauncherTopHeader).
 *
 * Basado exactamente en la imagen de referencia:
 * - Izquierda: Acento cian + Logotipo oficial El Boom + "Lican Ray"
 * - Derecha: Hora, fecha, clima interactivo y selector de idioma compacto [ 🌐 ES ⌵ ]
 */
@Composable
fun LauncherTopHeader(
    currentTime: String,
    currentDate: String,
    temperature: String,
    selectedLanguage: String = "ES",
    weatherConditionIcon: String = "☀️",
    weatherDescription: String = "Lican Ray",
    modifier: Modifier = Modifier,
    onWeatherClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onAdminRequested: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logotipo Oficial y Ubicación (Izquierda)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onAdminRequested() }
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

        // Derecha: Hora, Fecha, Clima y Selector de Idioma
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hora y Fecha
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
                    .height(24.dp)
                    .background(Color(0x33FFFFFF))
            )

            // Cápsula de Clima
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

            // Selector de Idioma compacto [ 🌐 ES ⌵ ]
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
