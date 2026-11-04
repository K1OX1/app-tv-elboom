package cl.elboomlicanray.tvlauncher.presentation.modals.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.elboomlicanray.tvlauncher.presentation.home.ActivityDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.domain.model.AccommodationUnit
import cl.elboomlicanray.tvlauncher.domain.model.ConnectionStatusInfo
import cl.elboomlicanray.tvlauncher.domain.model.GuestStayInfo
import cl.elboomlicanray.tvlauncher.domain.model.TvConnectionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DialogBgGradient = listOf(
    Color(0xF5081226),
    Color(0xFA040A17),
    Color(0xFE020409)
)
private val CardBg = Color(0xFF0F1A2E)
private val CardBgFocus = Color(0xFF1E3558)
private val FocusCyan = Color(0xFF00E5FF)
private val BorderIdle = Color(0xFF1E2D4A)
private val TextMuted = Color(0xFF94A3B8)

/**
 * Diálogo que se despliega al seleccionar el Indicador de Conexión (Punto Verde / Rojo)
 * en la barra superior del televisor con el control remoto.
 */
@Composable
fun ConnectionStatusDialog(
    unit: AccommodationUnit,
    connectionStatus: ConnectionStatusInfo,
    stay: GuestStayInfo,
    onReconfigurePin: () -> Unit,
    onRefreshConnection: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            initialFocus.requestFocus()
        } catch (_: Exception) {}
    }

    val isConnected = connectionStatus.state == TvConnectionState.CONNECTED
    val isAuthError = connectionStatus.state == TvConnectionState.AUTH_ERROR
    val isOffline = connectionStatus.state == TvConnectionState.NO_INTERNET

    val statusDotColor = when {
        isConnected -> Color(0xFF10B981) // Verde
        isAuthError -> Color(0xFFEF4444) // Rojo
        isOffline -> Color(0xFFF59E0B)   // Ámbar/Rojo
        else -> Color(0xFF94A3B8)
    }

    val statusTitle = when {
        isConnected -> "🟢 Conexión Activa y Segura"
        isAuthError -> "🔴 Token Inválido o Modificado (401)"
        isOffline -> "🔴 Sin Conexión a Internet"
        else -> "⚪ Configuración Pendiente"
    }

    val lastVerifiedText = remember(connectionStatus.lastVerifiedTimestamp) {
        if (connectionStatus.lastVerifiedTimestamp > 0) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(connectionStatus.lastVerifiedTimestamp))
        } else {
            "Pendiente"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(620.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(colors = DialogBgGradient))
                    .border(1.5.dp, if (isConnected) FocusCyan.copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CABECERA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = statusTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${unit.name} · Alojamientos El Boom Lican Ray",
                                    fontSize = 12.5.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        SetupDialogCloseButton(onClick = onDismiss)
                    }

                    // CAJA DE DETALLE TÉCNICO Y ESTADO
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg)
                            .border(1.dp, BorderIdle, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = connectionStatus.detail.ifBlank { connectionStatus.message },
                                fontSize = 13.5.sp,
                                color = if (isAuthError) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.9f),
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "• Cabaña vinculada: ${unit.name} (ID #${unit.id})\n" +
                                        "• Última verificación: $lastVerifiedText\n" +
                                        "• Red Wi-Fi Starlink: ${stay.wifiNetwork}\n" +
                                        "• Clave Wi-Fi: ${stay.wifiPassword}\n" +
                                        "• Servidor central: https://elboom.cl/api/hotel-tv",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // ADVERTENCIA SI HAY ERROR DE AUTENTICACIÓN
                    if (isAuthError) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.4f))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "⚠️ El token fue modificado en la administración web o el PIN es incorrecto. Haz clic en 'Actualizar PIN de la Cabaña' para ingresar el nuevo código.",
                                color = Color(0xFFFEE2E2),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // BOTONES DE ACCIÓN (D-PAD FRIENDLY)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SetupModalButton(
                            label = "🔄 Reverificar Conexión",
                            isPrimary = isConnected,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(if (isConnected) initialFocus else FocusRequester()),
                            onClick = {
                                onRefreshConnection()
                                onDismiss()
                            }
                        )

                        SetupModalButton(
                            label = "🔑 Actualizar PIN",
                            isPrimary = !isConnected,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(if (!isConnected) initialFocus else FocusRequester()),
                            onClick = {
                                onDismiss()
                                onReconfigurePin()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupModalButton(
    label: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "modal_btn_scale"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isPrimary && isFocused -> Color(0xFF00B4D8)
                    isPrimary -> Color(0xFF0284C7)
                    isFocused -> CardBgFocus
                    else -> CardBg
                }
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> FocusCyan
                    isPrimary -> Color(0xFF38BDF8)
                    else -> BorderIdle
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isPrimary -> Color.White
                isFocused -> FocusCyan
                else -> TextMuted
            }
        )
    }
}

@Composable
private fun SetupDialogCloseButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "close_scale"
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (isFocused) Color(0x3300E5FF) else Color.White.copy(alpha = 0.08f))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) FocusCyan else Color.White.copy(alpha = 0.18f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✕",
            color = if (isFocused) FocusCyan else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
