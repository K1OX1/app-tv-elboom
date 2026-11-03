package cl.elboomlicanray.tvlauncher.presentation.modals.info

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import cl.elboomlicanray.tvlauncher.domain.model.DEFAULT_OFFICIAL_ATTRACTIONS
import cl.elboomlicanray.tvlauncher.domain.model.DynamicTouristAttraction
import cl.elboomlicanray.tvlauncher.presentation.components.qr.QrCodeView
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Secuencia canónica de destinos solicitada para el alojamiento:
 * Licán Ray, Península Ziwilwe, Villarrica, Termas Geométricas, Huilo Huilo y Hua Hum.
 */
private val PREFERRED_DESTINATION_SEQUENCE = listOf(
    "Licán Ray",
    "Península Ziwilwe",
    "Villarrica",
    "Termas Geométricas",
    "Huilo Huilo",
    "Hua Hum"
)

private val FILTER_CATEGORIES = listOf(
    "TODOS",
    "CIUDADES",
    "ATRACTIVOS",
    "TERMAS",
    "PARQUES",
    "ACTIVIDADES"
)

/**
 * Guía Turística Cinematográfica para Smart TV (Alojamiento Boutique El Boom).
 *
 * Reproduce fielmente la composición visual del diseño de referencia:
 * - Fotografía panorámica a pantalla completa con sutiles degradados oscuros de lectura.
 * - Cabecera con título, subtítulo, filtros horizontales con indicador turquesa fino y botón Cerrar.
 * - Ficha flotante sobre el paisaje (Categoría, Título, Descripción breve, Rating y Distancia).
 * - Botones de acción ("Ver video" principal y "Cómo llegar ↗").
 * - Fila compacta de datos prácticos (Horario, Precio, Temporada) con divisores finos sin cajas pesadas.
 * - Franja horizontal inferior de miniaturas de destinos con indicador turquesa discreto en el seleccionado.
 * - Tarjeta secundaria y limpia con QR de navegación Google Maps en la esquina inferior derecha.
 * - Reproductor de video inmersivo a pantalla completa con retorno exacto a la ficha seleccionada.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TouristGuideDialog(
    attractions: List<DynamicTouristAttraction>,
    selectedLanguage: String = "ES",
    onSendAttractionToWhatsApp: (suspend (DynamicTouristAttraction) -> Result<String>)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSendingWhatsApp by remember { mutableStateOf(false) }
    val strings = remember(selectedLanguage) {
        cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog.get(selectedLanguage)
    }
    var isVisible by remember { mutableStateOf(false) }

    // Lista ordenada con los datos reales de la app o el fallback oficial
    val allAttractions = remember(attractions) {
        val baseList = if (attractions.isNotEmpty()) attractions else DEFAULT_OFFICIAL_ATTRACTIONS
        sortAttractions(baseList)
    }

    var selectedFilter by remember { mutableStateOf("TODOS") }

    val filterCategories = remember(strings) {
        listOf(
            "TODOS" to strings.filterAll,
            "CIUDADES" to strings.filterCities,
            "ATRACTIVOS" to strings.filterAttractions,
            "TERMAS" to strings.filterHotSprings,
            "PARQUES" to strings.filterParks,
            "ACTIVIDADES" to strings.filterActivities
        )
    }

    val filteredAttractions = remember(selectedFilter, allAttractions) {
        if (selectedFilter == "TODOS") {
            allAttractions
        } else {
            allAttractions.filter { matchesFilter(it, selectedFilter) }
        }
    }

    var selectedAttraction by remember(allAttractions) {
        mutableStateOf(allAttractions.firstOrNull() ?: DEFAULT_OFFICIAL_ATTRACTIONS.first())
    }

    // Mantener sincronizado el lugar si cambia el filtro
    LaunchedEffect(filteredAttractions) {
        if (!filteredAttractions.contains(selectedAttraction)) {
            filteredAttractions.firstOrNull()?.let { selectedAttraction = it }
        }
    }

    // Estado del reproductor de video a pantalla completa
    var isPlayingVideo by remember { mutableStateOf(false) }

    val closeFocusRequester = remember { FocusRequester() }
    val initialActionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isVisible = true
        kotlinx.coroutines.delay(120)
        try {
            initialActionFocusRequester.requestFocus()
        } catch (_: Exception) {
            try {
                closeFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = {
            if (isPlayingVideo) {
                isPlayingVideo = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        // Soporte para botón BACK del control remoto de TV
        BackHandler {
            if (isPlayingVideo) {
                isPlayingVideo = false
            } else {
                onDismiss()
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = spring(dampingRatio = 0.90f, stiffness = 420f)),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.98f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF030712))
            ) {
                // =========================================================================
                // 1. FOTOGRAFÍA PANORÁMICA DE FONDO A PANTALLA COMPLETA (CINEMATOGRÁFICA)
                // =========================================================================
                val backgroundModel = remember(selectedAttraction) {
                    getAttractionBackground(selectedAttraction)
                }

                Crossfade(
                    targetState = backgroundModel,
                    animationSpec = tween(durationMillis = 260),
                    label = "BackgroundImageCrossfade"
                ) { bgModel ->
                    AsyncImage(
                        model = bgModel,
                        contentDescription = selectedAttraction.name,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // =========================================================================
                // 2. DEGRADADOS OSCUROS SUTILES PARA LEGIBILIDAD ÓPTIMA SIN OPACAR EL PAISAJE
                // =========================================================================
                // Scrim lateral izquierdo para bloque de textos, títulos y datos (sutil y natural)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.00f to Color.Black.copy(alpha = 0.62f),
                                0.26f to Color.Black.copy(alpha = 0.38f),
                                0.45f to Color.Black.copy(alpha = 0.12f),
                                0.60f to Color.Transparent
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

                // Scrim inferior para franja de miniaturas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
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
                // 3. CAPA PRINCIPAL DE INTERFAZ DE USUARIO (10-FOOT UI PARA TELEVISOR)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 12.dp)
                ) {
                    // ---------------------------------------------------------------------
                    // BARRA SUPERIOR: TÍTULO, CÁPSULA DE FILTROS Y BOTÓN CERRAR (ANTI-SQUASH)
                    // ---------------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Título y bajada superior izquierda
                        Column {
                            Text(
                                text = strings.guideTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = strings.guideSubtitle,
                                fontSize = 10.5.sp,
                                color = Color(0xFFBAC4D0)
                            )
                        }

                        // Filtros de categoría en cápsula oscura de alto contraste
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.50f))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            filterCategories.forEach { (catKey, catLabel) ->
                                val isSelected = catKey == selectedFilter
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            val scale = if (isFocused) 1.06f else 1.0f
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
                                            selectedFilter = catKey
                                        }
                                        .focusable(interactionSource = interactionSource)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = catLabel,
                                        fontSize = 10.5.sp,
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

                        // Botón Cerrar: protegido contra compresión (softWrap = false)
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
                                text = strings.close,
                                fontSize = 11.5.sp,
                                fontWeight = if (closeFocused) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (closeFocused) Color(0xFF030712) else Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // ---------------------------------------------------------------------
                    // CONTENIDO MEDIO: FICHA INFORMATIVA FLOTANTE SOBRE EL PAISAJE
                    // ---------------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .widthIn(max = 480.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Badge de Categoría con borde fino turquesa
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x3300E5FF))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.85f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = selectedAttraction.category.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color(0xFF00E5FF)
                                )
                            }

                            // 2. Nombre del destino (jerarquía tipográfica proporcionada para no desbordar)
                            Text(
                                text = selectedAttraction.name,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 40.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 3. Descripción breve (2 líneas exactas)
                            Text(
                                text = getCleanDescription(selectedAttraction),
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 4. Valoración y distancia disponibles
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Rating
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    StarCanvasIcon(
                                        color = Color(0xFFFBBF24),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = selectedAttraction.rating.ifBlank { "4.7" },
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Divisor fino
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(12.dp)
                                        .background(Color.White.copy(alpha = 0.35f))
                                )

                                // Distancia y tiempo en auto desde El Boom
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CarCanvasIcon(
                                        color = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    val distanceLabel = buildString {
                                        if (selectedAttraction.drivingTime.isNotBlank()) {
                                            append(selectedAttraction.drivingTime)
                                            append(" desde El Boom")
                                        } else if (selectedAttraction.distanceKm.isNotBlank()) {
                                            append(selectedAttraction.distanceKm)
                                            append(" km desde El Boom")
                                        } else {
                                            append("Cerca de El Boom")
                                        }
                                    }
                                    Text(
                                        text = distanceLabel,
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // 5. Botones de acción principales en fila unificada (Ver video, Enviar a mi WhatsApp, Cómo llegar)
                            val hasVideo = selectedAttraction.videoUrl.isNotBlank() || selectedAttraction.youtubeUrl.isNotBlank()
                            val gmapsUrl = selectedAttraction.googleMapsUrl.ifBlank {
                                "https://maps.google.com/?q=${selectedAttraction.name.replace(" ", "+")}+Lican+Ray"
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                // A. BOTÓN PRINCIPAL: "Ver video" (si el destino tiene video disponible)
                                if (hasVideo) {
                                    val videoInteraction = remember { MutableInteractionSource() }
                                    val videoFocused by videoInteraction.collectIsFocusedAsState()

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                                        modifier = Modifier
                                            .focusRequester(initialActionFocusRequester)
                                            .graphicsLayer {
                                                val scale = if (videoFocused) 1.08f else 1.0f
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (videoFocused) Color(0xFFE50914)
                                                else Color(0xCCB91C1C)
                                            )
                                            .border(
                                                width = if (videoFocused) 2.5.dp else 1.dp,
                                                color = if (videoFocused) Color.White else Color.White.copy(alpha = 0.40f),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clickable(
                                                interactionSource = videoInteraction,
                                                indication = null
                                            ) {
                                                val videoUrl = selectedAttraction.videoUrl.ifBlank { selectedAttraction.youtubeUrl }
                                                val ytId = extractYoutubeVideoId(videoUrl)
                                                if (ytId != null) {
                                                    launchYoutubeVideoTV(context, videoUrl)
                                                } else {
                                                    isPlayingVideo = true
                                                }
                                            }
                                            .focusable(interactionSource = videoInteraction)
                                            .padding(horizontal = 16.dp, vertical = 7.dp)
                                    ) {
                                        PlayCanvasIcon(
                                            color = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = strings.watchVideo,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // B. BOTÓN "Enviar a mi WhatsApp" (ÚNICO, ELEGANTE Y DE ALTO CONTRASTE)
                                if (onSendAttractionToWhatsApp != null) {
                                    val waInteraction = remember { MutableInteractionSource() }
                                    val waFocused by waInteraction.collectIsFocusedAsState()

                                    val focusModifier = if (!hasVideo) Modifier.focusRequester(initialActionFocusRequester) else Modifier

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                                        modifier = focusModifier
                                            .graphicsLayer {
                                                val scale = if (waFocused) 1.08f else 1.0f
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (waFocused) Color(0xFF25D366)
                                                else Color(0xCC052E16)
                                            )
                                            .border(
                                                width = if (waFocused) 2.5.dp else 1.2.dp,
                                                color = if (waFocused) Color.White else Color(0xFF25D366).copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clickable(
                                                interactionSource = waInteraction,
                                                indication = null,
                                                enabled = !isSendingWhatsApp
                                            ) {
                                                if (!isSendingWhatsApp) {
                                                    isSendingWhatsApp = true
                                                    coroutineScope.launch {
                                                        Toast.makeText(context, strings.sendingToWhatsApp, Toast.LENGTH_SHORT).show()
                                                        val result = onSendAttractionToWhatsApp(selectedAttraction)
                                                        isSendingWhatsApp = false
                                                        result.onSuccess { msg ->
                                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                        }.onFailure { err ->
                                                            Toast.makeText(
                                                                context,
                                                                err.message ?: "No se pudo enviar el mensaje a WhatsApp",
                                                                Toast.LENGTH_LONG
                                                             ).show()
                                                        }
                                                    }
                                                }
                                            }
                                            .focusable(interactionSource = waInteraction)
                                            .padding(horizontal = 16.dp, vertical = 7.dp)
                                    ) {
                                        WhatsAppCanvasIcon(
                                            color = if (waFocused) Color(0xFF030712) else Color(0xFF25D366),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isSendingWhatsApp) strings.sendingToWhatsApp else strings.sendToWhatsApp,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (waFocused) Color(0xFF030712) else Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // C. BOTÓN "Cómo llegar" (ABRE GOOGLE MAPS)
                                val mapInteraction = remember { MutableInteractionSource() }
                                val mapFocused by mapInteraction.collectIsFocusedAsState()

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    modifier = Modifier
                                        .graphicsLayer {
                                            val scale = if (mapFocused) 1.08f else 1.0f
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (mapFocused) Color(0xFF00E5FF)
                                            else Color(0xCC0D2030)
                                        )
                                        .border(
                                            width = if (mapFocused) 2.5.dp else 1.2.dp,
                                            color = if (mapFocused) Color.White else Color(0xFF00E5FF).copy(alpha = 0.70f),
                                            shape = RoundedCornerShape(50)
                                        )
                                        .clickable(
                                            interactionSource = mapInteraction,
                                            indication = null
                                        ) {
                                            openMapOrShowToast(context, gmapsUrl, selectedAttraction.name)
                                        }
                                        .focusable(interactionSource = mapInteraction)
                                        .padding(horizontal = 16.dp, vertical = 7.dp)
                                ) {
                                    ArrowOutwardCanvasIcon(
                                        color = if (mapFocused) Color(0xFF030712) else Color(0xFF00E5FF),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = strings.howToGetThere,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mapFocused) Color(0xFF030712) else Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------------------------
                    // FRANJA LOWER-MIDDLE: DATOS PRÁCTICOS + TARJETA ACCIÓN QR CELULAR
                    // ---------------------------------------------------------------------
                    val gmapsUrl = selectedAttraction.googleMapsUrl.ifBlank {
                        "https://maps.google.com/?q=${selectedAttraction.name.replace(" ", "+")}+Lican+Ray"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Fila de Horario, Precio y Temporada (estilo refinado de 50dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xD90A0F1D))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            // Horario
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                ClockCanvasIcon(
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = strings.labelSchedule,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = cleanHours(selectedAttraction.hours, strings),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Divisor fino
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color.White.copy(alpha = 0.20f))
                            )

                            // Precio
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                TagCanvasIcon(
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = strings.labelPrice,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = cleanPrice(selectedAttraction.price, strings),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Divisor fino
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color.White.copy(alpha = 0.20f))
                            )

                            // Temporada
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                CalendarCanvasIcon(
                                    color = Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = strings.labelSeason,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = cleanSeason(selectedAttraction.season, strings),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // 2. Tarjeta QR elegante con la misma altura (50.dp) y foco interactivo
                        val qrInteraction = remember { MutableInteractionSource() }
                        val qrFocused by qrInteraction.collectIsFocusedAsState()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .height(50.dp)
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
                                    openMapOrShowToast(context, gmapsUrl, selectedAttraction.name)
                                }
                                .focusable(interactionSource = qrInteraction)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .padding(2.dp)
                            ) {
                                QrCodeView(
                                    content = gmapsUrl,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column {
                                Text(
                                    text = strings.scanMapMobile,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp,
                                    color = if (qrFocused) Color(0xFF00E5FF) else Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = strings.scanForRoute,
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // ---------------------------------------------------------------------
                    // FRANJA INFERIOR: CARRUSEL COMPLETO DE DESTINOS DE ANCHO TOTAL
                    // ---------------------------------------------------------------------
                    val useEqualWeights = filteredAttractions.size >= 5
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (useEqualWeights) Arrangement.spacedBy(14.dp) else Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        filteredAttractions.forEach { item ->
                            val isSelected = item.id == selectedAttraction.id
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isFocused by itemInteraction.collectIsFocusedAsState()

                            // Sincronización suave con control remoto
                            LaunchedEffect(isFocused) {
                                if (isFocused) {
                                    selectedAttraction = item
                                }
                            }

                            val cardModifier = if (useEqualWeights) {
                                Modifier.weight(1f)
                            } else {
                                Modifier.width(140.dp)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = cardModifier
                                    .graphicsLayer {
                                        val scale = if (isFocused) 1.10f else 1.0f
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = if (isFocused || isSelected) 1.0f else 0.68f
                                    }
                                    .clickable(
                                        interactionSource = itemInteraction,
                                        indication = null
                                    ) {
                                        selectedAttraction = item
                                    }
                                    .focusable(interactionSource = itemInteraction)
                            ) {
                                // Miniatura 16:9 con contorno turquesa y glow inconfundible al foco
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(
                                            width = when {
                                                isFocused -> 3.5.dp
                                                isSelected -> 2.dp
                                                else -> 0.8.dp
                                            },
                                            color = when {
                                                isFocused -> Color(0xFF00E5FF)
                                                isSelected -> Color(0xFF00E5FF).copy(alpha = 0.85f)
                                                else -> Color.White.copy(alpha = 0.16f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    AsyncImage(
                                        model = getAttractionThumbnail(item),
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Nombre del destino: Insignia luminosa cyan al estar enfocado
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isFocused) Color(0xFF00E5FF) else Color.Transparent)
                                        .padding(horizontal = if (isFocused) 6.dp else 2.dp, vertical = if (isFocused) 2.dp else 0.dp)
                                ) {
                                    Text(
                                        text = getCarouselDisplayName(item.name),
                                        fontSize = 10.5.sp,
                                        fontWeight = when {
                                            isFocused -> FontWeight.Black
                                            isSelected -> FontWeight.Bold
                                            else -> FontWeight.Medium
                                        },
                                        color = when {
                                            isFocused -> Color(0xFF030712)
                                            isSelected -> Color.White
                                            else -> Color(0xFFBAC4D0)
                                        },
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Indicador turquesa bajo el destino seleccionado
                                Box(
                                    modifier = Modifier
                                        .width(34.dp)
                                        .height(2.2.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                            shape = RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // =========================================================================
                // 4. REPRODUCTOR DE VIDEO A PANTALLA COMPLETA CON CONTROL REMOTO
                // =========================================================================
                if (isPlayingVideo) {
                    val videoUrl = selectedAttraction.videoUrl.ifBlank { selectedAttraction.youtubeUrl }
                    FullScreenVideoOverlay(
                        title = selectedAttraction.name,
                        videoUrl = videoUrl,
                        onClose = { isPlayingVideo = false }
                    )
                }
            }
        }
    }
}

/**
 * Overlay de reproducción de video a pantalla completa adaptado para Smart TV.
 *
 * Maneja videos de YouTube mediante embed HTML5 optimizado para hardware de TV,
 * así como enlaces directos (MP4/HLS) mediante Media3 ExoPlayer.
 * Permite cerrar y volver a la misma ficha con el botón "Atrás" del control remoto
 * o mediante el botón "Volver" con foco interactivo.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                        iframe { width: 100%; height: 100%; border: none; }
                    </style>
                </head>
                <body>
                    <iframe 
                        src="https://www.youtube.com/embed/$youtubeVideoId?autoplay=1&controls=1&rel=0&modestbranding=1&enablejsapi=1&origin=https://elboom.cl" 
                        title="Video Turístico" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                        allowfullscreen>
                    </iframe>
                </body>
                </html>
            """.trimIndent()
            var webViewInstance by remember { mutableStateOf<WebView?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    try {
                        webViewInstance?.apply {
                            stopLoading()
                            loadUrl("about:blank")
                            clearHistory()
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
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            allowFileAccess = true
                            allowContentAccess = true
                            userAgentString = desktopUserAgent
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL("https://elboom.cl", htmlContent, "text/html", "UTF-8", null)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    // Mantener activo
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Reproducción directa MP4 / HLS mediante Media3 ExoPlayer
            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(videoUrl))
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(exoPlayer) {
                onDispose {
                    exoPlayer.release()
                }
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

        // Barra superior translúcida con botón de retorno al destino
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

            // Opción alternativa para abrir directamente en la app nativa de YouTube TV si está instalada
            if (youtubeVideoId != null) {
                val appInteraction = remember { MutableInteractionSource() }
                val appFocused by appInteraction.collectIsFocusedAsState()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (appFocused) Color(0xFFDC2626)
                            else Color.Black.copy(alpha = 0.65f)
                        )
                        .border(
                            width = if (appFocused) 2.dp else 1.dp,
                            color = if (appFocused) Color.White else Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(
                            interactionSource = appInteraction,
                            indication = null
                        ) {
                            launchYoutubeVideoTV(context, videoUrl)
                        }
                        .focusable(interactionSource = appInteraction)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "▶ Abrir en App de YouTube",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Ordena la lista de atractivos conservando la secuencia canónica:
 * Licán Ray, Península Ziwilwe, Villarrica, Termas Geométricas, Huilo Huilo y Hua Hum.
 */
