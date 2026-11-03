package cl.elboomlicanray.tvlauncher.presentation.modals.kiosk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.data.helper.KioskPermissionHelper
import cl.elboomlicanray.tvlauncher.presentation.modals.base.DialogItemRow
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Diálogo Modal: Configuración de Kiosco para Zapping TV (ZappingKioskSetupDialog).
 *
 * Responsabilidad:
 * Guiar la activación del servicio de accesibilidad para re-mapeo seguro del D-Pad en Zapping.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ZappingKioskSetupDialog(
    onLaunchZapping: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEnabled = remember { KioskPermissionHelper.isAccessibilityServiceEnabled(context) }
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        primaryFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(840.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF7090E1A))
                .border(1.5.dp, if (isEnabled) Color(0xFF10B981) else Color(0xFFF59E0B), RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔒 Modo Kiosco Zapping TV",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TvTokens.TextWhite,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isEnabled) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnabled) "✓ ACTIVO" else "PENDIENTE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogItemRow(
                        icon = "🔊",
                        title = "Volumen (+ / -)",
                        desc = "Audio libre y normal para que el huésped suba o baje el volumen a su gusto."
                    )
                    DialogItemRow(
                        icon = "↕️",
                        title = "Flechas Arriba y Abajo",
                        desc = "Re-mapeadas a Channel Up y Channel Down (cambio directo de canal en Zapping)."
                    )
                    DialogItemRow(
                        icon = "🚫",
                        title = "Flechas Izquierda y Derecha",
                        desc = "100% deshabilitadas. Ningún huésped puede abrir menús, cambiar cuentas o salir de la TV."
                    )
                    DialogItemRow(
                        icon = "↩️",
                        title = "Botón Atrás (Back)",
                        desc = "Actúa como Home: al presionarlo, el televisor regresa de inmediato a este Launcher."
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isEnabled) {
                        Button(
                            onClick = {
                                val rootOk = KioskPermissionHelper.tryAutoEnableViaRoot(context)
                                if (rootOk) {
                                    onLaunchZapping()
                                } else {
                                    KioskPermissionHelper.openAccessibilitySettings(context)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.focusRequester(primaryFocusRequester),
                            colors = ButtonDefaults.colors(
                                focusedContainerColor = Color(0xFFFBBF24),
                                focusedContentColor = Color(0xFF0F172A),
                                containerColor = Color(0xFFF59E0B)
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "⚡ Activar en 1 Clic (Ajustes TV)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color(0xFF0F172A),
                                softWrap = false,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = onLaunchZapping,
                            colors = ButtonDefaults.colors(
                                focusedContainerColor = TvTokens.CyanBrand,
                                focusedContentColor = Color.Black,
                                containerColor = Color(0xFF111A2C)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "Abrir sin Kiosco",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    } else {
                        Button(
                            onClick = onLaunchZapping,
                            modifier = Modifier.focusRequester(primaryFocusRequester),
                            colors = ButtonDefaults.colors(
                                focusedContainerColor = Color(0xFF10B981),
                                focusedContentColor = Color.Black,
                                containerColor = Color(0xFF10B981).copy(alpha = 0.85f)
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "🚀 Abrir Zapping Protegido",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            focusedContainerColor = TvTokens.CyanBrand,
                            focusedContentColor = Color.Black,
                            containerColor = Color(0xFF111A2C)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "✕ Cerrar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
