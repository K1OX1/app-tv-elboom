package cl.elboomlicanray.tvlauncher.presentation.modals.stay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.elboomlicanray.tvlauncher.presentation.home.ActivityDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.domain.model.GuestStayInfo
import cl.elboomlicanray.tvlauncher.presentation.components.TvIcon
import cl.elboomlicanray.tvlauncher.presentation.components.qr.QrCodeView
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens
import java.text.NumberFormat
import java.util.Locale

/**
 * Diálogo Modal: Pagar Saldo en el Sitio Web (elboom.cl).
 *
 * Muestra el enlace oficial y el código QR de alta resolución para que el huésped
 * escanee con su teléfono móvil y pague o revise su reserva directamente en la web.
 * No abre navegadores externos ni pestañas en el televisor.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PaymentDialog(
    stay: GuestStayInfo,
    unitName: String,
    paymentUrl: String,
    onDismiss: () -> Unit
) {
    val closeFocusRequester = remember { FocusRequester() }
    var isVisible by remember { mutableStateOf(false) }

    val clpFormatter = remember { NumberFormat.getIntegerInstance(Locale("es", "CL")) }
    val formattedBalance = "$${clpFormatter.format(stay.balance)} CLP"

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
                    scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f)),
            exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.96f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD030816)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(620.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvTokens.BgNavyPanel)
                        .border(1.5.dp, TvTokens.CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Título y Cabina
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(20.dp)
                                            .background(TvTokens.CyanNeon, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PAGAR SALDO DE RESERVA",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Text(
                                    text = "$unitName · Alojamientos El Boom Lican Ray",
                                    color = TvTokens.TextMuted,
                                    fontSize = 12.sp
                                )
                            }

                            // Botón cerrar superior
                            Button(
                                onClick = onDismiss,
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                colors = ButtonDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.10f),
                                    contentColor = Color.White,
                                    focusedContainerColor = TvTokens.CyanNeon,
                                    focusedContentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(text = "✕ Cerrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Tarjeta de Saldo y QR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF071120))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Información del saldo
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Monto pendiente:",
                                    color = TvTokens.TextSlate,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formattedBalance,
                                    color = TvTokens.AmberPending,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                if (stay.guestFullName.isNotBlank()) {
                                    Text(
                                        text = "Titular: ${stay.guestFullName}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Escanea el código QR con tu celular para pagar en línea de forma segura con WebPay, débito o crédito:",
                                    color = TvTokens.TextMuted,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = paymentUrl,
                                    color = TvTokens.CyanNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Código QR
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                QrCodeView(
                                    content = paymentUrl,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Botón de acción: Volver a la pantalla principal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(closeFocusRequester),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
                                colors = ButtonDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    contentColor = Color.White,
                                    focusedContainerColor = TvTokens.CyanNeon,
                                    focusedContentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(vertical = 11.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "← Volver al inicio",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