private fun sortAttractions(items: List<DynamicTouristAttraction>): List<DynamicTouristAttraction> {
    return items.sortedBy { item ->
        val idx = PREFERRED_DESTINATION_SEQUENCE.indexOfFirst { namePattern ->
            item.name.contains(namePattern, ignoreCase = true)
        }
        if (idx != -1) idx else 99
    }
}

/**
 * Mapeo de categorías para filtros superiores.
 */
private fun matchesFilter(attraction: DynamicTouristAttraction, filter: String): Boolean {
    if (filter == "TODOS") return true
    val cat = attraction.category.uppercase()
    val sub = attraction.subcategory.uppercase()
    val name = attraction.name.uppercase()

    return when (filter) {
        "CIUDADES" -> cat.contains("CIUDAD") || sub.contains("PUEBLO") || sub.contains("CIUDAD")
        "ATRACTIVOS" -> cat.contains("ATRACTIVO") || sub.contains("MIRADOR")
        "TERMAS" -> cat.contains("TERMA") || name.contains("TERMA")
        "PARQUES" -> cat.contains("PARQUE") || cat.contains("SENDERO") || sub.contains("RESERVA") || name.contains("RESERVA")
        "ACTIVIDADES" -> cat.contains("ACTIVIDAD") || sub.contains("NAVEGACIÓN") || name.contains("TRANSBORDADOR")
        else -> cat.contains(filter)
    }
}

