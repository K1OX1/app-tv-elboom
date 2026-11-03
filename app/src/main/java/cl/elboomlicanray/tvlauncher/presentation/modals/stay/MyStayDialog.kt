package cl.elboomlicanray.tvlauncher.presentation.modals.stay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import cl.elboomlicanray.tvlauncher.R
import cl.elboomlicanray.tvlauncher.domain.model.AccommodationUnit
import cl.elboomlicanray.tvlauncher.domain.model.DynamicCabinDetails
import cl.elboomlicanray.tvlauncher.domain.model.DynamicPropertyInfo
import cl.elboomlicanray.tvlauncher.domain.model.GuestStayInfo
import cl.elboomlicanray.tvlauncher.presentation.components.qr.QrCodeView
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Diálogo Modal: "Mi Alojamiento" para Smart TV (Alojamiento Boutique El Boom).
 *
 * Reproduce fielmente la composición visual del diseño de referencia:
 * - Fondo panorámico cálido y cinematográfico del interior/terraza de la cabaña con degradados sutiles.
 * - Cabecera superior con título dinámico de la cabaña, subtítulo descriptivo, pestañas de filtro
 *   (Reserva, Fotos, Camas, Video Tour) con estilo boutique e indicador turquesa, y botón Cerrar.
 * - Grid de 3 columnas principales:
 *   1. "Tu alojamiento": Foto 16:9, tipo de cabaña, capacidad, m², baños y horarios oficiales de check-in / check-out.
 *   2. "Comodidades": Badges circulares con Estacionamiento techado, Horario de silencio y Calefacción a leña / Tinaja.
 *   3. "Conéctate": Código QR de alta visibilidad para Wi-Fi Starlink (Red y Clave) y contacto directo de Recepción WhatsApp.
 * - Franja inferior de 3 tarjetas interactivas de ancho completo:
 *   - "Fotos · {cantidad}" (abre galería HD adaptativa)
 *   - "Camas" (abre distribución detallada de dormitorios)
 *   - "Video Tour" (reproduce recorrido cinematográfico en TV o móvil)
 *
 * Todos los textos y métricas se alimentan dinámicamente de los datos reales del CMS web (elboom.cl)
 * y Channel Manager Nobeds (nombre de huésped, fechas de estadía, montos, estacionamiento, etc.).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyStayDialog(
    unit: AccommodationUnit,
    stay: GuestStayInfo,
    dynamicCabin: DynamicCabinDetails? = null,
    propertyInfo: DynamicPropertyInfo = DynamicPropertyInfo(),
    selectedLanguage: String = "es",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    // Lista de fotos completas de la cabaña desde el CMS web
    val allImages = remember(dynamicCabin) {
        val list = dynamicCabin?.images ?: emptyList()
        if (list.isNotEmpty()) list else listOf("https://i.postimg.cc/KvkXjb86/1.jpg")
    }

    // Filtro robusto para excluir banners y collages informativos del CMS web
    val isMarketingCollage = remember {
        { url: String ->
            url.contains("KvkXjb86") || url.contains("P5WRv5sQ") ||
            url.contains("T3MsPWSR") || url.contains("mkPnP3B5") ||
            url.contains("MHXGXLcS") || url.contains("GpcdHfNM") ||
            url.endsWith("/1.jpg") || url.endsWith("/2.jpg") ||
            url.endsWith("/3.jpg") || url.endsWith("/4.jpg")
        }
    }

    // Lista de fotos fotográficas reales y limpias de la cabaña
    val realPhotoImages = remember(allImages) {
        val clean = allImages.filter { !isMarketingCollage(it) }
        if (clean.isNotEmpty()) clean else allImages
    }

    var selectedImageIndex by remember { mutableIntStateOf(0) }

    val photoTabLabel = remember(allImages) { "Fotos (${allImages.size.coerceAtLeast(1)})" }
    val tabs = remember(photoTabLabel) {
        listOf(
            "Reserva",
            photoTabLabel,
            "Camas",
            "Video Tour"
        )
    }

    var selectedTab by remember { mutableStateOf("Reserva") }
    var isPlayingVideoOverlay by remember { mutableStateOf(false) }

    val closeFocusRequester = remember { FocusRequester() }
    val initialTabFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isVisible = true
        try {
            initialTabFocusRequester.requestFocus()
        } catch (_: Exception) {
            try {
                closeFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = {
            if (isPlayingVideoOverlay) {
                isPlayingVideoOverlay = false
            } else if (selectedTab != "Reserva") {
                selectedTab = "Reserva"
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        // Manejo del botón BACK del control remoto
        BackHandler {
            if (isPlayingVideoOverlay) {
                isPlayingVideoOverlay = false
            } else if (selectedTab != "Reserva") {
                selectedTab = "Reserva"
            } else {
                onDismiss()
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.97f, animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)),
            exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.97f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF030712))
            ) {
                // =========================================================================
                // 1. FOTOGRAFÍA PANORÁMICA DE FONDO (ATMÓSFERA CINEMATOGRÁFICA A PANTALLA COMPLETA)
                // =========================================================================
                if (selectedTab == "Reserva") {
                    Image(
                        painter = painterResource(id = R.drawable.bg_stay_cinematic),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val heroBackground = realPhotoImages.getOrNull(selectedImageIndex)
                        ?: realPhotoImages.firstOrNull() ?: propertyInfo.heroBgImage
                    AsyncImage(
                        model = heroBackground,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Scrim oscuro cálido para máxima legibilidad sin apagar la calidez del paisaje
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color(0xB8050B14),
                                0.45f to Color(0xCC07101E),
                                1.00f to Color(0xEA040810)
                            )
                        )
                )

                // =========================================================================
                // 2. CAPA PRINCIPAL DE INTERFAZ DE USUARIO (10-FOOT UI PARA TV)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp, vertical = 22.dp)
                ) {
                    // ---------------------------------------------------------------------
                    // BARRA SUPERIOR: NOMBRE DE CABAÑA, PESTAÑAS Y CERRAR
                    // ---------------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Título y Subtítulo dinámico
                        Column {
                            val cabinTitle = dynamicCabin?.name?.ifBlank { unit.name } ?: unit.name
                            Text(
                                text = cabinTitle,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val subtitleText = remember(stay) {
                                if (stay.isOccupied) {
                                    val guestName = stay.guestFullName.ifBlank { "Huésped registrado" }
                                    "Estadía de $guestName · ${stay.nights} noche${if (stay.nights > 1) "s" else ""} (${stay.checkinDate} al ${stay.checkoutDate})"
                                } else {
                                    "Información de tu estadía y características del alojamiento"
                                }
                            }
                            Text(
                                text = subtitleText,
                                fontSize = 12.sp,
                                color = Color(0xFFBAC4D0)
                            )
                        }

                        // Pestañas de Navegación Superiores (Estilo Boutique)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { index, tabName ->
                                val isSelected = tabName == selectedTab ||
                                        (tabName.startsWith("Fotos") && selectedTab.startsWith("Fotos"))
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()

                                val tabModifier = if (index == 0) {
                                    Modifier.focusRequester(initialTabFocusRequester)
                                } else {
                                    Modifier
                                }

                                Box(
                                    modifier = tabModifier
                                        .graphicsLayer {
                                            val scale = if (isFocused) 1.06f else 1.0f
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            when {
                                                isFocused -> Color(0xFF00E5FF).copy(alpha = 0.22f)
                                                isSelected -> Color(0xFF00E5FF).copy(alpha = 0.16f)
                                                else -> Color.White.copy(alpha = 0.05f)
                                            }
                                        )
                                        .border(
                                            width = if (isFocused || isSelected) 1.5.dp else 1.dp,
                                            color = when {
                                                isFocused -> Color(0xFF00E5FF)
                                                isSelected -> Color(0xFF00E5FF)
                                                else -> Color.White.copy(alpha = 0.14f)
                                            },
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            selectedTab = tabName
                                        }
                                        .focusable(interactionSource = interactionSource)
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tabName,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isFocused -> Color(0xFF00E5FF)
                                            isSelected -> Color(0xFF00E5FF)
                                            else -> Color.White.copy(alpha = 0.85f)
                                        }
                                    )
                                }
                            }

                            // Botón Cerrar (Píldora con borde fino)
                            val closeInteraction = remember { MutableInteractionSource() }
                            val closeFocused by closeInteraction.collectIsFocusedAsState()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .focusRequester(closeFocusRequester)
                                    .graphicsLayer {
                                        val scale = if (closeFocused) 1.06f else 1.0f
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (closeFocused) Color(0xFF00E5FF).copy(alpha = 0.20f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        width = if (closeFocused) 1.5.dp else 1.dp,
                                        color = if (closeFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable(
                                        interactionSource = closeInteraction,
                                        indication = null
                                    ) {
                                        onDismiss()
                                    }
                                    .focusable(interactionSource = closeInteraction)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                CloseCanvasIcon(
                                    color = if (closeFocused) Color(0xFF00E5FF) else Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Cerrar",
                                    fontSize = 12.sp,
                                    fontWeight = if (closeFocused) FontWeight.Bold else FontWeight.Normal,
                                    color = if (closeFocused) Color(0xFF00E5FF) else Color.White
                                )
                            }
                        }
                    }

                    // ---------------------------------------------------------------------
                    // CUERPO CENTRAL DE LA PANTALLA
                    // ---------------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when {
                            selectedTab == "Reserva" -> {
                                // VISTA PRINCIPAL: 3 COLUMNAS EXACTAS SEGÚN MAQUETA
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // =====================================================
                                    // COLUMNA 1: "Tu alojamiento"
                                    // =====================================================
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xCC0D1522))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            // Encabezado con ícono casa turquesa
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                HomeCanvasIcon(
                                                    color = Color(0xFF00E5FF),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Tu alojamiento",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Miniatura 16:9 con foto fotográfica real de la cabaña
                                            val cabinThumb = realPhotoImages.firstOrNull { it.contains("1Xbjwgzh") || it.contains("5.jpg") }
                                                ?: realPhotoImages.firstOrNull() ?: propertyInfo.heroBgImage
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(115.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF0B111A))
                                            ) {
                                                AsyncImage(
                                                    model = cabinThumb,
                                                    contentDescription = "Cabaña",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Nombre / Tipo de Cabaña
                                            val cabinType = dynamicCabin?.type?.ifBlank { unit.type } ?: unit.type
                                            Text(
                                                text = cabinType,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Fila de Capacidad, Metros cuadrados y Baños con íconos
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val capacity = dynamicCabin?.capacity ?: unit.capacity
                                                val area = dynamicCabin?.area ?: unit.area
                                                val bathrooms = dynamicCabin?.bathrooms ?: unit.bathrooms

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    PersonCanvasIcon(
                                                        color = Color(0xFF00E5FF),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "$capacity personas",
                                                        fontSize = 11.5.sp,
                                                        color = Color.White.copy(alpha = 0.85f)
                                                    )
                                                }

                                                Text(text = "·", color = Color.White.copy(alpha = 0.35f))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    RulerCanvasIcon(
                                                        color = Color(0xFF00E5FF),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "$area m²",
                                                        fontSize = 11.5.sp,
                                                        color = Color.White.copy(alpha = 0.85f)
                                                    )
                                                }

                                                Text(text = "·", color = Color.White.copy(alpha = 0.35f))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    ShowerCanvasIcon(
                                                        color = Color(0xFF00E5FF),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "$bathrooms baño${if (bathrooms > 1) "s" else ""}",
                                                        fontSize = 11.5.sp,
                                                        color = Color.White.copy(alpha = 0.85f)
                                                    )
                                                }
                                            }
                                        }

                                        // Fila compacta de Check-in y Check-out
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(Color.White.copy(alpha = 0.08f))
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Check-in
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    ClockCanvasIcon(
                                                        color = Color(0xFF00E5FF),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = if (stay.isOccupied) "CHECK-IN" else "HORARIO CHECK-IN",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.6.sp,
                                                            color = Color(0xFF94A3B8),
                                                            maxLines = 1
                                                        )
                                                        val checkinVal = if (stay.isOccupied && stay.checkinDate.isNotBlank()) {
                                                            stay.checkinDate
                                                        } else {
                                                            propertyInfo.checkIn.ifBlank { "15:00 a 22:00 hrs" }
                                                        }
                                                        Text(
                                                            text = checkinVal,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = if (stay.isOccupied) "Recepción ${stay.checkinOfficialTime}" else "Recepción y entrega de llaves",
                                                            fontSize = 8.5.sp,
                                                            color = Color(0xFF94A3B8),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                // Check-out
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    ClockCanvasIcon(
                                                        color = Color(0xFF00E5FF),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = if (stay.isOccupied) "CHECK-OUT" else "HORARIO CHECK-OUT",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.6.sp,
                                                            color = Color(0xFF94A3B8),
                                                            maxLines = 1
                                                        )
                                                        val checkoutVal = if (stay.isOccupied && stay.checkoutDate.isNotBlank()) {
                                                            stay.checkoutDate
                                                        } else {
                                                            propertyInfo.checkOut.ifBlank { "09:00 a 11:00 hrs" }
                                                        }
                                                        Text(
                                                            text = checkoutVal,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = if (stay.isOccupied) "Salida ${stay.checkoutOfficialTime}" else "Salida y entrega de llaves",
                                                            fontSize = 8.5.sp,
                                                            color = Color(0xFF94A3B8),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // =====================================================
                                    // COLUMNA 2: "Comodidades"
                                    // =====================================================
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xCC0D1522))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            // Encabezado con ícono engranaje/estrella dorada
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                GearCanvasIcon(
                                                    color = Color(0xFFFBBF24),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Comodidades",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFBBF24)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(18.dp))

                                            // Fila 1: Estacionamiento techado
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E2836)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CarCanvasIcon(
                                                        color = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = "Estacionamiento techado",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    val parkingSub = if (stay.isOccupied && stay.vehiclePlate.isNotBlank() && stay.vehiclePlate != "Estacionamiento Asignado") {
                                                        "Patente registrada: ${stay.vehiclePlate}"
                                                    } else {
                                                        "Dentro del recinto privado techado"
                                                    }
                                                    Text(
                                                        text = parkingSub,
                                                        fontSize = 10.5.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(18.dp))

                                            // Fila 2: Horario de silencio
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E2836)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    MoonCanvasIcon(
                                                        color = Color.White,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = "Horario de silencio",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "23:00 a 11:00 hrs",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "Prohibida música fuerte para permitir el descanso",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(18.dp))

                                            // Fila 3: Calefacción a leña / Tinaja
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E2836)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    FlameCanvasIcon(
                                                        color = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    val flameTitle = if (unit.hasTinaja) "Tinaja y Calefacción a leña" else "Calefacción a leña"
                                                    Text(
                                                        text = flameTitle,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    val flameSub = if (unit.hasTinaja) "Tinaja de madera nativa y estufa Bosca" else "A leña entregada por recepción"
                                                    Text(
                                                        text = flameSub,
                                                        fontSize = 10.5.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Text(
                                                        text = "La leña tiene costo adicional",
                                                        fontSize = 9.5.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }

                                        // Estado de cuenta / saldo si está ocupada
                                        if (stay.isOccupied) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (stay.isPaid) Color(0xFF10B981).copy(alpha = 0.15f)
                                                        else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (stay.isPaid) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (stay.isPaid) "ESTADO: PAGADO AL DÍA" else "SALDO PENDIENTE",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (stay.isPaid) Color(0xFF34D399) else Color(0xFFFBBF24)
                                                    )
                                                    Text(
                                                        text = if (stay.isPaid) "$0 CLP" else "$${stay.balance.formatNumber()} CLP",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // =====================================================
                                    // COLUMNA 3: "Conéctate" (Wi-Fi Starlink + WhatsApp Recepción)
                                    // =====================================================
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xCC0D1522))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                            .padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Encabezado con ícono Wi-Fi turquesa
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                WifiCanvasIcon(
                                                    color = Color(0xFF00E5FF),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Conéctate",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Tarjeta blanca con QR de Wi-Fi Starlink de alta visibilidad
                                            Box(
                                                modifier = Modifier
                                                    .size(108.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.White)
                                                    .padding(6.dp)
                                            ) {
                                                QrCodeView(
                                                    content = "WIFI:T:WPA;S:${stay.wifiNetwork};P:${stay.wifiPassword};;",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "Red: ${stay.wifiNetwork}",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Clave: ${stay.wifiPassword}",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E5FF)
                                            )
                                        }

                                        // Contacto Recepción WhatsApp
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(Color.White.copy(alpha = 0.08f))
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                WhatsAppCanvasIcon(
                                                    color = Color(0xFF00E5FF),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "CONTACTO RECEPCIÓN",
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.6.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Text(
                                                        text = stay.hostWhatsappFormatted.ifBlank { "+56 9 7269 4597" },
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "WhatsApp disponible 24/7",
                                                        fontSize = 9.5.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            selectedTab.startsWith("Fotos") -> {
                                // VISOR HD DE FOTOS
                                FullPhotoGalleryView(
                                    images = allImages,
                                    selectedIndex = selectedImageIndex,
                                    cabinName = dynamicCabin?.name ?: unit.name,
                                    onSelectIndex = { selectedImageIndex = it },
                                    onBack = { selectedTab = "Reserva" }
                                )
                            }

                            selectedTab == "Camas" -> {
                                // DISTRIBUCIÓN DE DORMITORIOS Y CAMAS
                                BedroomDistributionView(
                                    dynamicCabin = dynamicCabin,
                                    unit = unit
                                )
                            }

                            selectedTab == "Video Tour" -> {
                                // REPRODUCTOR Y DETALLE DE VIDEO TOUR
                                VideoTourPanelView(
                                    dynamicCabin = dynamicCabin,
                                    unit = unit,
                                    onPlayFullscreen = { isPlayingVideoOverlay = true }
                                )
                            }
                        }
                    }

                    // ---------------------------------------------------------------------
                    // FRANJA INFERIOR: 3 TARJETAS INTERACTIVAS (FOTOS, CAMAS, VIDEO TOUR)
                    // ---------------------------------------------------------------------
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TARJETA 1: Fotos · {cantidad}
                        val photoInteraction = remember { MutableInteractionSource() }
                        val photoFocused by photoInteraction.collectIsFocusedAsState()
                        val isPhotoActive = selectedTab.startsWith("Fotos")

                        val photoThumb = remember(realPhotoImages) {
                            realPhotoImages.firstOrNull { it.contains("1Xbjwgzh") || it.contains("5.jpg") }
                                ?: realPhotoImages.firstOrNull() ?: propertyInfo.heroBgImage
                        }

                        InteractiveActionCard(
                            title = "Fotos · ${allImages.size.coerceAtLeast(1)}",
                            imageUrl = photoThumb,
                            icon = { PhotoCanvasIcon(color = Color.White, modifier = Modifier.size(16.dp)) },
                            isFocused = photoFocused,
                            isActive = isPhotoActive,
                            interactionSource = photoInteraction,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = photoTabLabel }
                        )

                        // TARJETA 2: Camas
                        val bedInteraction = remember { MutableInteractionSource() }
                        val bedFocused by bedInteraction.collectIsFocusedAsState()
                        val isBedActive = selectedTab == "Camas"

                        val bedThumb = remember(realPhotoImages) {
                            realPhotoImages.firstOrNull { it.contains("14xv4LvW") || it.contains("12.jpg") || it.contains("13.jpg") }
                                ?: realPhotoImages.getOrNull(1) ?: photoThumb
                        }

                        InteractiveActionCard(
                            title = "Camas",
                            imageUrl = bedThumb,
                            icon = { BedCanvasIcon(color = Color.White, modifier = Modifier.size(17.dp)) },
                            isFocused = bedFocused,
                            isActive = isBedActive,
                            interactionSource = bedInteraction,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = "Camas" }
                        )

                        // TARJETA 3: Video Tour
                        val videoInteraction = remember { MutableInteractionSource() }
                        val videoFocused by videoInteraction.collectIsFocusedAsState()
                        val isVideoActive = selectedTab == "Video Tour"

                        val videoThumb = remember(realPhotoImages) {
                            realPhotoImages.firstOrNull { it.contains("mDPx9WSB") || it.contains("17.jpg") || it.contains("3wJQKv4Z") }
                                ?: realPhotoImages.getOrNull(2) ?: photoThumb
                        }

                        InteractiveActionCard(
                            title = "Video Tour",
                            imageUrl = videoThumb,
                            icon = { VideoCameraCanvasIcon(color = Color.White, modifier = Modifier.size(16.dp)) },
                            showPlayOverlay = true,
                            isFocused = videoFocused,
                            isActive = isVideoActive,
                            interactionSource = videoInteraction,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = "Video Tour" }
                        )
                    }
                }

                // =========================================================================
                // 3. OVERLAY DE VIDEO A PANTALLA COMPLETA
                // =========================================================================
                if (isPlayingVideoOverlay) {
                    val youtubeId = dynamicCabin?.youtubeVideoId?.ifEmpty { "BoOdYfc7Hx8" } ?: "BoOdYfc7Hx8"
                    val videoUrl = "https://www.youtube.com/watch?v=$youtubeId"
                    FullScreenVideoOverlay(
                        title = dynamicCabin?.name ?: unit.name,
                        videoUrl = videoUrl,
                        onClose = { isPlayingVideoOverlay = false }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de la franja inferior (Fotos, Camas, Video Tour).
 */
@Composable
private fun InteractiveActionCard(
    title: String,
    imageUrl: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    showPlayOverlay: Boolean = false,
    isFocused: Boolean = false,
    isActive: Boolean = false,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                val scale = if (isFocused) 1.035f else 1.0f
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .border(
                width = when {
                    isFocused -> 2.dp
                    isActive -> 1.5.dp
                    else -> 0.8.dp
                },
                color = when {
                    isFocused -> Color(0xFF00E5FF)
                    isActive -> Color(0xFF00E5FF)
                    else -> Color.White.copy(alpha = 0.16f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .focusable(interactionSource = interactionSource)
    ) {
        // Foto de fondo panorámica
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Degradado vertical elegante para legibilidad óptima
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.40f),
                        1.00f to Color.Black.copy(alpha = 0.88f)
                    )
                )
        )

        // Botón Play central si es Video Tour (fiel a la maqueta)
        if (showPlayOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.6.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PlayCanvasIcon(color = Color.White, modifier = Modifier.size(15.dp))
            }
        }

        // Título e Ícono a la izquierda + Flecha a la derecha en la base de la tarjeta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon()
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused || isActive) Color(0xFF00E5FF) else Color.White
                )
            }

            ChevronRightCanvasIcon(
                color = if (isFocused || isActive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Visor Adaptativo HD de Fotos con galería y miniaturas.
 */
@Composable
private fun FullPhotoGalleryView(
    images: List<String>,
    selectedIndex: Int,
    cabinName: String,
    onSelectIndex: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        val currentPhotoUrl = images.getOrNull(selectedIndex) ?: ""
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (currentPhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = currentPhotoUrl,
                    contentDescription = "Foto ${selectedIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.80f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Foto ${selectedIndex + 1} de ${images.size} · $cabinName",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fila de miniaturas navegable con D-Pad
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(images) { index, imgUrl ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()

                LaunchedEffect(isFocused) {
                    if (isFocused) onSelectIndex(index)
                }

                Box(
                    modifier = Modifier
                        .size(width = 92.dp, height = 52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black)
                        .border(
                            width = if (isSelected || isFocused) 2.dp else 0.8.dp,
                            color = if (isSelected || isFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            onSelectIndex(index)
                        }
                        .focusable(interactionSource = interactionSource)
                ) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Vista de Camas y Distribución de Dormitorios.
 */
@Composable
private fun BedroomDistributionView(
    dynamicCabin: DynamicCabinDetails?,
    unit: AccommodationUnit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Columna 1: Dormitorios
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC0D1522))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Distribución de Dormitorios",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val bedrooms = dynamicCabin?.bedrooms ?: emptyList()
            if (bedrooms.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bedrooms.size) { idx ->
                        val rm = bedrooms[idx]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2836).copy(alpha = 0.7f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = rm.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = rm.bedsDescription.ifEmpty { "Camas listas y equipadas" },
                                fontSize = 11.5.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = dynamicCabin?.bedsSummary?.ifEmpty { unit.bedsDescription } ?: unit.bedsDescription,
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 18.sp
                )
            }
        }

        // Columna 2: Equipamiento y Amenidades
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC0D1522))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Equipamiento Incluido",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFBBF24)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val amenities = dynamicCabin?.amenities ?: unit.amenities
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(amenities.size) { idx ->
                    val amen = amenities[idx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E2836).copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 11.sp,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = amen,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vista de Video Tour con opción de reproducir en TV o móvil.
 */
@Composable
private fun VideoTourPanelView(
    dynamicCabin: DynamicCabinDetails?,
    unit: AccommodationUnit,
    onPlayFullscreen: () -> Unit
) {
    val youtubeId = dynamicCabin?.youtubeVideoId?.ifEmpty { "BoOdYfc7Hx8" } ?: "BoOdYfc7Hx8"
    val youtubeUrl = "https://www.youtube.com/watch?v=$youtubeId"
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC0D1522))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Video Tour de ${dynamicCabin?.name ?: unit.name}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = dynamicCabin?.description?.ifEmpty {
                    "Recorrido en video cinematográfico para conocer los dormitorios, cocina equipada, estufa Bosca a leña y entorno natural de la cabaña."
                } ?: "",
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 18.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Reproduce el video a pantalla completa en este televisor:",
                fontSize = 11.5.sp,
                color = Color(0xFFBAC4D0)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val playInteraction = remember { MutableInteractionSource() }
            val playFocused by playInteraction.collectIsFocusedAsState()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .graphicsLayer {
                        val scale = if (playFocused) 1.05f else 1.0f
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(50))
                    .background(if (playFocused) Color(0xFFDC2626) else Color(0xFFE50914))
                    .border(
                        width = if (playFocused) 2.dp else 0.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(
                        interactionSource = playInteraction,
                        indication = null
                    ) {
                        onPlayFullscreen()
                    }
                    .focusable(interactionSource = playInteraction)
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                PlayCanvasIcon(color = Color.White, modifier = Modifier.size(12.dp))
                Text(
                    text = "▶ Reproducir Video Tour en TV",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Tarjeta QR para ver en el móvil
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC0D1522))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(6.dp)
            ) {
                QrCodeView(
                    content = youtubeUrl,
                    modifier = Modifier.size(95.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ABRIR EN EL MÓVIL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E5FF)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Escanea con la cámara",
                fontSize = 9.5.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

/**
 * Overlay de reproducción de video a pantalla completa.
 */
@OptIn(UnstableApi::class)
@Composable
private fun FullScreenVideoOverlay(
    title: String,
    videoUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val returnFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            returnFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val youtubeVideoId = remember(videoUrl) { extractYoutubeVideoId(videoUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var exoPlayerInstance by remember { mutableStateOf<ExoPlayer?>(null) }
    var feedbackText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedbackText) {
        if (feedbackText != null) {
            kotlinx.coroutines.delay(850)
            feedbackText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Back, Key.Escape -> {
                            onClose()
                            true
                        }
                        Key.DirectionRight -> {
                            if (youtubeVideoId != null) {
                                webViewInstance?.evaluateJavascript("seekRelative(10);", null)
                            } else {
                                exoPlayerInstance?.let { p -> p.seekTo(p.currentPosition + 10_000L) }
                            }
                            feedbackText = "Adelantar +10s ⏩"
                            true
                        }
                        Key.DirectionLeft -> {
                            if (youtubeVideoId != null) {
                                webViewInstance?.evaluateJavascript("seekRelative(-10);", null)
                            } else {
                                exoPlayerInstance?.let { p -> p.seekTo(maxOf(0L, p.currentPosition - 10_000L)) }
                            }
                            feedbackText = "Retroceder -10s ⏪"
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        if (youtubeVideoId != null) {
            val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        html, body { width: 100%; height: 100%; background-color: #000; overflow: hidden; }
                        #ytIframe { width: 100%; height: 100%; border: none; }
                        .ytp-chrome-top, .ytp-watermark, .ytp-youtube-button, .ytp-show-cards-title,
                        .ytp-pause-overlay, .ytp-ad-overlay-container, .ytp-ad-message-container,
                        .video-ads, .ytp-ad-module, .ytp-ce-element { display: none !important; }
                    </style>
                </head>
                <body>
                    <iframe id="ytIframe"
                        src="https://www.youtube-nocookie.com/embed/$youtubeVideoId?enablejsapi=1&autoplay=1&controls=0&rel=0&modestbranding=1&iv_load_policy=3&fs=0&playsinline=1&origin=https://elboom.cl" 
                        title="Video Tour" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                        allowfullscreen>
                    </iframe>
                    <script>
                        var tag = document.createElement('script');
                        tag.src = "https://www.youtube.com/iframe_api";
                        var first = document.getElementsByTagName('script')[0];
                        first.parentNode.insertBefore(tag, first);

                        var player;
                        function onYouTubeIframeAPIReady() {
                            player = new YT.Player('ytIframe', {
                                events: {
                                    'onReady': function(e) { e.target.playVideo(); }
                                }
                            });
                        }
                        function seekRelative(sec) {
                            if (player && typeof player.getCurrentTime === 'function') {
                                var c = player.getCurrentTime();
                                player.seekTo(Math.max(0, c + sec), true);
                            } else {
                                var ifr = document.getElementById('ytIframe');
                                if (ifr && ifr.contentWindow) {
                                    ifr.contentWindow.postMessage(JSON.stringify({
                                        event: 'command',
                                        func: 'seekTo',
                                        args: [sec > 0 ? 10 : 0, true]
                                    }), '*');
                                }
                            }
                        }
                        function togglePlayPause() {
                            if (player && typeof player.getPlayerState === 'function') {
                                if (player.getPlayerState() === 1) player.pauseVideo();
                                else player.playVideo();
                            }
                        }
                        setInterval(function() {
                            try {
                                var btn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-ad-text');
                                if (btn) btn.click();
                                var vid = document.querySelector('.ad-showing video');
                                if (vid && vid.duration) vid.currentTime = vid.duration;
                            } catch(e) {}
                        }, 200);
                    </script>
                </body>
                </html>
            """.trimIndent()

            DisposableEffect(Unit) {
                onDispose {
                    try {
                        webViewInstance?.apply {
                            stopLoading()
                            onPause()
                            pauseTimers()
                            loadUrl("about:blank")
                            clearHistory()
                            clearCache(true)
                            removeAllViews()
                            (parent as? ViewGroup)?.removeView(this)
                            destroy()
                        }
                    } catch (_: Exception) {}
                    webViewInstance = null
                    System.gc()
                }
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        isFocusable = false
                        isFocusableInTouchMode = false
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            allowFileAccess = false
                            allowContentAccess = false
                            userAgentString = desktopUserAgent
                        }
                        setBackgroundColor(android.graphics.Color.BLACK)
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        setOnKeyListener { _, keyCode, event ->
                            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                                onClose()
                                true
                            } else {
                                false
                            }
                        }
                        loadDataWithBaseURL("https://elboom.cl", htmlContent, "text/html", "UTF-8", null)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(videoUrl))
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(exoPlayer) {
                exoPlayerInstance = exoPlayer
                onDispose {
                    exoPlayerInstance = null
                    exoPlayer.stop()
                    exoPlayer.release()
                    System.gc()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Indicador visual flotante de adelantar / retroceder (+10s / -10s)
        AnimatedVisibility(
            visible = feedbackText != null,
            enter = fadeIn(tween(80)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(1.2.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = feedbackText ?: "",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Barra superior con ÚNICO botón Volver
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val returnInteraction = remember { MutableInteractionSource() }
            val returnFocused by returnInteraction.collectIsFocusedAsState()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .focusRequester(returnFocusRequester)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (returnFocused) Color(0xFF00E5FF)
                        else Color.Black.copy(alpha = 0.65f)
                    )
                    .border(
                        width = if (returnFocused) 2.dp else 1.dp,
                        color = if (returnFocused) Color.White else Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(
                        interactionSource = returnInteraction,
                        indication = null
                    ) {
                        onClose()
                    }
                    .focusable(interactionSource = returnInteraction)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "← Volver a $title",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (returnFocused) Color.Black else Color.White
                )
            }
        }
    }
}

private fun extractYoutubeVideoId(url: String): String? {
    if (url.isBlank()) return null
    val pattern = ".*(?:youtu.be/|v/|u/\\w/|embed/|watch\\?v=|&v=)([^#&?]*).*".toRegex()
    val match = pattern.find(url)
    val id = match?.groups?.get(1)?.value
    return if (!id.isNullOrBlank() && id.length in 10..12) id else null
}

private fun Number.formatNumber(): String {
    return this.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
}

// =============================================================================
// ICONOS VECTORIALES ULTRA LIGEROS NATIVOS PARA TV (PIXEL PERFECT)
// =============================================================================

@Composable
private fun HomeCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.90f, h * 0.46f)
            lineTo(w * 0.80f, h * 0.46f)
            lineTo(w * 0.80f, h * 0.88f)
            lineTo(w * 0.20f, h * 0.88f)
            lineTo(w * 0.20f, h * 0.46f)
            lineTo(w * 0.10f, h * 0.46f)
            close()
        }
        drawPath(path = path, color = color, style = stroke)

        // Puerta
        val door = Path().apply {
            moveTo(w * 0.40f, h * 0.88f)
            lineTo(w * 0.40f, h * 0.60f)
            lineTo(w * 0.60f, h * 0.60f)
            lineTo(w * 0.60f, h * 0.88f)
        }
        drawPath(path = door, color = color, style = stroke)
    }
}

@Composable
private fun ClockCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round)
        drawCircle(color = color, radius = size.minDimension / 2f - stroke.width, style = stroke)
        val center = Offset(size.width / 2f, size.height / 2f)
        drawLine(color = color, start = center, end = Offset(center.x, center.y - size.height * 0.30f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = color, start = center, end = Offset(center.x + size.width * 0.22f, center.y), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun CarCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val body = Path().apply {
            moveTo(w * 0.20f, h * 0.35f)
            lineTo(w * 0.80f, h * 0.35f)
            lineTo(w * 0.92f, h * 0.55f)
            lineTo(w * 0.92f, h * 0.72f)
            lineTo(w * 0.08f, h * 0.72f)
            lineTo(w * 0.08f, h * 0.55f)
            close()
        }
        drawPath(path = body, color = color, style = stroke)

        // Ruedas
        drawCircle(color = color, radius = h * 0.12f, center = Offset(w * 0.28f, h * 0.75f), style = Fill)
        drawCircle(color = color, radius = h * 0.12f, center = Offset(w * 0.72f, h * 0.75f), style = Fill)
    }
}

@Composable
private fun MoonCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.70f, h * 0.15f)
            cubicTo(w * 0.35f, h * 0.18f, w * 0.20f, h * 0.50f, w * 0.35f, h * 0.82f)
            cubicTo(w * 0.45f, h * 0.88f, w * 0.60f, h * 0.90f, w * 0.75f, h * 0.85f)
            cubicTo(w * 0.50f, h * 0.75f, w * 0.45f, h * 0.38f, w * 0.70f, h * 0.15f)
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }
}

@Composable
private fun FlameCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            cubicTo(w * 0.55f, h * 0.30f, w * 0.85f, h * 0.48f, w * 0.85f, h * 0.68f)
            cubicTo(w * 0.85f, h * 0.88f, w * 0.68f, h * 0.95f, w * 0.50f, h * 0.95f)
            cubicTo(w * 0.32f, h * 0.95f, w * 0.15f, h * 0.88f, w * 0.15f, h * 0.68f)
            cubicTo(w * 0.15f, h * 0.52f, w * 0.35f, h * 0.38f, w * 0.45f, h * 0.30f)
            cubicTo(w * 0.45f, h * 0.42f, w * 0.55f, h * 0.48f, w * 0.60f, h * 0.45f)
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }
}

@Composable
private fun GearCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round)
        drawCircle(color = color, radius = size.minDimension * 0.28f, style = stroke)

        // Dientes
        val cX = size.width / 2f
        val cY = size.height / 2f
        val rOut = size.minDimension * 0.45f
        val rIn = size.minDimension * 0.32f

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val x1 = (cX + rIn * Math.cos(angle)).toFloat()
            val y1 = (cY + rIn * Math.sin(angle)).toFloat()
            val x2 = (cX + rOut * Math.cos(angle)).toFloat()
            val y2 = (cY + rOut * Math.sin(angle)).toFloat()
            drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = stroke.width)
        }
    }
}

@Composable
private fun WifiCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round)

        // Punto inferior
        drawCircle(color = color, radius = 2.2f * density, center = Offset(w * 0.50f, h * 0.85f), style = Fill)

        // Arco 1
        val arc1 = Path().apply {
            moveTo(w * 0.32f, h * 0.65f)
            cubicTo(w * 0.40f, h * 0.55f, w * 0.60f, h * 0.55f, w * 0.68f, h * 0.65f)
        }
        drawPath(path = arc1, color = color, style = stroke)

        // Arco 2
        val arc2 = Path().apply {
            moveTo(w * 0.16f, h * 0.45f)
            cubicTo(w * 0.32f, h * 0.30f, w * 0.68f, h * 0.30f, w * 0.84f, h * 0.45f)
        }
        drawPath(path = arc2, color = color, style = stroke)

        // Arco 3
        val arc3 = Path().apply {
            moveTo(w * 0.05f, h * 0.25f)
            cubicTo(w * 0.28f, h * 0.08f, w * 0.72f, h * 0.08f, w * 0.95f, h * 0.25f)
        }
        drawPath(path = arc3, color = color, style = stroke)
    }
}

@Composable
private fun WhatsAppCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height

        // Círculo con colita de bocadillo
        val bubble = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            cubicTo(w * 0.80f, h * 0.10f, w * 0.90f, h * 0.35f, w * 0.90f, h * 0.55f)
            cubicTo(w * 0.90f, h * 0.75f, w * 0.75f, h * 0.88f, w * 0.50f, h * 0.88f)
            lineTo(w * 0.25f, h * 0.94f)
            lineTo(w * 0.30f, h * 0.82f)
            cubicTo(w * 0.15f, h * 0.75f, w * 0.10f, h * 0.65f, w * 0.10f, h * 0.55f)
            cubicTo(w * 0.10f, h * 0.35f, w * 0.25f, h * 0.10f, w * 0.50f, h * 0.10f)
            close()
        }
        drawPath(path = bubble, color = color, style = stroke)

        // Auricular
        val phone = Path().apply {
            moveTo(w * 0.36f, h * 0.38f)
            cubicTo(w * 0.38f, h * 0.52f, w * 0.48f, h * 0.62f, w * 0.62f, h * 0.64f)
        }
        drawPath(path = phone, color = color, style = Stroke(width = 2.2f * density, cap = StrokeCap.Round))
    }
}

@Composable
private fun PersonCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height

        // Cabeza
        drawCircle(color = color, radius = h * 0.20f, center = Offset(w * 0.50f, h * 0.28f), style = stroke)

        // Cuerpo
        val body = Path().apply {
            moveTo(w * 0.15f, h * 0.88f)
            cubicTo(w * 0.20f, h * 0.58f, w * 0.80f, h * 0.58f, w * 0.85f, h * 0.88f)
        }
        drawPath(path = body, color = color, style = stroke)
    }
}

@Composable
private fun RulerCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height

        val ruler = Path().apply {
            moveTo(w * 0.15f, h * 0.20f)
            lineTo(w * 0.85f, h * 0.85f)
        }
        drawPath(path = ruler, color = color, style = stroke)

        // Marcas
        drawLine(color = color, start = Offset(w * 0.35f, h * 0.32f), end = Offset(w * 0.43f, h * 0.26f), strokeWidth = stroke.width)
        drawLine(color = color, start = Offset(w * 0.50f, h * 0.50f), end = Offset(w * 0.60f, h * 0.42f), strokeWidth = stroke.width)
        drawLine(color = color, start = Offset(w * 0.65f, h * 0.68f), end = Offset(w * 0.73f, h * 0.60f), strokeWidth = stroke.width)
    }
}

@Composable
private fun ShowerCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height

        // Tubo
        val pipe = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.35f)
            cubicTo(w * 0.15f, h * 0.20f, w * 0.55f, h * 0.20f, w * 0.55f, h * 0.35f)
        }
        drawPath(path = pipe, color = color, style = stroke)

        // Cabezal
        drawLine(color = color, start = Offset(w * 0.42f, h * 0.42f), end = Offset(w * 0.68f, h * 0.42f), strokeWidth = stroke.width * 1.5f)

        // Gotas
        drawLine(color = color, start = Offset(w * 0.48f, h * 0.52f), end = Offset(w * 0.46f, h * 0.62f), strokeWidth = stroke.width)
        drawLine(color = color, start = Offset(w * 0.55f, h * 0.52f), end = Offset(w * 0.55f, h * 0.65f), strokeWidth = stroke.width)
        drawLine(color = color, start = Offset(w * 0.62f, h * 0.52f), end = Offset(w * 0.64f, h * 0.62f), strokeWidth = stroke.width)
    }
}

@Composable
private fun PhotoCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height

        drawRoundRect(color = color, topLeft = Offset(w * 0.10f, h * 0.15f), size = Size(w * 0.80f, h * 0.70f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)

        // Sol / Círculo
        drawCircle(color = color, radius = h * 0.10f, center = Offset(w * 0.32f, h * 0.38f), style = stroke)

        // Montaña
        val mountain = Path().apply {
            moveTo(w * 0.15f, h * 0.75f)
            lineTo(w * 0.45f, h * 0.48f)
            lineTo(w * 0.60f, h * 0.62f)
            lineTo(w * 0.72f, h * 0.52f)
            lineTo(w * 0.85f, h * 0.75f)
        }
        drawPath(path = mountain, color = color, style = stroke)
    }
}

@Composable
private fun BedCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height

        // Respaldar y estructura
        val frame = Path().apply {
            moveTo(w * 0.10f, h * 0.30f)
            lineTo(w * 0.10f, h * 0.80f)
            moveTo(w * 0.10f, h * 0.65f)
            lineTo(w * 0.90f, h * 0.65f)
            lineTo(w * 0.90f, h * 0.80f)
        }
        drawPath(path = frame, color = color, style = stroke)

        // Almohada
        drawRoundRect(color = color, topLeft = Offset(w * 0.18f, h * 0.45f), size = Size(w * 0.24f, h * 0.16f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
    }
}

@Composable
private fun VideoCameraCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height

        // Cuerpo cámara
        drawRoundRect(color = color, topLeft = Offset(w * 0.10f, h * 0.25f), size = Size(w * 0.55f, h * 0.50f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)

        // Lente triangular
        val lens = Path().apply {
            moveTo(w * 0.65f, h * 0.38f)
            lineTo(w * 0.90f, h * 0.25f)
            lineTo(w * 0.90f, h * 0.75f)
            lineTo(w * 0.65f, h * 0.62f)
            close()
        }
        drawPath(path = lens, color = color, style = stroke)
    }
}

@Composable
private fun PlayCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.12f)
            lineTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.20f, h * 0.88f)
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }
}

@Composable
private fun CloseCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.15f, h * 0.15f), end = Offset(w * 0.85f, h * 0.85f), strokeWidth = stroke.width)
        drawLine(color = color, start = Offset(w * 0.85f, h * 0.15f), end = Offset(w * 0.15f, h * 0.85f), strokeWidth = stroke.width)
    }
}

@Composable
private fun ChevronRightCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.8f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.30f, h * 0.15f)
            lineTo(w * 0.70f, h * 0.50f)
            lineTo(w * 0.30f, h * 0.85f)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}
