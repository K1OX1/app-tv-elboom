package cl.elboomlicanray.tvlauncher.presentation.modals.stay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import cl.elboomlicanray.tvlauncher.presentation.home.ActivityDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    val initialActionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isVisible = true
        kotlinx.coroutines.delay(100)
        try {
            initialActionFocusRequester.requestFocus()
        } catch (_: Exception) {
            try {
                initialTabFocusRequester.requestFocus()
            } catch (_: Exception) {
                try {
                    closeFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(isPlayingVideoOverlay) {
        if (!isPlayingVideoOverlay) {
            kotlinx.coroutines.delay(100)
            try {
                initialActionFocusRequester.requestFocus()
            } catch (_: Exception) {
                try {
                    initialTabFocusRequester.requestFocus()
                } catch (_: Exception) {
                    try {
                        closeFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }
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
                if (isPlayingVideoOverlay) {
                    val youtubeId = dynamicCabin?.youtubeVideoId?.ifEmpty { "BoOdYfc7Hx8" } ?: "BoOdYfc7Hx8"
                    val videoUrl = dynamicCabin?.videoUrl?.takeIf { it.isNotBlank() }
                        ?: "https://www.youtube.com/watch?v=$youtubeId"
                    FullScreenVideoOverlay(
                        title = dynamicCabin?.name ?: unit.name,
                        videoUrl = videoUrl,
                        onClose = { isPlayingVideoOverlay = false }
                    )
                } else {
                // =========================================================================
                // 1. FONDO CINEMATOGRÁFICO FIJO DE ALTA GAMA (NO CAMBIA A FOTOS DE CAMAS)
                // =========================================================================
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(R.drawable.bg_stay_cinematic)
                        .size(1280, 720)
                        .crossfade(false)
                        .build(),
                    fallback = painterResource(R.drawable.bg_stay_cinematic),
                    error = painterResource(R.drawable.bg_stay_cinematic),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Scrim lateral izquierdo para bloque de textos, títulos y datos (idéntico a Guía Turística)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.00f to Color(0xFF081822),
                                0.40f to Color(0xFF081822).copy(alpha = 0.94f),
                                0.68f to Color(0xFF081822).copy(alpha = 0.65f),
                                1.00f to Color(0xFF081822).copy(alpha = 0.20f)
                            )
                        )
                )

                // Scrim superior para cabecera, filtros y botón cerrar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.65f),
                                0.5f to Color.Black.copy(alpha = 0.22f),
                                1.0f to Color.Transparent
                            )
                        )
                )

                // Scrim inferior para barra de datos prácticos
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.40f to Color.Black.copy(alpha = 0.45f),
                                1.0f to Color.Black.copy(alpha = 0.88f)
                            )
                        )
                )

                // =========================================================================
                // 3. CAPA PRINCIPAL DE INTERFAZ DE USUARIO (10-FOOT UI PARA TV)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 40.dp, end = 40.dp, top = 24.dp, bottom = 20.dp)
                ) {
                    // ---------------------------------------------------------------------
                    // BARRA SUPERIOR: TÍTULO, CÁPSULA DE NAVEGACIÓN Y BOTÓN CERRAR
                    // ---------------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Título y subtítulo superior izquierdo
                        Column(modifier = Modifier.weight(1f).padding(end = 20.dp)) {
                            Text(
                                text = "MI ALOJAMIENTO",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            val guestName = formatGuestDisplayName(stay.guestFullName, stay.guestFirstName)
                            val dateRange = formatFriendlyDateRange(stay.checkinDate, stay.checkoutDate)
                            val dateSuffix = if (dateRange.isNotBlank()) " · ($dateRange)" else ""
                            Text(
                                text = if (stay.isOccupied) "Estadía de $guestName · ${stay.nights} noche${if (stay.nights > 1) "s" else ""}$dateSuffix"
                                       else "Información y características de tu estadía",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(0xFFBAC4D0)
                            )
                        }

                        // Botón Cerrar
                        val closeInteraction = remember { MutableInteractionSource() }
                        val closeFocused by closeInteraction.collectIsFocusedAsState()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .focusRequester(closeFocusRequester)
                                .graphicsLayer {
                                    val scale = if (closeFocused) 1.08f else 1.0f
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(50))
                                .background(if (closeFocused) Color.White else Color.Black.copy(alpha = 0.50f))
                                .border(
                                    width = if (closeFocused) 2.dp else 1.dp,
                                    color = if (closeFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable(
                                    interactionSource = closeInteraction,
                                    indication = null
                                ) {
                                    onDismiss()
                                }
                                .focusable(interactionSource = closeInteraction)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            CloseCanvasIcon(
                                color = if (closeFocused) Color(0xFF030712) else Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Cerrar",
                                fontSize = 11.5.sp,
                                fontWeight = if (closeFocused) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (closeFocused) Color(0xFF030712) else Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                        // Cápsula de Navegación / Filtros horizontales
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.50f))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val navTabs = listOf(
                                "Reserva" to "RESERVA",
                                "Fotos" to "FOTOS (${realPhotoImages.size.coerceAtLeast(1)})",
                                "Camas" to "CAMAS",

                            )
                            navTabs.forEachIndexed { idx, (tabKey, tabLabel) ->
                                val isSelected = (tabKey == "Reserva" && selectedTab == "Reserva") ||
                                                 (tabKey == "Fotos" && selectedTab.startsWith("Fotos")) ||
                                                 (tabKey == "Camas" && selectedTab == "Camas") ||
                                                 (tabKey == "Video Tour" && selectedTab == "Video Tour")
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()

                                val focusScale by androidx.compose.animation.core.animateFloatAsState(if (isFocused) 1.035f else 1f, tween(150), label = "tab focus")
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .then(if (idx == 0) Modifier.focusRequester(initialTabFocusRequester) else Modifier)
                                        .graphicsLayer {
                                            val scale = focusScale
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            when {
                                                isFocused -> Color(0xFF00E5FF)
                                                isSelected -> Color.White.copy(alpha = 0.16f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = when {
                                                isFocused -> 2.dp
                                                isSelected -> 1.dp
                                                else -> 0.dp
                                            },
                                            color = when {
                                                isFocused -> Color.White
                                                isSelected -> Color(0xFF00E5FF).copy(alpha = 0.60f)
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(50)
                                        )
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            selectedTab = when (tabKey) {
                                                "Fotos" -> photoTabLabel
                                                "Camas" -> "Camas"
                                                "Video Tour" -> "Video Tour"
                                                else -> "Reserva"
                                            }
                                        }
                                        .focusable(interactionSource = interactionSource)
                                        .padding(horizontal = 18.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = tabLabel,
                                        fontSize = 12.sp,
                                        fontWeight = when {
                                            isFocused -> FontWeight.Black
                                            isSelected -> FontWeight.Bold
                                            else -> FontWeight.Medium
                                        },
                                        letterSpacing = 0.4.sp,
                                        color = when {
                                            isFocused -> Color(0xFF030712)
                                            isSelected -> Color.White
                                            else -> Color(0xFFCBD5E1)
                                        },
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                    Spacer(Modifier.height(8.dp))
                    // ---------------------------------------------------------------------
                    // CONTENIDO MEDIO: FICHA INFORMATIVA FLOTANTE O VISTAS SECUNDARIAS
                    // ---------------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when {
                            selectedTab == "Reserva" -> {
                                ReservationOverviewLayout(
                                    unit = unit,
                                    stay = stay,
                                    dynamicCabin = dynamicCabin,
                                    photoCount = realPhotoImages.size.coerceAtLeast(1),
                                    initialFocusRequester = initialActionFocusRequester,
                                    onNavigateToTab = { selectedTab = if (it == "Fotos") photoTabLabel else it },
                                    onPlayVideo = { isPlayingVideoOverlay = true }
                                )
                            }

                            selectedTab.startsWith("Fotos") -> {
                                FullPhotoGalleryView(
                                    images = realPhotoImages,
                                    selectedIndex = selectedImageIndex,
                                    cabinName = dynamicCabin?.name ?: unit.name,
                                    onSelectIndex = { selectedImageIndex = it },
                                    onBack = { selectedTab = "Reserva" }
                                )
                            }

                            selectedTab == "Camas" -> {
                                BedroomDistributionView(
                                    dynamicCabin = dynamicCabin,
                                    unit = unit
                                )
                            }

                            selectedTab == "Video Tour" -> {
                                VideoTourPanelView(
                                    dynamicCabin = dynamicCabin,
                                    unit = unit,
                                    onPlayFullscreen = { isPlayingVideoOverlay = true }
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

/**
 * Vista de resumen de la estadía y cabaña (Pestaña "RESERVA").
 * Reproduce fielmente la composición cinematográfica y flotante de Guía Turística:
 * - Ficha flotante sobre el paisaje (Categoría, Título, Descripción boutique, Métricas y Botones de acción).
 * - Fila inferior refinada de datos prácticos (Check-in, Check-out, Silencio y Cuenta) con divisores finos.
 * - Tarjeta secundaria interactiva con QR de conexión rápida Wi-Fi Starlink.
 */
@Composable
private fun ReservationOverviewLayout(
    unit: AccommodationUnit,
    stay: GuestStayInfo,
    dynamicCabin: DynamicCabinDetails?,
    photoCount: Int,
    initialFocusRequester: FocusRequester,
    onNavigateToTab: (String) -> Unit,
    onPlayVideo: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // =========================================================================
        // BLOQUE MEDIO-ALTO: FICHA FLOTANTE SOBRE EL PAISAJE (ESTILO GUÍA TURÍSTICA)
        // =========================================================================
        Column(
            modifier = Modifier.widthIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Badges de Categoría y Tinaja en cápsulas refinadas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x3300E5FF))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.85f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 3.5.dp)
                ) {
                    val cabinType = dynamicCabin?.type?.ifBlank { unit.type } ?: unit.type
                    Text(
                        text = "${cabinType.uppercase()} · ${unit.name.uppercase()}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFF00E5FF)
                    )
                }

                if (unit.hasTinaja) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x33F97316))
                            .border(1.dp, Color(0xFFF97316).copy(alpha = 0.85f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.5.dp)
                    ) {
                        Text(
                            text = "🪵 TINAJA DE MADERA NATIVA",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFFB923C)
                        )
                    }
                }
            }

            // 2. Nombre del Alojamiento en tipografía destacada
            val cabinTitle = dynamicCabin?.name?.ifBlank { unit.name } ?: unit.name
            Text(
                text = cabinTitle,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 42.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 3. Descripción cálida boutique
            val cabinDesc = dynamicCabin?.description?.ifBlank {
                "Cabaña boutique totalmente equipada en Licán Ray, inmersa en la tranquilidad del bosque nativo con estufa Bosca a leña, terraza techada y tinaja privada de agua caliente."
            } ?: "Cabaña boutique totalmente equipada en Licán Ray, inmersa en la tranquilidad del bosque nativo con estufa Bosca a leña, terraza techada y tinaja privada de agua caliente."
            Text(
                text = cabinDesc,
                fontSize = 13.sp,
                color = Color(0xFFE2E8F0),
                lineHeight = 18.5.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // 4. Métricas clave en fila unificada con divisores finos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                val capacity = dynamicCabin?.capacity ?: unit.capacity
                val area = dynamicCabin?.area ?: unit.area
                val bathrooms = dynamicCabin?.bathrooms ?: unit.bathrooms

                // Capacidad
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PersonCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                    Text(text = "$capacity pers", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.White.copy(alpha = 0.35f)))

                // Superficie
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RulerCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                    Text(text = "$area m²", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.White.copy(alpha = 0.35f)))

                // Baños
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShowerCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                    Text(text = "$bathrooms baño${if (bathrooms > 1) "s" else ""}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.White.copy(alpha = 0.35f)))

                // Estacionamiento
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CarCanvasIcon(color = Color.White.copy(alpha = 0.90f), modifier = Modifier.size(13.dp))
                    val plate = stay.vehiclePlate.trim()
                    val parkingLabel = when {
                        plate.isBlank() || plate.contains("Estacionamiento", true) || plate.length > 14 -> "Estacionamiento techado"
                        else -> "Patente: $plate"
                    }
                    Text(text = parkingLabel, fontSize = 12.sp, color = Color.White.copy(alpha = 0.90f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Botones de acción principales en fila unificada (exacto a Guía Turística)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // A. Ver Video Tour
                val videoInteraction = remember { MutableInteractionSource() }
                val videoFocused by videoInteraction.collectIsFocusedAsState()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .focusRequester(initialFocusRequester)
                        .graphicsLayer {
                            val scale = if (videoFocused) 1.04f else 1.0f
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(50))
                        .background(if (videoFocused) Color(0xFF287D77) else Color(0xFF195550))
                        .border(
                            width = if (videoFocused) 2.5.dp else 1.dp,
                            color = if (videoFocused) Color.White else Color.White.copy(alpha = 0.40f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(interactionSource = videoInteraction, indication = null) {
                            onPlayVideo()
                        }
                        .focusable(interactionSource = videoInteraction)
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    PlayCanvasIcon(color = Color.White, modifier = Modifier.size(11.dp))
                    Text(
                        text = "Ver recorrido",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // B. WhatsApp Recepción
                val waInteraction = remember { MutableInteractionSource() }
                val waFocused by waInteraction.collectIsFocusedAsState()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = if (waFocused) 1.08f else 1.0f
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(50))
                        .background(if (waFocused) Color(0xFF25D366) else Color(0xCC052E16))
                        .border(
                            width = if (waFocused) 2.5.dp else 1.2.dp,
                            color = if (waFocused) Color.White else Color(0xFF25D366).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(interactionSource = waInteraction, indication = null) {
                            Toast.makeText(context, "Recepción El Boom: ${stay.hostWhatsappFormatted}", Toast.LENGTH_LONG).show()
                        }
                        .focusable(interactionSource = waInteraction)
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    WhatsAppCanvasIcon(
                        color = if (waFocused) Color(0xFF030712) else Color(0xFF25D366),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Contactar recepción",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (waFocused) Color(0xFF030712) else Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }


            }
        }

        // =========================================================================
        // FRANJA INFERIOR: DATOS PRÁCTICOS + TARJETA ACCIÓN QR WI-FI (52dp)
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Fila de Horario Check-in, Check-out, Silencio y Cuenta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD90A0F1D))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Check-in
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ClockCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = "CHECK-IN",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF94A3B8)
                        )
                        val checkinDateFormatted = formatFriendlyDate(stay.checkinDate)
                        val checkinTimeStr = if (checkinDateFormatted.isNotBlank()) "$checkinDateFormatted · ${stay.checkinOfficialTime}" else "Desde 15:00 hrs"
                        Text(
                            text = checkinTimeStr,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Divisor fino
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.20f)))

                // Check-out
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ClockCanvasIcon(color = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = if (stay.isCheckoutDay) "SALIDA HOY" else "CHECK-OUT",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF94A3B8)
                        )
                        val checkoutDateFormatted = formatFriendlyDate(stay.checkoutDate)
                        val checkoutTimeStr = if (checkoutDateFormatted.isNotBlank()) "$checkoutDateFormatted · ${stay.checkoutOfficialTime}" else "Hasta 11:00 hrs"
                        Text(
                            text = checkoutTimeStr,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stay.isCheckoutDay) Color(0xFFFFD580) else Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Divisor fino
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.20f)))

                // Silencio nocturno
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    MoonCanvasIcon(color = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = "SILENCIO NOCTURNO",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "23:00 a 11:00 hrs",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (stay.isOccupied) {
                // Divisor fino
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.20f)))

                // Estado de Cuenta
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val isPaidClean = stay.isPaid || stay.balance <= 0
                    val iconColor = if (isPaidClean) Color(0xFF34D399) else Color(0xFFFBBF24)
                    CheckCanvasIcon(color = iconColor, modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = if (isPaidClean) "ESTADO RESERVA" else "SALDO PENDIENTE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = if (isPaidClean) "Cuenta al Día · Pagado" else "$${stay.balance.formatNumber()} CLP",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPaidClean) Color(0xFF6EE7B7) else Color(0xFFFBBF24),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                }
            }

            // 2. Tarjeta QR Wi-Fi Starlink (Misma altura 52dp, interactiva con foco)
            val qrInteraction = remember { MutableInteractionSource() }
            val qrFocused by qrInteraction.collectIsFocusedAsState()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .height(52.dp)
                    .graphicsLayer {
                        val scale = if (qrFocused) 1.05f else 1.0f
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (qrFocused) Color(0xEE132338) else Color(0xD90A0F1D))
                    .border(
                        width = if (qrFocused) 2.dp else 1.dp,
                        color = if (qrFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(
                        interactionSource = qrInteraction,
                        indication = null
                    ) {
                        Toast.makeText(context, "Red: ${stay.wifiNetwork} | Clave: ${stay.wifiPassword}", Toast.LENGTH_LONG).show()
                    }
                    .focusable(interactionSource = qrInteraction)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(2.dp)
                ) {
                    QrCodeView(
                        content = "WIFI:T:WPA;S:${stay.wifiNetwork};P:${stay.wifiPassword};;",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column {
                    Text(
                        text = "Wi-Fi Starlink Satelital",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                        color = if (qrFocused) Color(0xFF00E5FF) else Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Red: ${stay.wifiNetwork}",
                            fontSize = 8.5.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(text = "·", color = Color(0xFF64748B))
                        Text(
                            text = "Clave: ${stay.wifiPassword}",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }
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
    val youtubeUrl = dynamicCabin?.videoUrl?.takeIf { it.isNotBlank() }
        ?: "https://www.youtube.com/watch?v=$youtubeId"
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
 * Puente JavaScript para notificar de forma asíncrona a Android
 * cuando finaliza un video en la sección Mi Estadía.
 */
private class VideoEndBridgeStay(private val ready: () -> Unit, private val error: () -> Unit, private val onEnd: () -> Unit) {
    @JavascriptInterface fun started() { Handler(Looper.getMainLooper()).post { ready() } }
    @JavascriptInterface fun failed() { Handler(Looper.getMainLooper()).post { error() } }

    @JavascriptInterface
    fun onVideoEnded() {
        Handler(Looper.getMainLooper()).post {
            onEnd()
        }
    }
}

/**
 * Overlay de reproducción de video a pantalla completa.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun FullScreenVideoOverlay(title: String, videoUrl: String, onClose: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val events = cl.elboomlicanray.tvlauncher.presentation.home.MainActivityEvents
        events.videoOpen = true
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) onClose()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            events.videoOpen = false
            events.notifyUserInteraction()
        }
    }
    var attempt by remember(videoUrl) { mutableIntStateOf(0) }
    key(videoUrl, attempt) {
        var started by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }
        LaunchedEffect(started) {
            if (!started) { kotlinx.coroutines.delay(45_000); failed = true }
        }
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (!failed) {
                VideoSession(title, videoUrl, onClose, { started = true }, { failed = true })
                if (!started) Text("$title · Abriendo video…", color = Color.White, fontSize = 18.sp)
            } else {
                val retryFocus = remember { FocusRequester() }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No se pudo reproducir $title", color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        androidx.tv.material3.Button(onClick = { attempt++ }, modifier = Modifier.focusRequester(retryFocus)) { Text("Reintentar") }
                        androidx.tv.material3.Button(onClick = onClose) { Text("Volver") }
                    }
                }
                LaunchedEffect(Unit) { retryFocus.requestFocus() }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSession(title: String, videoUrl: String, onClose: () -> Unit, onStarted: () -> Unit, onFailure: () -> Unit) {
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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    exoPlayerInstance?.pause()
                    webViewInstance?.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null)
                    webViewInstance?.onPause()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    webViewInstance?.onResume()
                    webViewInstance?.evaluateJavascript("if (player && player.playVideo) player.playVideo();", null)
                    exoPlayerInstance?.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var feedbackAction by remember { mutableStateOf<String?>(null) }
    var feedbackTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(feedbackTrigger) {
        if (feedbackTrigger > 0L) {
            kotlinx.coroutines.delay(650)
            feedbackAction = null
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
                                exoPlayerInstance?.let { p ->
                                    val dur = if (p.duration > 0L) p.duration else Long.MAX_VALUE
                                    p.seekTo(minOf(maxOf(0L, dur - 1000L), p.currentPosition + 10_000L))
                                }
                            }
                            feedbackAction = "Adelantar +10s ⏩"
                            feedbackTrigger++
                            true
                        }
                        Key.DirectionLeft -> {
                            if (youtubeVideoId != null) {
                                webViewInstance?.evaluateJavascript("seekRelative(-10);", null)
                            } else {
                                exoPlayerInstance?.let { p -> p.seekTo(maxOf(0L, p.currentPosition - 10_000L)) }
                            }
                            feedbackAction = "Retroceder -10s ⏪"
                            feedbackTrigger++
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            // Consumir flechas hacia arriba o hacia abajo para aislar totalmente la TV
                            true
                        }
                        else -> false
                    }
                } else {
                    when (keyEvent.key) {
                        Key.Back, Key.Escape,
                        Key.DirectionRight, Key.DirectionLeft,
                        Key.DirectionUp, Key.DirectionDown -> true
                        else -> false
                    }
                }
            }
    ) {
        if (youtubeVideoId != null) {
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                        #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; }
                        .ytp-chrome-top, .ytp-watermark, .ytp-youtube-button, .ytp-show-cards-title,
                        .ytp-pause-overlay, .ytp-ad-overlay-container, .ytp-ad-message-container,
                        .video-ads, .ytp-ad-module, .ytp-ce-element { display: none !important; }
                    </style>
                </head>
                <body>
                    <div id="player"></div>
                    <script>
                        var tag = document.createElement('script');
                        tag.src = "https://www.youtube.com/iframe_api";
                        var firstScriptTag = document.getElementsByTagName('script')[0];
                        firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                        var player;
                        function onYouTubeIframeAPIReady() {
                            player = new YT.Player('player', {
                                height: '100%',
                                width: '100%',
                                videoId: '$youtubeVideoId',
                                playerVars: {
                                    'autoplay': 1,
                                    'controls': 0,
                                    'rel': 0,
                                    'showinfo': 0,
                                    'iv_load_policy': 3,
                                    'modestbranding': 1,
                                    'playsinline': 1,
                                    'fs': 0,
                                    'disablekb': 1,
                                    'origin': 'https://elboom.cl'
                                },
                                events: {
                                    'onReady': function(e) {
                                        try {
                                            e.target.unMute();
                                            e.target.setVolume(100);
                                            e.target.playVideo();
                                        } catch(err) {}
                                    },
                                    'onError': function() { window.AndroidHost.failed(); },
                                    'onStateChange': function(e) {
                                        if (e.data === 1) window.AndroidHost.started();
                                        try {
                                            if (e.data === 0) {
                                                if (window.AndroidHost && typeof window.AndroidHost.onVideoEnded === 'function') {
                                                    window.AndroidHost.onVideoEnded();
                                                }
                                            }
                                        } catch(err) {}
                                    }
                                }
                            });
                        }

                        function seekRelative(sec) {
                            try {
                                if (player && typeof player.getCurrentTime === 'function') {
                                    var curr = player.getCurrentTime();
                                    var dur = (typeof player.getDuration === 'function') ? player.getDuration() : 999999;
                                    var target = Math.max(0, Math.min(dur > 2 ? dur - 1 : dur, curr + sec));
                                    player.seekTo(target, true);
                                }
                            } catch(err) {}
                        }
                    </script>
                </body>
                </html>
            """.trimIndent()

            DisposableEffect(Unit) {
                onDispose {
                    try {
                        webViewInstance?.apply {
                            removeJavascriptInterface("AndroidHost")
                            stopLoading()
                            onPause()
                            resumeTimers()
                            loadUrl("about:blank")
                            clearHistory()
                            removeAllViews()
                            (parent as? ViewGroup)?.removeView(this)
                            destroy()
                        }
                    } catch (_: Exception) {}
                    webViewInstance = null
                }
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        resumeTimers()
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
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        }
                        setBackgroundColor(android.graphics.Color.BLACK)
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return false
                                val host = uri.host ?: ""
                                if (host.contains("youtube.com") ||
                                    host.contains("googlevideo.com") ||
                                    host.contains("ytimg.com") ||
                                    host.contains("youtube-nocookie.com") ||
                                    uri.toString() == "about:blank") {
                                    return false
                                }
                                return true
                            }
                        }
                        addJavascriptInterface(VideoEndBridgeStay(onStarted, onFailure, onClose), "AndroidHost")
                        setOnKeyListener { _, keyCode, event ->
                            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                                onClose()
                                true
                            } else {
                                false
                            }
                        }
                        loadDataWithBaseURL("https://elboom.cl/", htmlContent, "text/html", "UTF-8", null)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    // Mantener activo
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val exoPlayer = remember(videoUrl) {
                cl.elboomlicanray.tvlauncher.presentation.playback.createTvPlayer(context).apply {
                    setMediaItem(MediaItem.fromUri(videoUrl))
                    addListener(object : Player.Listener {
                        override fun onRenderedFirstFrame() = onStarted()
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) = onFailure()
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                onClose()
                            }
                        }
                    })
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(exoPlayer) {
                exoPlayerInstance = exoPlayer
                onDispose {
                    exoPlayerInstance = null
                    try {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        exoPlayer.release()
                    } catch (_: Exception) {}
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

        // Indicador minimalista de avance/retroceso (discreto en el tercio inferior, nunca tapa el centro)
        AnimatedVisibility(
            visible = feedbackAction != null,
            enter = fadeIn(tween(60)) + scaleIn(initialScale = 0.90f),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text(
                    text = feedbackAction ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Barra superior con botón minimalista "← Volver" (vidrio translúcido elegante, sin tapar el video)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val returnInteraction = remember { MutableInteractionSource() }
            val returnFocused by returnInteraction.collectIsFocusedAsState()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .focusRequester(returnFocusRequester)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (returnFocused) Color.White.copy(alpha = 0.22f)
                        else Color.Black.copy(alpha = 0.45f)
                    )
                    .border(
                        width = if (returnFocused) 1.5.dp else 1.dp,
                        color = if (returnFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(
                        interactionSource = returnInteraction,
                        indication = null
                    ) {
                        onClose()
                    }
                    .focusable(interactionSource = returnInteraction)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "← Volver",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
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
    return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}

private fun Number.formatNumber(): String {
    return this.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
}

private fun formatGuestDisplayName(fullName: String, firstName: String): String {
    if (firstName.isNotBlank()) return firstName
    if (fullName.isBlank()) return "Huésped registrado"
    val parts = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0]} ${parts[1]}"
        parts.isNotEmpty() -> parts[0]
        else -> "Huésped registrado"
    }
}

private fun formatFriendlyDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    try {
        val parts = if (dateStr.contains("-")) dateStr.split("-") else dateStr.split("/")
        if (parts.size == 3) {
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            val months = listOf("", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
            val m = months.getOrElse(month) { "" }
            return "$day $m"
        }
    } catch (_: Exception) {}
    return dateStr
}

private fun formatFriendlyDateRange(start: String, end: String): String {
    if (start.isBlank()) return ""
    try {
        val sParts = if (start.contains("-")) start.split("-") else start.split("/")
        val eParts = if (end.contains("-")) end.split("-") else end.split("/")
        if (sParts.size == 3 && eParts.size == 3) {
            val (sY, sM, sD) = if (sParts[0].length == 4) Triple(sParts[0].toInt(), sParts[1].toInt(), sParts[2].toInt()) else Triple(sParts[2].toInt(), sParts[1].toInt(), sParts[0].toInt())
            val (eY, eM, eD) = if (eParts[0].length == 4) Triple(eParts[0].toInt(), eParts[1].toInt(), eParts[2].toInt()) else Triple(eParts[2].toInt(), eParts[1].toInt(), eParts[0].toInt())
            val monthsFull = listOf("", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
            return if (sM == eM) {
                "$sD al $eD de ${monthsFull.getOrElse(sM) { "" }}"
            } else {
                val monthsShort = listOf("", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                "$sD ${monthsShort.getOrElse(sM) { "" }} al $eD ${monthsShort.getOrElse(eM) { "" }}"
            }
        }
    } catch (_: Exception) {}
    return if (start.isNotBlank() && end.isNotBlank()) "$start al $end" else start
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

@Composable
private fun CheckCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.8f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.76f)
            lineTo(w * 0.82f, h * 0.24f)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

private data class CabinSpaceItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val isVideo: Boolean = false,
    val isBeds: Boolean = false
)