/**
 * Asigna la fotografía panorámica cinematográfica para cada destino.
 * Para Licán Ray utiliza la panorámica fotorrealista limpia al atardecer en el lago Calafquén.
 */
private fun getAttractionBackground(item: DynamicTouristAttraction): Any {
    return when {
        item.name.contains("Licán", ignoreCase = true) -> R.drawable.bg_calafquen_clean
        item.mainImageUrl.isNotBlank() -> item.mainImageUrl
        else -> R.drawable.bg_calafquen_clean
    }
}

/**
 * Asigna la miniatura exacta 16:9 de la guía turística del alojamiento.
 */
private fun getAttractionThumbnail(item: DynamicTouristAttraction): Any {
    return when {
        item.name.contains("Licán", ignoreCase = true) -> R.drawable.guide_licanray
        item.name.contains("Ziwilwe", ignoreCase = true) -> R.drawable.guide_ziwilwe
        item.name.contains("Villarrica", ignoreCase = true) -> R.drawable.guide_villarrica
        item.name.contains("Geométrica", ignoreCase = true) -> R.drawable.guide_termas
        item.name.contains("Huilo", ignoreCase = true) -> R.drawable.guide_huilohuilo
        item.name.contains("Hua Hum", ignoreCase = true) -> R.drawable.guide_huahum
        item.mainImageUrl.isNotBlank() -> item.mainImageUrl
        else -> R.drawable.guide_licanray
    }
}

