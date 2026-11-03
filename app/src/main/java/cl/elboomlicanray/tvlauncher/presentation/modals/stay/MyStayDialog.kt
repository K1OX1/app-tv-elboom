package cl.elboomlicanray.tvlauncher.presentation.modals.stay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog

/**
 * Modelo de categoría para el dock inferior de "Mi Alojamiento".
 */
private data class StayCategoryTab(
    val id: String,
    val title: String,
    val icon: @Composable (Color) -> Unit
)

/**
 * Diálogo Modal Rediseñado: "Mi Alojamiento" para Smart TV (Alojamiento Boutique El Boom).
 *
 * Arquitectura visual:
 * - Fotografía panorámica a pantalla completa con crossfade suave y degradado azul marino oscuro para contraste.
 * - Información integrada a la izquierda sobre el paisaje, limpia y sin paneles opacos pesados.
 * - Cabecera superior con título de la cabaña, subtítulo con datos de huésped/fechas y botón Cerrar.
 * - Franja horizontal inferior navegable con el D-Pad del control remoto, con borde turquesa de foco (#00E5FF).
 * - 5 categorías esenciales:
 *   1. Detalles (Capacidad, m², baños, check-in, check-out y saldo)
 *   2. Wi-Fi & Recepción (QR Starlink grande, red, clave y WhatsApp)
 *   3. Comodidades (Estacionamiento techado/patente, tinaja y leña Bosca)
 *   4. Camas (Distribución de dormitorios y ropa de cama)
 *   5. Fotos & Tour (Galería HD navegable y acceso a Video Tour a pantalla completa)
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyStayDialog(
    unit: AccommodationUnit,
    stay: GuestStayInfo,
    dynamicCabin: DynamicCabinDetails? = null,
    propertyInfo: DynamicPropertyInfo = DynamicPropertyInfo(),
    selectedLanguage: String = "ES",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = remember(selectedLanguage) { AppStringsCatalog.get(selectedLanguage) }

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

    val realPhotoImages = remember(allImages) {
        val clean = allImages.filter { !isMarketingCollage(it) }
        if (clean.isNotEmpty()) clean else allImages
    }

    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var isPlayingVideoOverlay by remember { mutableStateOf(false) }

    val categories = remember(strings, allImages.size) {
        listOf(
            StayCategoryTab("detalles", "Detalles") { col -> HomeCanvasIcon(color = col, modifier = Modifier.size(16.dp)) },
            StayCategoryTab("wifi", "Wi-Fi & Contacto") { col -> WifiCanvasIcon(color = col, modifier = Modifier.size(16.dp)) },
            StayCategoryTab("comodidades", "Comodidades") { col -> FlameCanvasIcon(color = col, modifier = Modifier.size(16.dp)) },
            StayCategoryTab("camas", "Camas") { col -> BedCanvasIcon(color = col, modifier = Modifier.size(16.dp)) },
            StayCategoryTab("fotos", "Fotos & Tour (${allImages.size.coerceAtLeast(1)})") { col -> PhotoCanvasIcon(color = col, modifier = Modifier.size(16.dp)) }
        )
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val closeFocusRequester = remember { FocusRequester() }
    val categoryFocusRequesters = remember { List(categories.size) { FocusRequester() } }

    LaunchedEffect(Unit) {
        isVisible = true
        try {
            categoryFocusRequesters.first().requestFocus()
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
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        BackHandler {
            if (isPlayingVideoOverlay) {
                isPlayingVideoOverlay = false
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
                // 1. FOTOGRAFÍA PANORÁMICA DE FONDO SEGÚN CATEGORÍA SELECCIONADA
                // =========================================================================
                val activeBgModel: Any = remember(selectedCategoryIndex, realPhotoImages, selectedImageIndex) {
                    when (selectedCategoryIndex) {
                        0 -> realPhotoImages.firstOrNull { it.contains("1Xbjwgzh") || it.contains("5.jpg") }
                            ?: realPhotoImages.firstOrNull() ?: R.drawable.bg_stay_cinematic
                        1 -> R.drawable.bg_licanray_sunset
                        2 -> R.drawable.bg_info_tinajas
                        3 -> realPhotoImages.firstOrNull { it.contains("14xv4LvW") || it.contains("12.jpg") || it.contains("13.jpg") }
                            ?: realPhotoImages.getOrNull(1) ?: R.drawable.bg_stay_cinematic
                        4 -> realPhotoImages.getOrNull(selectedImageIndex) ?: realPhotoImages.firstOrNull() ?: R.drawable.bg_stay_cinematic
                        else -> R.drawable.bg_stay_cinematic
                    }
                }

                Crossfade(
                    targetState = activeBgModel,
                    animationSpec = tween(durationMillis = 260),
                    label = "StayBackgroundCrossfade"
                ) { bgModel ->
                    AsyncImage(
                        model = bgModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // =========================================================================
                // 2. DEGRADADOS OSCUROS DE CONTRASTE Y LEGIBILIDAD
                // =========================================================================
                // Scrim lateral izquierdo para bloque de textos y datos
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.00f to Color(0xF4030712),
                                0.38f to Color(0xDD030712),
                                0.60f to Color(0x77030712),
                                0.88f to Color.Transparent
                            )
                        )
                )

                // Scrim superior para cabecera y botón cerrar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0xCC000000),
                                0.6f to Color(0x66000000),
                                1.0f to Color.Transparent
                            )
                        )
                )

                // Scrim inferior para la franja horizontal de categorías
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.40f to Color(0x88030712),
                                1.0f to Color(0xFA030712)
                            )
                        )
                )

                // =========================================================================
                // 3. CAPA DE INTERFAZ PRINCIPAL (10-FOOT UI PARA ANDROID TV)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 46.dp, vertical = 22.dp)
                ) {
                    // ---------------------------------------------------------------------
                    // BARRA SUPERIOR: NOMBRE DE CABAÑA, SUBTÍTULO Y BOTÓN CERRAR
                    // ---------------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val cabinTitle = dynamicCabin?.name?.ifBlank { unit.name } ?: unit.name
                            Text(
                                text = cabinTitle,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
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
                                color = Color.White.copy(alpha = 0.70f)
                            )
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
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            CloseCanvasIcon(
                                color = if (closeFocused) Color(0xFF00E5FF) else Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = strings.close,
                                fontSize = 12.sp,
                                fontWeight = if (closeFocused) FontWeight.Bold else FontWeight.Normal,
                                color = if (closeFocused) Color(0xFF00E5FF) else Color.White
                            )
                        }
                    }

                    // ---------------------------------------------------------------------
                    // CONTENIDO DINÁMICO SEGÚN LA CATEGORÍA ACTIVA (LADO IZQUIERDO)
                    // ---------------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 8.dp, bottom = 6.dp)
                    ) {
                        Crossfade(
                            targetState = selectedCategoryIndex,
                            animationSpec = tween(durationMillis = 200),
                            label = "StayCategoryCrossfade"
                        ) { catIndex ->
                            when (catIndex) {
                                0 -> StayDetailsSection(unit, stay, dynamicCabin, propertyInfo)
                                1 -> StayWifiSection(stay)
                                2 -> StayAmenitiesSection(unit, stay, dynamicCabin)
                                3 -> StayBedsSection(unit, dynamicCabin)
                                4 -> StayPhotosAndTourSection(
                                    images = realPhotoImages,
                                    selectedIndex = selectedImageIndex,
                                    cabinName = dynamicCabin?.name ?: unit.name,
                                    dynamicCabin = dynamicCabin,
                                    unit = unit,
                                    onSelectIndex = { selectedImageIndex = it },
                                    onPlayFullscreen = { isPlayingVideoOverlay = true }
                                )
                            }
                        }
                    }

                    // ---------------------------------------------------------------------
                    // FRANJA HORIZONTAL INFERIOR DE CATEGORÍAS (RECORRIBLE CON CONTROL REMOTO)
                    // ---------------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        categories.forEachIndexed { index, category ->
                            val isSelected = index == selectedCategoryIndex
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isFocused by itemInteraction.collectIsFocusedAsState()

                            LaunchedEffect(isFocused) {
                                if (isFocused) {
                                    selectedCategoryIndex = index
                                }
                            }

                            val tabThumbUrl = remember(index, realPhotoImages) {
                                when (index) {
                                    0 -> realPhotoImages.firstOrNull { it.contains("1Xbjwgzh") || it.contains("5.jpg") } ?: realPhotoImages.firstOrNull() ?: ""
                                    1 -> "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=400&q=80"
                                    2 -> "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=400&q=80"
                                    3 -> realPhotoImages.firstOrNull { it.contains("14xv4LvW") || it.contains("12.jpg") || it.contains("13.jpg") } ?: realPhotoImages.getOrNull(1) ?: ""
                                    4 -> realPhotoImages.firstOrNull { it.contains("mDPx9WSB") || it.contains("17.jpg") } ?: realPhotoImages.getOrNull(2) ?: ""
                                    else -> ""
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(148.dp)
                                    .graphicsLayer {
                                        val scale = if (isFocused) 1.05f else 1.0f
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clickable(
                                        interactionSource = itemInteraction,
                                        indication = null
                                    ) {
                                        selectedCategoryIndex = index
                                    }
                                    .focusRequester(categoryFocusRequesters[index])
                                    .focusable(interactionSource = itemInteraction)
                            ) {
                                // Miniatura con foco turquesa nítido
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(
                                            width = when {
                                                isFocused -> 2.2.dp
                                                isSelected -> 1.5.dp
                                                else -> 0.8.dp
                                            },
                                            color = when {
                                                isFocused -> Color(0xFF00E5FF)
                                                isSelected -> Color(0xFF00E5FF)
                                                else -> Color.White.copy(alpha = 0.20f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    if (tabThumbUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = tabThumbUrl,
                                            contentDescription = category.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.bg_stay_cinematic),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Degradado para que el ícono resalte
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.0f to Color.Black.copy(alpha = 0.25f),
                                                    1.0f to Color.Black.copy(alpha = 0.70f)
                                                )
                                            )
                                    )

                                    // Ícono representativo en la esquina inferior derecha
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isFocused || isSelected) Color(0xFF00E5FF) else Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        category.icon(if (isFocused || isSelected) Color.Black else Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Título de la categoría
                                Text(
                                    text = category.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isFocused -> Color(0xFF00E5FF)
                                        isSelected -> Color.White
                                        else -> Color.White.copy(alpha = 0.65f)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // =========================================================================
                // 4. OVERLAY DE VIDEO A PANTALLA COMPLETA
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

// =============================================================================
// SECCIÓN 1: DETALLES DE ALOJAMIENTO Y RESERVA
// =============================================================================
@Composable
private fun StayDetailsSection(
    unit: AccommodationUnit,
    stay: GuestStayInfo,
    dynamicCabin: DynamicCabinDetails?,
    propertyInfo: DynamicPropertyInfo
) {
    val capacity = dynamicCabin?.capacity ?: unit.capacity
    val area = dynamicCabin?.area ?: unit.area
    val bathrooms = dynamicCabin?.bathrooms ?: unit.bathrooms
    val cabinType = dynamicCabin?.type?.ifBlank { unit.type } ?: unit.type

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Detalles y Registro del Alojamiento",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )

        // Tarjeta ligera con métricas clave de la cabaña
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC07101E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = cabinType,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PersonCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                        Text(text = "$capacity personas", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                    Text(text = "·", color = Color.White.copy(alpha = 0.35f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RulerCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                        Text(text = "$area m²", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                    Text(text = "·", color = Color.White.copy(alpha = 0.35f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ShowerCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                        Text(text = "$bathrooms baño${if (bathrooms > 1) "s" else ""}", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            // Badge de estado de cuenta
            if (stay.isOccupied) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (stay.isPaid) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFF59E0B).copy(alpha = 0.18f))
                        .border(1.dp, if (stay.isPaid) Color(0xFF10B981) else Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (stay.isPaid) "✓ PAGADO AL DÍA" else "SALDO: $${stay.balance.formatNumber()} CLP",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stay.isPaid) Color(0xFF34D399) else Color(0xFFFBBF24)
                    )
                }
            }
        }

        // Horarios oficiales de Check-in y Check-out
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Check-in
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC07101E))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Text(
                        text = if (stay.isOccupied) "CHECK-IN" else "HORARIO CHECK-IN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val checkinVal = if (stay.isOccupied && stay.checkinDate.isNotBlank()) stay.checkinDate else propertyInfo.checkIn.ifBlank { "15:00 a 22:00 hrs" }
                Text(
                    text = checkinVal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (stay.isOccupied) "Hora oficial: ${stay.checkinOfficialTime}" else "Recepción y entrega de llaves",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Check-out
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC07101E))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Text(
                        text = if (stay.isOccupied) "CHECK-OUT" else "HORARIO CHECK-OUT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val checkoutVal = if (stay.isOccupied && stay.checkoutDate.isNotBlank()) stay.checkoutDate else propertyInfo.checkOut.ifBlank { "09:00 a 11:00 hrs" }
                Text(
                    text = checkoutVal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (stay.isOccupied) "Hora oficial: ${stay.checkoutOfficialTime}" else "Entrega de llaves para aseo",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Descripción de bienvenida de la unidad
        val cabinDesc = dynamicCabin?.description?.ifEmpty {
            "Alojamiento privado totalmente equipado en Licán Ray, rodeado de naturaleza nativa, calefacción a leña y máxima comodidad."
        } ?: ""
        Text(
            text = cabinDesc,
            fontSize = 11.5.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 16.5.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =============================================================================
// SECCIÓN 2: WI-FI STARLINK & CONTACTO DE RECEPCIÓN
// =============================================================================
@Composable
private fun StayWifiSection(stay: GuestStayInfo) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 760.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tarjeta Wi-Fi con Código QR
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xCC07101E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WifiCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                Text(
                    text = "Internet Satelital Starlink",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            }

            Box(
                modifier = Modifier
                    .size(118.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(6.dp)
            ) {
                QrCodeView(
                    content = "WIFI:T:WPA;S:${stay.wifiNetwork};P:${stay.wifiPassword};;",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Escanea el código QR con la cámara de tu celular",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Red: ${stay.wifiNetwork}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Clave: ${stay.wifiPassword}",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            }
        }

        // Contacto Recepción WhatsApp
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xCC07101E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WhatsAppCanvasIcon(color = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                    Text(
                        text = "Recepción & Asistencia",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Estamos a tu disposición para reposición de leña, encendido de tinajas o consultas del entorno.",
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2836).copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "WHATSAPP DE ATENCIÓN",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stay.hostWhatsappFormatted.ifBlank { "+56 9 7269 4597" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Disponible para mensajes las 24 hrs",
                    fontSize = 9.5.sp,
                    color = Color(0xFF34D399)
                )
            }
        }
    }
}

// =============================================================================
// SECCIÓN 3: COMODIDADES, ESTACIONAMIENTO Y CALEFACCIÓN
// =============================================================================
@Composable
private fun StayAmenitiesSection(
    unit: AccommodationUnit,
    stay: GuestStayInfo,
    dynamicCabin: DynamicCabinDetails?
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 700.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Comodidades y Convivencia",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )

        // Fila 1: Estacionamiento techado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC07101E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2836)),
                contentAlignment = Alignment.Center
            ) {
                CarCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
            }
            Column {
                Text(
                    text = "Estacionamiento techado dentro del recinto",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                val parkingSub = if (stay.isOccupied && stay.vehiclePlate.isNotBlank() && stay.vehiclePlate != "Estacionamiento Asignado") {
                    "Patente de vehículo registrada: ${stay.vehiclePlate}"
                } else {
                    "Recinto privado con portón seguro y acceso controlado"
                }
                Text(
                    text = parkingSub,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Fila 2: Horario de silencio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC07101E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2836)),
                contentAlignment = Alignment.Center
            ) {
                MoonCanvasIcon(color = Color(0xFFFBBF24), modifier = Modifier.size(17.dp))
            }
            Column {
                Text(
                    text = "Horario de descanso y silencio: 23:00 a 11:00 hrs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Por favor evita música alta y ruidos molestos para permitir el descanso de todas las familias.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Fila 3: Calefacción a leña y tinaja
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC07101E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2836)),
                contentAlignment = Alignment.Center
            ) {
                FlameCanvasIcon(color = Color(0xFFF97316), modifier = Modifier.size(18.dp))
            }
            Column {
                val flameTitle = if (unit.hasTinaja) "Tinaja de madera nativa y calefacción Bosca a leña" else "Calefacción Bosca a leña"
                Text(
                    text = flameTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Usa leña seca, regula el tiraje y mantén la puerta cerrada. La leña adicional tiene costo extra.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

// =============================================================================
// SECCIÓN 4: DISTRIBUCIÓN DE DORMITORIOS Y CAMAS
// =============================================================================
@Composable
private fun StayBedsSection(
    unit: AccommodationUnit,
    dynamicCabin: DynamicCabinDetails?
) {
    val bedrooms = dynamicCabin?.bedrooms ?: emptyList()
    val bedsSummary = dynamicCabin?.bedsSummary?.ifEmpty { unit.bedsDescription } ?: unit.bedsDescription

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 700.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Distribución de Dormitorios y Camas",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )

        if (bedrooms.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bedrooms.size) { idx ->
                    val rm = bedrooms[idx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC07101E))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2836)),
                            contentAlignment = Alignment.Center
                        ) {
                            BedCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text(
                                text = rm.name,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = rm.bedsDescription.ifEmpty { "Camas listas con sábanas y cobertores limpios" },
                                fontSize = 11.5.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC07101E))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = bedsSummary,
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// =============================================================================
// SECCIÓN 5: GALERÍA DE FOTOS HD Y ACCESO A VIDEO TOUR
// =============================================================================
@Composable
private fun StayPhotosAndTourSection(
    images: List<String>,
    selectedIndex: Int,
    cabinName: String,
    dynamicCabin: DynamicCabinDetails?,
    unit: AccommodationUnit,
    onSelectIndex: (Int) -> Unit,
    onPlayFullscreen: () -> Unit
) {
    val playInteraction = remember { MutableInteractionSource() }
    val isPlayFocused by playInteraction.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna Izquierda: Visor HD y tira de miniaturas
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val currentPhotoUrl = images.getOrNull(selectedIndex) ?: ""
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (currentPhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentPhotoUrl,
                        contentDescription = "Foto $cabinName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.80f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Foto ${selectedIndex + 1} de ${images.size} · $cabinName",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Miniaturas recorribles con D-Pad
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
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
                            .size(width = 86.dp, height = 48.dp)
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

        // Columna Derecha: Tarjeta de Video Tour Cinematográfico
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xCC07101E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoCameraCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    Text(
                        text = "Video Tour Cinematográfico",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Recorrido en video de la cabaña, terraza y bosque nativo.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
            }

            // Botón interactivo Reproducir
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .graphicsLayer {
                        val scale = if (isPlayFocused) 1.05f else 1.0f
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(50))
                    .background(if (isPlayFocused) Color(0xFF00E5FF) else Color(0xFF1E2836))
                    .border(
                        width = if (isPlayFocused) 2.dp else 1.dp,
                        color = if (isPlayFocused) Color.White else Color.White.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(
                        interactionSource = playInteraction,
                        indication = null
                    ) {
                        onPlayFullscreen()
                    }
                    .focusable(interactionSource = playInteraction)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                PlayCanvasIcon(
                    color = if (isPlayFocused) Color.Black else Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Ver Video en Pantalla Completa",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlayFocused) Color.Black else Color.White
                )
            }

            // Código QR para ver en móvil
            val youtubeId = dynamicCabin?.youtubeVideoId?.ifEmpty { "BoOdYfc7Hx8" } ?: "BoOdYfc7Hx8"
            val youtubeUrl = "https://www.youtube.com/watch?v=$youtubeId"
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(4.dp)
                ) {
                    QrCodeView(
                        content = youtubeUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "O escanéalo con tu móvil",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

// =============================================================================
// OVERLAY DE REPRODUCCIÓN DE VIDEO A PANTALLA COMPLETA
// =============================================================================
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (youtubeVideoId != null) {
            val embedUrl = "https://www.youtube-nocookie.com/embed/$youtubeVideoId?autoplay=1&controls=1&rel=0&iv_load_policy=3&playsinline=1"

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        loadUrl(embedUrl)
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
                onDispose { exoPlayer.release() }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Barra superior con botón Volver
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
// ICONOS VECTORIALES NATIVOS DE ALTO RENDIMIENTO (PIXEL PERFECT)
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
private fun WifiCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round)

        drawCircle(color = color, radius = 2.2f * density, center = Offset(w * 0.50f, h * 0.85f), style = Fill)

        val arc1 = Path().apply {
            moveTo(w * 0.32f, h * 0.65f)
            cubicTo(w * 0.40f, h * 0.55f, w * 0.60f, h * 0.55f, w * 0.68f, h * 0.65f)
        }
        drawPath(path = arc1, color = color, style = stroke)

        val arc2 = Path().apply {
            moveTo(w * 0.16f, h * 0.45f)
            cubicTo(w * 0.32f, h * 0.30f, w * 0.68f, h * 0.30f, w * 0.84f, h * 0.45f)
        }
        drawPath(path = arc2, color = color, style = stroke)

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
        drawCircle(color = color, radius = h * 0.20f, center = Offset(w * 0.50f, h * 0.28f), style = stroke)
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
        val pipe = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.35f)
            cubicTo(w * 0.15f, h * 0.20f, w * 0.55f, h * 0.20f, w * 0.55f, h * 0.35f)
        }
        drawPath(path = pipe, color = color, style = stroke)
        drawLine(color = color, start = Offset(w * 0.42f, h * 0.42f), end = Offset(w * 0.68f, h * 0.42f), strokeWidth = stroke.width * 1.5f)
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
        drawCircle(color = color, radius = h * 0.10f, center = Offset(w * 0.32f, h * 0.38f), style = stroke)
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
        val frame = Path().apply {
            moveTo(w * 0.10f, h * 0.30f)
            lineTo(w * 0.10f, h * 0.80f)
            moveTo(w * 0.10f, h * 0.65f)
            lineTo(w * 0.90f, h * 0.65f)
            lineTo(w * 0.90f, h * 0.80f)
        }
        drawPath(path = frame, color = color, style = stroke)
        drawRoundRect(color = color, topLeft = Offset(w * 0.18f, h * 0.45f), size = Size(w * 0.24f, h * 0.16f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
    }
}

@Composable
private fun VideoCameraCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        drawRoundRect(color = color, topLeft = Offset(w * 0.10f, h * 0.25f), size = Size(w * 0.55f, h * 0.50f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
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
