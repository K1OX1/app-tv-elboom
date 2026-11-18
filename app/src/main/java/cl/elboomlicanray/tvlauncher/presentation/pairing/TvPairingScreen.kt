package cl.elboomlicanray.tvlauncher.presentation.pairing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.elboomlicanray.tvlauncher.domain.model.TvPairingUiState
import cl.elboomlicanray.tvlauncher.presentation.components.qr.QrCodeView
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens
import kotlinx.coroutines.delay

@Composable
fun TvPairingScreen(
    viewModel: TvPairingViewModel,
    onPairingSuccess: (unitId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state) {
        if (state is TvPairingUiState.Approved) {
            val approved = state as TvPairingUiState.Approved
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            delay(1500L) // Breve confirmación visual antes de entrar
            onPairingSuccess(approved.unitId)
        } else if (state is TvPairingUiState.WaitingForApproval || state is TvPairingUiState.Expired || state is TvPairingUiState.NetworkError) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F1E36),
                        TvTokens.CanvasBackground,
                        Color(0xFF02040A)
                    ),
                    radius = 1600f
                )
            )
            .padding(horizontal = 48.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val uiState = state) {
            is TvPairingUiState.Loading -> {
                LoadingView(uiState.message)
            }

            is TvPairingUiState.WaitingForApproval -> {
                PairingActiveView(
                    state = uiState,
                    focusRequester = focusRequester,
                    onRegenerate = { viewModel.generateNewCode() }
                )
            }

            is TvPairingUiState.Approved -> {
                ApprovedView(
                    unitName = uiState.unitName,
                    focusRequester = focusRequester,
                    onContinue = { onPairingSuccess(uiState.unitId) },
                    onRePair = { viewModel.resetAndStartPairing() }
                )
            }

            is TvPairingUiState.Expired -> {
                ExpiredView(
                    focusRequester = focusRequester,
                    onRegenerate = { viewModel.generateNewCode() }
                )
            }

            is TvPairingUiState.NetworkError -> {
                NetworkErrorView(
                    message = uiState.message,
                    focusRequester = focusRequester,
                    onRetry = { viewModel.retry() }
                )
            }

            is TvPairingUiState.ServerError -> {
                ServerErrorView(
                    message = uiState.message,
                    focusRequester = focusRequester,
                    onRetry = { viewModel.retry() }
                )
            }

            is TvPairingUiState.Revoked -> {
                RevokedView(
                    focusRequester = focusRequester,
                    onPair = { viewModel.startPairing() }
                )
            }
        }
    }
}

@Composable
private fun PairingActiveView(
    state: TvPairingUiState.WaitingForApproval,
    focusRequester: FocusRequester,
    onRegenerate: () -> Unit
) {
    val minutes = state.secondsRemaining / 60
    val seconds = state.secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna Izquierda: QR Grande y Nítido (320x320dp)
        Box(
            modifier = Modifier
                .size(330.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(3.dp, TvTokens.CyanBrand.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            QrCodeView(
                content = state.approvalUrl,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Columna Derecha: Información y Código Monospace de 6 Dígitos
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // Marca
            Text(
                text = "ALOJAMIENTOS EL BOOM · LICÁN RAY",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TvTokens.CyanBrand,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Título Principal
            Text(
                text = "Vincular este televisor",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = TvTokens.TextWhite,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Instrucción
            Text(
                text = "Escanea el código QR con la cámara de tu teléfono para asignar este televisor a una cabaña desde el panel de administración.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = TvTokens.TextSlate
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Contenedor del Código de 6 Dígitos
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvTokens.CardBackgroundFocus)
                    .border(1.5.dp, TvTokens.CyanBrand.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "CÓDIGO DE EMPAREJAMIENTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TvTokens.TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = state.code,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = TvTokens.TextWhite,
                            letterSpacing = 8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Estado de espera + Temporizador
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(TvTokens.EmeraldSuccess.copy(alpha = alphaAnim))
                )
                Text(
                    text = "Esperando aprobación...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TvTokens.EmeraldSuccessText
                )
                Text(
                    text = "·",
                    fontSize = 14.sp,
                    color = TvTokens.TextSubtle
                )
                Text(
                    text = "Expira en $timeFormatted",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TvTokens.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Focusable con D-Pad
            TvButton(
                text = "Generar nuevo código",
                icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                onClick = onRegenerate,
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    }
}

@Composable
private fun ApprovedView(
    unitName: String,
    focusRequester: FocusRequester,
    onContinue: () -> Unit,
    onRePair: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TvTokens.EmeraldSuccess.copy(alpha = 0.2f))
                .border(2.dp, TvTokens.EmeraldSuccess, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = TvTokens.EmeraldSuccess,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "¡Televisor Vinculado con Éxito!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Asignado a: $unitName",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TvTokens.CyanBrand
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Iniciando pantalla de bienvenida...",
            fontSize = 14.sp,
            color = TvTokens.TextMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvButton(
                text = "Ingresar ahora",
                icon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                onClick = onContinue,
                modifier = Modifier.focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.width(16.dp))
            TvButton(
                text = "Generar nuevo código",
                icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                onClick = onRePair
            )
        }
    }
}

@Composable
private fun ExpiredView(
    focusRequester: FocusRequester,
    onRegenerate: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TvTokens.AmberPending,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "El código de emparejamiento ha expirado",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Por seguridad, cada código de vinculación tiene una vigencia máxima de 10 minutos.",
            fontSize = 15.sp,
            color = TvTokens.TextSlate,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        TvButton(
            text = "Generar nuevo código",
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
            onClick = onRegenerate,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Composable
private fun NetworkErrorView(
    message: String,
    focusRequester: FocusRequester,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TvTokens.RedAlert,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Sin conexión a internet",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No se pudo conectar con el servidor de Alojamientos El Boom.\nVerifica la conexión Wi-Fi de este televisor.",
            fontSize = 15.sp,
            color = TvTokens.TextSlate,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        TvButton(
            text = "Reintentar conexión",
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
            onClick = onRetry,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Composable
private fun ServerErrorView(
    message: String,
    focusRequester: FocusRequester,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TvTokens.RedAlert,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Error del servidor",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 15.sp,
            color = TvTokens.TextSlate,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        TvButton(
            text = "Reintentar",
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
            onClick = onRetry,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Composable
private fun RevokedView(
    focusRequester: FocusRequester,
    onPair: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TvTokens.AmberPending,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Acceso revocado por el administrador",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Este televisor fue desvinculado desde el panel de control.\nPara volver a utilizarlo, debe emparejarse nuevamente.",
            fontSize = 15.sp,
            color = TvTokens.TextSlate,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        TvButton(
            text = "Emparejar televisor",
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
            onClick = onPair,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Composable
private fun LoadingView(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            color = TvTokens.CyanBrand,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = message,
            fontSize = 16.sp,
            color = TvTokens.TextSlate
        )
    }
}

@Composable
private fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFocused) TvTokens.CyanBrand else TvTokens.CardBackgroundFocus
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color.White else TvTokens.GlassBorderStroke,
                shape = RoundedCornerShape(14.dp)
            )
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon?.invoke()
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) Color.Black else Color.White
            )
        }
    }
}