/**
 * Obtiene la descripción concisa en 2 líneas ideal para televisión.
 */
private fun getCleanDescription(attraction: DynamicTouristAttraction): String {
    if (attraction.name.contains("Licán", ignoreCase = true)) {
        return "Un tranquilo balneario a orillas del Lago Calafquén, a 10 minutos de El Boom."
    }
    val short = attraction.descriptionShort.trim()
    return if (short.isNotBlank()) short else attraction.descriptionLong.trim()
}

private fun cleanHours(raw: String, strings: cl.elboomlicanray.tvlauncher.presentation.i18n.AppStrings): String {
    val clean = raw.trim()
    if (clean.isBlank()) return strings.freeAccess
    if (clean.contains("libre", ignoreCase = true) || clean.contains("24", ignoreCase = true)) return strings.freeAccess
    if (clean.contains("(") && clean.contains(")")) {
        val inside = clean.substringAfter("(").substringBefore(")").trim()
        if (inside.isNotBlank()) return inside
    }
    return clean
}

private fun cleanPrice(raw: String, strings: cl.elboomlicanray.tvlauncher.presentation.i18n.AppStrings): String {
    val clean = raw.trim()
    if (clean.isBlank() || clean.equals("0", ignoreCase = true)) return strings.freePrice
    if (clean.contains("Aporte", ignoreCase = true)) return "Aporte voluntario"
    if (clean.contains("Gratis", ignoreCase = true)) return strings.freePrice
    return clean
}

