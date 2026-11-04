package cl.elboomlicanray.tvlauncher.presentation.modals.setup

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.R
import cl.elboomlicanray.tvlauncher.domain.model.AccommodationUnit
import cl.elboomlicanray.tvlauncher.domain.model.AuthTokenException
import cl.elboomlicanray.tvlauncher.domain.model.NetworkOfflineException
import cl.elboomlicanray.tvlauncher.domain.model.OFFICIAL_ACCOMMODATION_UNITS
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens
import kotlinx.coroutines.launch

private val SetupBgGradient = listOf(
    Color(0xF5060D1A), // 96% opacidad
    Color(0xFA030812), // 98% opacidad
    Color(0xFE010408)  // 99% opacidad
)
private val CardBg = Color(0xFF0F1A2E)
private val CardBgFocus = Color(0xFF1E3558)
private val FocusCyan = Color(0xFF00E5FF)
private val BorderIdle = Color(0xFF1E2D4A)
private val TextMuted = Color(0xFF94A3B8)

/**
 * Diálogo de Configuración Inicial de Cabaña y Autenticación por PIN/Token.
 *
 * Características:
 * 1. CERO Tokens Hardcodeados: Obligatorio en el primer encendido si SharedPreferences está vacío.
 * 2. Selector de Cabaña / Habitación (1 al 12).
 * 3. Entrada de PIN / Token mediante teclado físico numérico del control remoto (0-9)
 *    y teclado visual en pantalla navegable con D-Pad.
 * 4. Validación en vivo contra GET https://elboom.cl/api/hotel-tv?unit={unitId}&token={token}.
 * 5. Si responde 200 OK: Guarda en SharedPreferences locales e ingresa al Launcher en modo Verde 🟢.
 * 6. Si responde 401: Muestra alerta destacada "PIN incorrecto".
 * 7. Si no hay conexión: Muestra "Sin conexión a internet".
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InitialSetupDialog(
    initialUnitId: Int,
    canCancel: Boolean = false,
    onConnect: suspend (Int, String) -> Result<Boolean>,
    onDismiss: () -> Unit
) {
    var selectedUnitId by remember { mutableIntStateOf(initialUnitId) }
    var pinText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val initialFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            initialFocus.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = {
            if (canCancel && !isLoading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = canCancel && !isLoading
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(colors = SetupBgGradient, radius = 1800f))
                .padding(horizontal = 36.dp, vertical = 24.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && !isLoading) {
                        val keyCode = keyEvent.nativeKeyEvent.keyCode
                        val unicode = keyEvent.nativeKeyEvent.unicodeChar
                        val charTyped: Char? = if (unicode != 0 && (unicode.toChar().isLetterOrDigit() || unicode.toChar() == '-' || unicode.toChar() == '_')) {
                            unicode.toChar()
                        } else {
                            when (keyCode) {
                                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> '0'
                                KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> '1'
                                KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> '2'
                                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> '3'
                                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> '4'
                                KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> '5'
                                KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> '6'
                                KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> '7'
                                KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> '8'
                                KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> '9'
                                else -> null
                            }
                        }

                        if (charTyped != null) {
                            if (pinText.length < 64) {
                                pinText += charTyped
                                errorMessage = null
                            }
                            return@onPreviewKeyEvent true
                        }

                        if (keyCode == KeyEvent.KEYCODE_DEL) {
                            if (pinText.isNotEmpty()) {
                                pinText = pinText.dropLast(1)
                                errorMessage = null
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // CABECERA SUPERIOR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(32.dp)
                                .background(FocusCyan, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_horizontal),
                            contentDescription = "El Boom",
                            modifier = Modifier.height(28.dp),
                            contentScale = ContentScale.FillHeight
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "·  Configuración Inicial de la Cabaña",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (canCancel) {
                        SetupCloseButton(onClick = { if (!isLoading) onDismiss() })
                    }
                }

                // CONTENEDOR DE DOS COLUMNAS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // COLUMNA IZQUIERDA: SELECTOR DE CABAÑA (46% ancho)
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x700B1424))
                            .border(1.dp, BorderIdle, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "1. Selecciona este Alojamiento:",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Indica qué cabaña o habitación corresponde a este televisor:",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(OFFICIAL_ACCOMMODATION_UNITS) { unit ->
                                val isSelected = unit.id == selectedUnitId
                                SetupUnitItemRow(
                                    unit = unit,
                                    isSelected = isSelected,
                                    onSelect = {
                                        selectedUnitId = unit.id
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    // COLUMNA DERECHA: ENTRADA DE PIN / TOKEN Y ACCIÓN (54% ancho)
                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x700B1424))
                            .border(1.dp, BorderIdle, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val currentUnitName = OFFICIAL_ACCOMMODATION_UNITS.firstOrNull { it.id == selectedUnitId }?.name ?: "Cabaña N°$selectedUnitId"
                            Text(
                                text = "2. PIN / Token de Acceso para $currentUnitName:",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ingresa el PIN asignado en la administración (elboom.cl/admin). Puedes usar el teclado numérico de tu control remoto.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // PANTALLA VISUAL DEL PIN
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070F1D))
                                    .border(
                                        width = if (errorMessage != null) 1.5.dp else 1.dp,
                                        color = if (errorMessage != null) Color(0xFFEF4444) else FocusCyan.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (pinText.isEmpty()) "Ingresa el PIN o Token..." else pinText,
                                        color = if (pinText.isEmpty()) Color.White.copy(alpha = 0.35f) else Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = if (pinText.isNotEmpty()) 2.sp else 0.sp
                                    )

                                    if (pinText.isNotEmpty()) {
                                        Text(
                                            text = "${pinText.length} carácteres",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // MENSAJE DE ERROR O ADVERTENCIA
                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.35f))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // TECLADO VISUAL D-PAD (3 x 4)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SetupKeypadButton(digit = "1", modifier = Modifier.weight(1f)) {
                                        pinText += "1"; errorMessage = null
                                    }
                                    SetupKeypadButton(digit = "2", modifier = Modifier.weight(1f)) {
                                        pinText += "2"; errorMessage = null
                                    }
                                    SetupKeypadButton(digit = "3", modifier = Modifier.weight(1f)) {
                                        pinText += "3"; errorMessage = null
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SetupKeypadButton(digit = "4", modifier = Modifier.weight(1f)) {
                                        pinText += "4"; errorMessage = null
                                    }
                                    SetupKeypadButton(digit = "5", modifier = Modifier.weight(1f)) {
                                        pinText += "5"; errorMessage = null
                                    }
                                    SetupKeypadButton(digit = "6", modifier = Modifier.weight(1f)) {
                                        pinText += "6"; errorMessage = null
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SetupKeypadButton(digit = "7", modifier = Modifier.weight(1f)) {
                                        pinText += "7"; errorMessage = null
                                    }
                                    SetupKeypadButton(digit = "8", modifier = Modifier.weight(1f)) {
                                        pinText += "8"; errorMessage = null
                                    }
                                    SetupKeypadButton(digit = "9", modifier = Modifier.weight(1f)) {
                                        pinText += "9"; errorMessage = null
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SetupKeypadButton(digit = "⌫ Borrar", modifier = Modifier.weight(1f), isSpecial = true) {
                                        if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                        errorMessage = null
                                    }
                                    SetupKeypadButton(digit = "0", modifier = Modifier.weight(1f)) {
                                        pinText += "0"; errorMessage = null
                                    }
                                    SetupKeypadButton(digit = "Limpiar", modifier = Modifier.weight(1f), isSpecial = true) {
                                        pinText = ""
                                        errorMessage = null
                                    }
                                }
                            }
                        }

                        // BOTÓN PRINCIPAL DE CONEXIÓN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (canCancel) {
                                SetupActionButton(
                                    label = "Cancelar",
                                    isPrimary = false,
                                    enabled = !isLoading,
                                    modifier = Modifier.weight(0.35f),
                                    onClick = onDismiss
                                )
                            }

                            SetupActionButton(
                                label = if (isLoading) "Verificando con El Boom..." else "Conectar con El Boom ⚡",
                                isPrimary = true,
                                enabled = !isLoading && pinText.isNotBlank(),
                                modifier = Modifier
                                    .weight(if (canCancel) 0.65f else 1f)
                                    .focusRequester(initialFocus),
                                onClick = {
                                    if (pinText.isBlank()) {
                                        errorMessage = "Por favor ingresa el PIN o Token de acceso."
                                        return@SetupActionButton
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val result = onConnect(selectedUnitId, pinText.trim())
                                        isLoading = false
                                        result.fold(
                                            onSuccess = {
                                                // La conexión fue exitosa, el diálogo se cierra automáticamente desde el ViewModel
                                            },
                                            onFailure = { error ->
                                                when (error) {
                                                    is AuthTokenException -> {
                                                        errorMessage = "⚠️ PIN / Token incorrecto para esta cabaña. Revisa en elboom.cl/admin"
                                                    }
                                                    is NetworkOfflineException -> {
                                                        errorMessage = "📡 Sin conexión con elboom.cl. Verifica tu cable de red o Wi-Fi."
                                                    }
                                                    else -> {
                                                        errorMessage = error.message ?: "Error al conectar con el servidor."
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupUnitItemRow(
    unit: AccommodationUnit,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "unit_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isFocused -> CardBgFocus
                    isSelected -> Color(0xFF0369A1).copy(alpha = 0.35f)
                    else -> CardBg
                }
            )
            .border(
                width = if (isFocused || isSelected) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> FocusCyan
                    isSelected -> Color(0xFF38BDF8)
                    else -> BorderIdle
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .onFocusChanged {
                if (it.isFocused) {
                    onSelect()
                }
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = unit.name,
                    color = if (isFocused || isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SELECCIONADA",
                            color = Color(0xFF4ADE80),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${unit.type} · Capacidad: ${unit.capacity} personas · ${unit.bedsDescription}",
                color = TextMuted,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) FocusCyan else Color.Transparent)
                .border(1.5.dp, if (isSelected) FocusCyan else BorderIdle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text(text = "✓", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SetupKeypadButton(
    digit: String,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "key_scale"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isFocused -> CardBgFocus
                    isSpecial -> Color.White.copy(alpha = 0.05f)
                    else -> CardBg
                }
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) FocusCyan else BorderIdle,
                shape = RoundedCornerShape(10.dp)
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
            text = digit,
            fontSize = if (isSpecial) 12.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isFocused -> FocusCyan
                isSpecial -> TextMuted
                else -> Color.White
            }
        )
    }
}

@Composable
private fun SetupActionButton(
    label: String,
    isPrimary: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.05f)
                    isPrimary && isFocused -> Color(0xFF00B4D8)
                    isPrimary -> Color(0xFF0284C7)
                    isFocused -> CardBgFocus
                    else -> CardBg
                }
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = when {
                    !enabled -> BorderIdle
                    isFocused -> FocusCyan
                    isPrimary -> Color(0xFF38BDF8)
                    else -> BorderIdle
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(enabled = enabled, interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.3f)
                isPrimary -> Color.White
                isFocused -> FocusCyan
                else -> TextMuted
            }
        )
    }
}

@Composable
private fun SetupCloseButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "close_scale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
