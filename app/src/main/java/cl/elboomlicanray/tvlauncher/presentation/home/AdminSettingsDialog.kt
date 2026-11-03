package cl.elboomlicanray.tvlauncher.presentation.home

import android.view.KeyEvent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.domain.model.AccommodationUnit
import cl.elboomlicanray.tvlauncher.domain.model.AppItem
import cl.elboomlicanray.tvlauncher.domain.model.OFFICIAL_ACCOMMODATION_UNITS
import cl.elboomlicanray.tvlauncher.presentation.components.rememberNativeDrawablePainter
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

// Tokens cromáticos y de cristal templado
private val AdminBgGradient = listOf(
    Color(0xF0081226), // 94% opacidad
    Color(0xF8040A17), // 97% opacidad
    Color(0xFC020409)  // 99% opacidad
)
private val CardBg = Color(0xFF0F1A2E)
private val CardBgFocus = Color(0xFF1E3558)
private val FocusCyan = Color(0xFF00E5FF)
private val CyanAccent = Color(0xFF38BDF8)
private val BorderIdle = Color(0xFF1E2D4A)
private val TextMuted = Color(0xFF94A3B8)

/**
 * Menú de Administrador y Configuración Hotelera (Pantalla Completa 100%).
 *
 * Características:
 * 1. Ocupa todo el espacio de pantalla disponible (fillMaxSize) con estética Luxury Dark Glassmorphism.
 * 2. Protección por PIN de 4 dígitos (con teclado visual D-Pad y detección de teclado físico del control remoto).
 * 3. Selector de Cabañas/Alojamientos oficiales Nobeds con visualización clara de ID y capacidad.
 * 4. Selector de Aplicaciones Visibles en pantalla con iconos oficiales y checkboxes intuitivos.
 * 5. Pestaña de Kiosco & Seguridad (Zapping Kiosk y salvapantallas).
 * 6. Pestaña de Actualizaciones OTA con verificación en la nube en vivo.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AdminSettingsDialog(
    currentUnitId: Int,
    availableApps: List<AppItem> = emptyList(),
    initiallySelectedApps: Set<String> = emptySet(),
    onVerifyPin: (String) -> Boolean,
    onSaveUnit: (Int) -> Unit,
    onSaveApps: (Set<String>) -> Unit = {},
    onOpenKioskSetup: () -> Unit,
    onCheckUpdateNow: () -> Unit = {},
    isSmartDnsEnabled: Boolean = false,
    smartDnsIp: String = "104.198.180.125",
    onToggleSmartDns: (Boolean) -> Unit = {},
    onOpenNetworkSettings: () -> Unit = {},
    isPinLockedOut: () -> Boolean = { false },
    getLockoutRemainingSeconds: () -> Int = { 0 },
    onDismiss: () -> Unit
) {
    var isPinVerified by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var selectedUnitId by remember { mutableIntStateOf(currentUnitId) }
    var selectedAppPackages by remember { mutableStateOf(initiallySelectedApps.toMutableSet()) }
    var activeAdminTab by remember { mutableStateOf("unit") } // "unit", "apps", "kiosk", "update"

    var isVisible by remember { mutableStateOf(false) }
    val primaryFocus = remember { FocusRequester() }
    val dashboardFocus = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isVisible = true
        try { primaryFocus.requestFocus() } catch (_: Exception) {}
    }

    LaunchedEffect(isPinVerified) {
        if (isPinVerified) {
            try {
                kotlinx.coroutines.delay(100)
                dashboardFocus.requestFocus()
            } catch (_: Exception) {}
        }
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
                    scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)),
            exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.96f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(colors = AdminBgGradient, radius = 1800f))
                    .padding(horizontal = 40.dp, vertical = 24.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        // Entrada directa por teclado físico o numérico del control remoto
                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && !isPinVerified) {
                            val keyCode = keyEvent.nativeKeyEvent.keyCode
                            val digitChar: Char? = when (keyCode) {
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

                            if (digitChar != null) {
                                if (pinText.length < 4) {
                                    val nextPin = pinText + digitChar
                                    pinText = nextPin
                                    pinError = false
                                    if (nextPin.length == 4) {
                                        if (isPinLockedOut()) {
                                            pinError = true
                                            pinText = ""
                                            val secs = getLockoutRemainingSeconds()
                                            Toast.makeText(context, "Seguridad activada: reintente en $secs seg.", Toast.LENGTH_SHORT).show()
                                        } else if (onVerifyPin(nextPin)) {
                                            isPinVerified = true
                                        } else {
                                            pinError = true
                                            pinText = ""
                                            if (isPinLockedOut()) {
                                                val secs = getLockoutRemainingSeconds()
                                                Toast.makeText(context, "Bloqueado por seguridad: reintente en $secs seg.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                return@onPreviewKeyEvent true
                            }

                            if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_CLEAR) {
                                if (pinText.isNotEmpty()) {
                                    pinText = pinText.dropLast(1)
                                    pinError = false
                                }
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    }
            ) {
                if (!isPinVerified) {
                    // =========================================================
                    // PANTALLA 1: INGRESO DE PIN DE SEGURIDAD (HOTELERO)
                    // =========================================================
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Tarjeta Central Flotante de PIN
                        Box(
                            modifier = Modifier
                                .width(540.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF0B1424).copy(alpha = 0.85f))
                                .border(1.5.dp, BorderIdle, RoundedCornerShape(28.dp))
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🛡️", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Panel de Administrador",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    AdminGlassCloseButton(onClick = onDismiss)
                                }

                                Text(
                                    text = "Ingresa el PIN de 4 dígitos para configurar esta pantalla (Por defecto: 1234)",
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                // 4 Indicadores visuales de dígitos PIN
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 4) {
                                        val isFilled = i < pinText.length
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isFilled) Color(0xFF0284C7).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f))
                                                .border(
                                                    width = if (isFilled) 2.dp else 1.dp,
                                                    color = if (isFilled) FocusCyan else if (pinError) Color(0xFFEF4444) else BorderIdle,
                                                    shape = RoundedCornerShape(14.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isFilled) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(FocusCyan)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (pinError) {
                                    Text(
                                        text = "⚠️ PIN incorrecto. Intenta con 1234.",
                                        color = Color(0xFFEF4444),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Teclado numérico en pantalla optimizado para D-Pad TV
                                val keypadRows = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("⌫", "0", "✓")
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    keypadRows.forEachIndexed { rIdx, rowKeys ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            rowKeys.forEachIndexed { cIdx, key ->
                                                val isInitial = rIdx == 0 && cIdx == 0
                                                AdminKeypadButton(
                                                    label = key,
                                                    modifier = if (isInitial) Modifier.focusRequester(primaryFocus) else Modifier,
                                                    onClick = {
                                                        when (key) {
                                                            "⌫" -> {
                                                                if (pinText.isNotEmpty()) {
                                                                    pinText = pinText.dropLast(1)
                                                                    pinError = false
                                                                }
                                                            }
                                                            "✓" -> {
                                                                if (isPinLockedOut()) {
                                                                    pinError = true
                                                                    pinText = ""
                                                                    val secs = getLockoutRemainingSeconds()
                                                                    Toast.makeText(context, "Seguridad activada: reintente en $secs seg.", Toast.LENGTH_SHORT).show()
                                                                } else if (onVerifyPin(pinText)) {
                                                                    isPinVerified = true
                                                                } else {
                                                                    pinError = true
                                                                    pinText = ""
                                                                    if (isPinLockedOut()) {
                                                                        val secs = getLockoutRemainingSeconds()
                                                                        Toast.makeText(context, "Bloqueado por seguridad: reintente en $secs seg.", Toast.LENGTH_LONG).show()
                                                                    } else {
                                                                        Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                            else -> {
                                                                if (pinText.length < 4) {
                                                                    val next = pinText + key
                                                                    pinText = next
                                                                    pinError = false
                                                                    if (next.length == 4) {
                                                                        if (isPinLockedOut()) {
                                                                            pinError = true
                                                                            pinText = ""
                                                                            val secs = getLockoutRemainingSeconds()
                                                                            Toast.makeText(context, "Seguridad activada: reintente en $secs seg.", Toast.LENGTH_SHORT).show()
                                                                        } else if (onVerifyPin(next)) {
                                                                            isPinVerified = true
                                                                        } else {
                                                                            pinError = true
                                                                            pinText = ""
                                                                            if (isPinLockedOut()) {
                                                                                val secs = getLockoutRemainingSeconds()
                                                                                Toast.makeText(context, "Bloqueado por seguridad: reintente en $secs seg.", Toast.LENGTH_LONG).show()
                                                                            } else {
                                                                                Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
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
                } else {
                    // =========================================================
                    // PANTALLA 2: DASHBOARD COMPLETO DE ADMINISTRACIÓN
                    // =========================================================
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. CABECERA SUPERIOR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "⚙️", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Configuración del Sistema Hotel TV",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.20f))
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "PIN VERIFICADO ✓",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4ADE80)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Alojamientos El Boom Lican Ray · Ajustes de cabaña, aplicaciones y sincronización",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }

                            AdminGlassCloseButton(onClick = onDismiss)
                        }

                        // 2. SELECTOR DE PESTAÑAS PRINCIPALES
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminTabButton(
                                title = "🏡 Cabaña / Alojamiento",
                                subtitle = "${OFFICIAL_ACCOMMODATION_UNITS.size} unidades oficiales",
                                isSelected = activeAdminTab == "unit",
                                modifier = Modifier.focusRequester(dashboardFocus),
                                onClick = { activeAdminTab = "unit" }
                            )

                            AdminTabButton(
                                title = "📱 Apps en Pantalla",
                                subtitle = "${availableApps.size} apps detectadas",
                                isSelected = activeAdminTab == "apps",
                                onClick = { activeAdminTab = "apps" }
                            )

                            AdminTabButton(
                                title = "🇺🇸 Netflix USA",
                                subtitle = if (isSmartDnsEnabled) "🟢 Smart DNS Activo" else "⚪ Inactivo (Chile)",
                                isSelected = activeAdminTab == "dns",
                                onClick = { activeAdminTab = "dns" }
                            )

                            AdminTabButton(
                                title = "🛡️ Kiosco & Seguridad",
                                subtitle = "Zapping TV y control",
                                isSelected = activeAdminTab == "kiosk",
                                onClick = { activeAdminTab = "kiosk" }
                            )

                            AdminTabButton(
                                title = "🚀 Actualización OTA",
                                subtitle = "Nube permanente en vivo",
                                isSelected = activeAdminTab == "update",
                                onClick = { activeAdminTab = "update" }
                            )
                        }

                        // 3. CONTENEDOR CENTRAL OCUPANDO TODO EL ESPACIO RESTANTE
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x700B1424))
                                .border(1.dp, BorderIdle, RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            when (activeAdminTab) {
                                "unit" -> {
                                    // PESTAÑA 1: SELECCIÓN DE CABAÑA (GRID 2 COLUMNAS)
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 8.dp)
                                    ) {
                                        items(OFFICIAL_ACCOMMODATION_UNITS) { unit ->
                                            AdminUnitCard(
                                                unit = unit,
                                                isSelected = unit.id == selectedUnitId,
                                                onSelect = { selectedUnitId = unit.id }
                                            )
                                        }
                                    }
                                }

                                "apps" -> {
                                    // PESTAÑA 2: SELECCIÓN DE APPS VISIBLES (GRID 2 COLUMNAS)
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Selecciona qué aplicaciones aparecerán en el catálogo principal de los huéspedes:",
                                                fontSize = 13.sp,
                                                color = TextMuted
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                AdminSmallActionButton(
                                                    label = "Seleccionar Todas",
                                                    onClick = {
                                                        selectedAppPackages = availableApps.map { it.packageName }.toMutableSet()
                                                    }
                                                )
                                                AdminSmallActionButton(
                                                    label = "Deseleccionar",
                                                    onClick = {
                                                        selectedAppPackages = mutableSetOf()
                                                    }
                                                )
                                            }
                                        }

                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(bottom = 8.dp)
                                        ) {
                                            items(availableApps) { app ->
                                                val isChecked = selectedAppPackages.isEmpty() || selectedAppPackages.contains(app.packageName)
                                                AdminAppToggleCard(
                                                    app = app,
                                                    isChecked = isChecked,
                                                    onToggle = {
                                                        val current = selectedAppPackages.toMutableSet()
                                                        if (current.isEmpty()) {
                                                            current.addAll(availableApps.map { it.packageName })
                                                        }
                                                        if (current.contains(app.packageName)) {
                                                            current.remove(app.packageName)
                                                        } else {
                                                            current.add(app.packageName)
                                                        }
                                                        selectedAppPackages = current
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                "kiosk" -> {
                                    // PESTAÑA 3: KIOSCO & SEGURIDAD TV
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            text = "Configuración de Seguridad y Modo Hotel TV:",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            AdminSecurityFeatureCard(
                                                title = "🛡️ Kiosco Zapping TV",
                                                description = "Bloquea el botón Home para que la TV siempre vuelva a El Boom TV y gestiona el lanzamiento directo de canales en vivo.",
                                                actionLabel = "Abrir Ajustes de Kiosco",
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    onDismiss()
                                                    onOpenKioskSetup()
                                                }
                                            )

                                            AdminSecurityFeatureCard(
                                                title = "🔐 Atajo Secreto '0000'",
                                                description = "Presionando cuatro veces seguidas la tecla '0' en el control remoto de la TV en cualquier momento se abre este panel.",
                                                actionLabel = "Activo por defecto",
                                                isActionEnabled = false,
                                                modifier = Modifier.weight(1f),
                                                onClick = {}
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            AdminSecurityFeatureCard(
                                                title = "🌄 Salvapantallas Ambiental",
                                                description = "Se activa automáticamente tras inactividad mostrando fotos de Lican Ray, reloj y clima para proteger la pantalla LED/OLED.",
                                                actionLabel = "Protección 24/7 Activa",
                                                isActionEnabled = false,
                                                modifier = Modifier.weight(1f),
                                                onClick = {}
                                            )

                                            AdminSecurityFeatureCard(
                                                title = "🏨 Sistema Nobeds PMS",
                                                description = "Sincroniza en tiempo real los nombres de huéspedes, fechas de check-in/check-out y red Wi-Fi según la cabaña configurada.",
                                                actionLabel = "Sincronización Automática",
                                                isActionEnabled = false,
                                                modifier = Modifier.weight(1f),
                                                onClick = {}
                                            )
                                        }
                                    }
                                }

                                "update" -> {
                                    // PESTAÑA 4: ACTUALIZACIONES OTA EN LA NUBE
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "Sistema Autónomo de Actualizaciones OTA (Over The Air):",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(CardBg)
                                                    .border(1.dp, BorderIdle, RoundedCornerShape(16.dp))
                                                    .padding(20.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    val currentVer = remember {
                                                        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "1.0" }
                                                    }
                                                    Text(text = "• Aplicación: El Boom TV Launcher\n• Paquete: cl.elboomlicanray.tvlauncher\n• Versión instalada: v$currentVer\n• Servidor Oficial: elboom.cl (https://elboom.cl/tv/version.json)\n• Canal de distribución: OTA Oficial Directo", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 19.sp)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(0.8f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color(0xFF0369A1).copy(alpha = 0.18f))
                                                    .border(1.5.dp, FocusCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                                    .padding(20.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    Text(text = "🚀", fontSize = 38.sp)
                                                    Text(
                                                        text = "Comprobar Actualizaciones",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Text(
                                                        text = "Consulta inmediatamente el servidor en la nube para descargar e instalar la versión más reciente.",
                                                        fontSize = 12.sp,
                                                        color = TextMuted,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    AdminActionButton(
                                                        label = "Verificar Ahora",
                                                        isPrimary = true,
                                                        onClick = onCheckUpdateNow
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                "dns" -> {
                                    // PESTAÑA: DESBLOQUEO SMART DNS USA
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "Servidor Proxy Smart DNS USA (Cuentas de Netflix EE.UU.):",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        // TARJETA PRINCIPAL DE ESTADO Y TOGGLE
                                        val dnsStatusBg = if (isSmartDnsEnabled) Color(0xFF0A2E28) else CardBg
                                        val dnsBorder = if (isSmartDnsEnabled) Color(0xFF10B981) else BorderIdle
                                        val dnsTitle = if (isSmartDnsEnabled) "🟢 DESBLOQUEO USA ACTIVADO" else "⚪ MODO CHILE ESTÁNDAR (DESACTIVADO)"
                                        val dnsDesc = if (isSmartDnsEnabled) {
                                            "El televisor está conectado al servidor Cloud en Iowa, EE.UU. ($smartDnsIp). Las cuentas de Netflix contratadas en Estados Unidos pueden reproducirse con normalidad."
                                        } else {
                                            "El televisor resuelve nombres mediante tu conexión local de Chile. Todas las cuentas chilenas y Zapping TV operan con máxima velocidad habitual."
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(dnsStatusBg)
                                                .border(1.5.dp, dnsBorder, RoundedCornerShape(16.dp))
                                                .padding(20.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = dnsTitle,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSmartDnsEnabled) Color(0xFF4ADE80) else Color.White
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = dnsDesc,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFFCBD5E1),
                                                        lineHeight = 18.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "• IP Servidor: $smartDnsIp   • Región: Google Cloud us-central1 (Iowa)   • Puerto DNS: 53 UDP",
                                                        fontSize = 11.5.sp,
                                                        color = TextMuted
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(20.dp))

                                                // BOTÓN DE ACCIÓN TOGGLE D-PAD
                                                AdminActionButton(
                                                    label = if (isSmartDnsEnabled) "🇨🇱 Desactivar (Modo Chile)" else "🇺🇸 Activar Desbloqueo USA",
                                                    isPrimary = !isSmartDnsEnabled,
                                                    onClick = {
                                                        val nextState = !isSmartDnsEnabled
                                                        onToggleSmartDns(nextState)
                                                        Toast.makeText(
                                                            context,
                                                            if (nextState) "¡Desbloqueo USA Activado! ($smartDnsIp)" else "Modo Chile Restaurado",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                )
                                            }
                                        }

                                        // TARJETAS INFORMATIVAS Y ACCESO RÁPIDO A RED
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(CardBg)
                                                    .border(1.dp, BorderIdle, RoundedCornerShape(14.dp))
                                                    .padding(16.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "⚡ Cero Impacto en Zapping y Streaming Nacional",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CyanAccent
                                                    )
                                                    Text(
                                                        text = "El Smart DNS no ralentiza tu Internet: solo redirige las consultas de validación de catálogo de Netflix a EE.UU. El video en vivo de Zapping TV y demás apps continúa descargándose a 100% de velocidad desde los servidores locales en Chile.",
                                                        fontSize = 12.sp,
                                                        color = TextMuted,
                                                        lineHeight = 17.sp
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(0.8f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(CardBg)
                                                    .border(1.dp, BorderIdle, RoundedCornerShape(14.dp))
                                                    .padding(16.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxHeight(),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(
                                                            text = "⚙️ Ajustes de Red del TV",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = "Accede directamente a la pantalla de red Wi-Fi / Ethernet de Android TV:",
                                                            fontSize = 12.sp,
                                                            color = TextMuted,
                                                            lineHeight = 16.sp
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    AdminSmallActionButton(
                                                        label = "🌐 Abrir Red de Android",
                                                        onClick = onOpenNetworkSettings
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. BARRA DE ACCIONES INFERIOR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedUnit = OFFICIAL_ACCOMMODATION_UNITS.firstOrNull { it.id == selectedUnitId }
                            Text(
                                text = "Alojamiento seleccionado: ${selectedUnit?.name ?: "Sin asignar"} · Apps visibles: ${if (selectedAppPackages.isEmpty()) availableApps.size else selectedAppPackages.size} de ${availableApps.size}",
                                fontSize = 12.5.sp,
                                color = TextMuted
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AdminActionButton(
                                    label = "Cancelar",
                                    isPrimary = false,
                                    onClick = onDismiss
                                )

                                AdminActionButton(
                                    label = "💾 Guardar y Aplicar",
                                    isPrimary = true,
                                    onClick = {
                                        onSaveUnit(selectedUnitId)
                                        onSaveApps(selectedAppPackages)
                                        Toast.makeText(context, "Ajustes de hotel guardados con éxito", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SUBCOMPONENTES ESTILIZADOS REUTILIZABLES (LUXURY GLASS DESIGN)
// =========================================================================

@Composable
private fun AdminGlassCloseButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "close_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.10f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusCyan else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✕ Cerrar",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) Color.Black else Color.White
        )
    }
}

@Composable
private fun AdminTabButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "tab_scale"
    )

    val bg = when {
        isFocused -> Color(0xFF0284C7)
        isSelected -> Color(0xFF0EA5E9).copy(alpha = 0.35f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    val border = when {
        isFocused -> FocusCyan
        isSelected -> CyanAccent
        else -> BorderIdle
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (isSelected || isFocused) 1.8.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = if (isSelected || isFocused) Color(0xFFE2E8F0) else TextMuted
            )
        }
    }
}

@Composable
private fun AdminUnitCard(
    unit: AccommodationUnit,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.025f else 1.0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "unit_scale"
    )

    val bg = when {
        isFocused -> CardBgFocus
        isSelected -> Color(0xFF132A4A)
        else -> CardBg
    }

    val borderColor = when {
        isFocused -> FocusCyan
        isSelected -> CyanAccent
        else -> BorderIdle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (isSelected || isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = unit.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "· Capacidad ${unit.capacity} pers.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ID Nobeds: ${unit.nobedsId} · ${unit.type} · ${unit.bedsDescription}",
                fontSize = 11.5.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.25f))
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "✓ SELECCIONADA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4ADE80)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Seleccionar",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun AdminAppToggleCard(
    app: AppItem,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.025f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "app_scale"
    )

    val bg = when {
        isFocused -> CardBgFocus
        isChecked -> Color(0xFF0F2642)
        else -> CardBg
    }

    val borderColor = when {
        isFocused -> FocusCyan
        isChecked -> Color(0xFF0284C7).copy(alpha = 0.7f)
        else -> BorderIdle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (isFocused || isChecked) 1.8.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                val iconPainter = rememberNativeDrawablePainter(app.iconDrawable)
                if (iconPainter != null) {
                    Image(
                        painter = iconPainter,
                        contentDescription = app.name,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(text = "📱", fontSize = 20.sp)
                }
            }

            Column {
                Text(
                    text = app.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isChecked) Color(0xFF10B981).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                .border(
                    1.dp,
                    if (isChecked) Color(0xFF10B981) else Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isChecked) "VISIBLE ✓" else "OCULTO ✕",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isChecked) Color(0xFF4ADE80) else TextMuted
            )
        }
    }
}

@Composable
private fun AdminSecurityFeatureCard(
    title: String,
    description: String,
    actionLabel: String,
    modifier: Modifier = Modifier,
    isActionEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused && isActionEnabled) 1.025f else 1.0f,
        animationSpec = spring(0.8f, 420f),
        label = "sec_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused && isActionEnabled) CardBgFocus else CardBg)
            .border(
                width = if (isFocused && isActionEnabled) 2.dp else 1.dp,
                color = if (isFocused && isActionEnabled) FocusCyan else BorderIdle,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (isActionEnabled) {
                    Modifier
                        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                        .focusable(interactionSource = interactionSource)
                } else Modifier
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActionEnabled) Color(0xFF0284C7).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActionEnabled) CyanAccent else TextMuted
                    )
                }
            }
            Text(text = description, fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun AdminActionButton(
    label: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "btn_scale"
    )

    val backgroundModifier = when {
        isFocused && isPrimary -> Modifier.background(FocusCyan)
        isFocused -> Modifier.background(Color(0xFF334155))
        isPrimary -> Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF0EA5E9))))
        else -> Modifier.background(CardBg)
    }

    val textColor = when {
        isFocused && isPrimary -> Color.Black
        else -> Color.White
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .then(backgroundModifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else BorderIdle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun AdminSmallActionButton(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) FocusCyan else Color.White.copy(alpha = 0.08f))
            .border(1.dp, if (isFocused) Color.White else BorderIdle, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFocused) Color.Black else Color.White
        )
    }
}

@Composable
private fun AdminKeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "key_scale"
    )

    val isAction = label == "⌫" || label == "✓"
    val bg = when {
        isFocused -> FocusCyan
        isAction -> Color(0xFF1E293B)
        else -> Color(0xFF162032)
    }

    val textColor = when {
        isFocused -> Color(0xFF0B132B)
        label == "✓" -> Color(0xFF4ADE80)
        label == "⌫" -> Color(0xFFF87171)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .size(width = 74.dp, height = 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else BorderIdle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