private fun cleanSeason(raw: String, strings: cl.elboomlicanray.tvlauncher.presentation.i18n.AppStrings): String {
    val clean = raw.trim()
    if (clean.isBlank()) return strings.allYear
    if (clean.contains("Todo el año", ignoreCase = true)) return strings.allYear
    return clean
}

/**
 * Nombre corto para no desbordar en las tarjetas inferiores de televisión.
 */
private fun getCarouselDisplayName(name: String): String {
    return when {
        name.contains("Licán Ray", ignoreCase = true) -> "Licán Ray"
        name.contains("Ziwilwe", ignoreCase = true) -> "Península Ziwilwe"
        name.contains("Villarrica", ignoreCase = true) -> "Villarrica"
        name.contains("Geométricas", ignoreCase = true) -> "Termas Geométricas"
        name.contains("Huilo Huilo", ignoreCase = true) -> "Huilo Huilo"
        name.contains("Hua Hum", ignoreCase = true) -> "Hua Hum"
        else -> name
    }
}

/**
 * Abre Google Maps o muestra un mensaje amigable invitando a escanear el QR.
 */
private fun openMapOrShowToast(context: Context, url: String, placeName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Escanea el código QR en pantalla con tu móvil para iniciar la ruta a $placeName",
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * Lanza el video directamente en la aplicación nativa de YouTube para Android TV (o navegador).
 */
private fun launchYoutubeVideoTV(context: Context, videoUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
            setPackage("com.google.android.youtube.tv")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(genericIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo abrir el reproductor de YouTube", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Extrae el identificador de video de un enlace estándar o corto de YouTube.
 */
private fun extractYoutubeVideoId(url: String): String? {
    if (url.isBlank()) return null
    val pattern = ".*(?:youtu.be/|v/|u/\\w/|embed/|watch\\?v=|&v=)([^#&?]*).*".toRegex()
    val match = pattern.find(url)
    val id = match?.groups?.get(1)?.value
    return if (!id.isNullOrBlank() && id.length in 10..12) id else null
}

// =============================================================================
// ICONOS VECTORIALES ULTRA LIGEROS NATIVOS PARA TV (PIXEL PERFECT)
// =============================================================================

@Composable
private fun StarCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.62f, h * 0.35f)
            lineTo(w * 0.92f, h * 0.38f)
            lineTo(w * 0.70f, h * 0.58f)
            lineTo(w * 0.76f, h * 0.88f)
            lineTo(w * 0.50f, h * 0.73f)
            lineTo(w * 0.24f, h * 0.88f)
            lineTo(w * 0.30f, h * 0.58f)
            lineTo(w * 0.08f, h * 0.38f)
            lineTo(w * 0.38f, h * 0.35f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
private fun CarCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.56f)
            lineTo(w * 0.24f, h * 0.32f)
            lineTo(w * 0.76f, h * 0.32f)
            lineTo(w * 0.88f, h * 0.56f)
            lineTo(w * 0.92f, h * 0.56f)
            lineTo(w * 0.92f, h * 0.76f)
            lineTo(w * 0.08f, h * 0.76f)
            lineTo(w * 0.08f, h * 0.56f)
            close()
        }
        drawPath(path = path, color = color, style = stroke)
        drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.26f, h * 0.76f))
        drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.76f))
    }
}

