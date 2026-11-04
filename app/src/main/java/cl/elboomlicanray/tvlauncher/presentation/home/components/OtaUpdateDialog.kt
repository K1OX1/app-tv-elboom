package cl.elboomlicanray.tvlauncher.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.elboomlicanray.tvlauncher.presentation.home.ActivityDialog as Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import cl.elboomlicanray.tvlauncher.data.update.UpdateInfo
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OtaUpdateDialog(
    updateInfo: UpdateInfo,
    downloadProgress: Float?,
    onConfirmUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE60A1931))
                .border(1.dp, TvTokens.GlassBorderStroke, RoundedCornerShape(20.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🚀 Nueva Actualización Disponible",
                    color = TvTokens.CyanNeon,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Versión ${updateInfo.versionName}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = updateInfo.changelog,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (downloadProgress != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = TvTokens.CyanNeon,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (downloadProgress >= 1.0f) "Instalando actualización..." else "Descargando... ${(downloadProgress * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.25f)
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                        ) {
                            Text(text = "Cancelar", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.25f)
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(10.dp))
                        ) {
                            Text(text = "Más tarde", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = onConfirmUpdate,
                            colors = ButtonDefaults.colors(
                                containerColor = TvTokens.CyanNeon.copy(alpha = 0.2f),
                                focusedContainerColor = TvTokens.CyanNeon
                            ),
                            border = ButtonDefaults.border(
                                focusedBorder = Border(
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(10.dp))
                        ) {
                            Text(text = "Actualizar Ahora", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
