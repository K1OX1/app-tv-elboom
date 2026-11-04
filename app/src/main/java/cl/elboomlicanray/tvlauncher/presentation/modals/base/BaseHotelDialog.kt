package cl.elboomlicanray.tvlauncher.presentation.modals.base

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.DialogProperties
import cl.elboomlicanray.tvlauncher.presentation.home.ActivityDialog as Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.presentation.theme.TvDimens
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Contenedor Base para Diálogos Modales de Android TV.
 *
 * Responsabilidad:
 * Proporcionar ventana modal centrada, cabecera con icono, título, subtítulo
 * y botón de cierre con foco inicial D-Pad para TV.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BaseHotelDialog(
    title: String,
    subtitle: String? = null,
    icon: String? = null,
    dialogWidth: Dp = TvDimens.ModalWideWidth,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        val closeFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            try {
                closeFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }

        Box(
            modifier = Modifier
                .width(dialogWidth)
                .wrapContentHeight()
                .clip(TvTokens.RadiusModal)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF20B132B),
                            Color(0xF5060A14)
                        )
                    )
                )
                .border(1.5.dp, TvTokens.CyanNeon.copy(alpha = 0.35f), TvTokens.RadiusModal)
                .padding(horizontal = 30.dp, vertical = 22.dp)
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Text(text = icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = TvTokens.TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    fontSize = 11.5.sp,
                                    color = TvTokens.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .focusRequester(closeFocusRequester)
                            .border(1.2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.14f),
                            contentColor = Color.White,
                            focusedContainerColor = TvTokens.CyanBrand,
                            focusedContentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "✕ Cerrar (Atrás)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                content()
            }
        }
    }
}