@Composable
private fun PlayCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.14f)
            lineTo(w * 0.86f, h * 0.50f)
            lineTo(w * 0.20f, h * 0.86f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
private fun ArrowOutwardCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.8f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.25f, h * 0.75f), end = Offset(w * 0.75f, h * 0.25f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.40f, h * 0.25f), end = Offset(w * 0.75f, h * 0.25f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.75f, h * 0.60f), end = Offset(w * 0.75f, h * 0.25f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun ClockCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f * 0.84f
        drawCircle(color = color, radius = r, center = center, style = stroke)
        drawLine(color = color, start = center, end = Offset(center.x, center.y - r * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = color, start = center, end = Offset(center.x + r * 0.46f, center.y), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun TagCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.46f)
            lineTo(w * 0.46f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.54f)
            lineTo(w * 0.54f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.46f)
            close()
        }
        drawPath(path = path, color = color, style = stroke)
        drawCircle(color = color, radius = 1.4f * density, center = Offset(w * 0.68f, h * 0.32f))
    }
}

@Composable
private fun CalendarCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val corner = CornerRadius(2.5f * density, 2.5f * density)
        drawRoundRect(color = color, topLeft = Offset(w * 0.14f, h * 0.22f), size = Size(w * 0.72f, h * 0.64f), cornerRadius = corner, style = stroke)
        drawLine(color = color, start = Offset(w * 0.14f, h * 0.42f), end = Offset(w * 0.86f, h * 0.42f), strokeWidth = stroke.width)
        drawLine(color = color, start = Offset(w * 0.34f, h * 0.12f), end = Offset(w * 0.34f, h * 0.26f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.66f, h * 0.12f), end = Offset(w * 0.66f, h * 0.26f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun CloseCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.0f * density, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.20f, h * 0.20f), end = Offset(w * 0.80f, h * 0.80f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.80f, h * 0.20f), end = Offset(w * 0.20f, h * 0.80f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun WhatsAppCanvasIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        
        // Burbuja de chat exterior
        val bubblePath = Path().apply {
            moveTo(w * 0.25f, h * 0.78f)
            lineTo(w * 0.15f, h * 0.88f)
            lineTo(w * 0.22f, h * 0.70f)
            close()
        }
        drawPath(bubblePath, color = color)
        
        // Círculo principal
        drawCircle(
            color = color,
            radius = size.minDimension * 0.38f,
            center = Offset(w * 0.50f, h * 0.46f),
            style = stroke
        )
        
        // Teléfono auricular estilizado interior
        val phonePath = Path().apply {
            moveTo(w * 0.38f, h * 0.40f)
            cubicTo(w * 0.38f, h * 0.52f, w * 0.48f, h * 0.62f, w * 0.60f, h * 0.62f)
        }
        drawPath(phonePath, color = color, style = stroke)
    }
}
