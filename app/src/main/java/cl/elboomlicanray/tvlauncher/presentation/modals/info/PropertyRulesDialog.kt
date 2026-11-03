package cl.elboomlicanray.tvlauncher.presentation.modals.info

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import cl.elboomlicanray.tvlauncher.R
import cl.elboomlicanray.tvlauncher.domain.model.DynamicPropertyInfo
import cl.elboomlicanray.tvlauncher.presentation.components.qr.QrCodeView
import cl.elboomlicanray.tvlauncher.presentation.i18n.AppStrings
import cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog

/**
 * Modelo interno para las 5 categorías oficiales de información.
 */
private data class InfoCategoryItem(
    val id: String,
    val title: String,
    @DrawableRes val thumbRes: Int,
    @DrawableRes val bgRes: Int
)

private fun getInfoCategories(strings: AppStrings) = listOf(
    InfoCategoryItem(
        id = "horarios",
        title = strings.tabSchedules,
        thumbRes = R.drawable.info_thumb_horarios,
        bgRes = R.drawable.bg_info_horarios
    ),
    InfoCategoryItem(
        id = "normas",
        title = strings.tabRules,
        thumbRes = R.drawable.info_thumb_normas,
        bgRes = R.drawable.bg_info_normas
    ),
    InfoCategoryItem(
        id = "tinajas",
        title = strings.tabHotTubs,
        thumbRes = R.drawable.info_thumb_tinajas,
        bgRes = R.drawable.bg_info_tinajas
    ),
    InfoCategoryItem(
        id = "mascotas",
        title = strings.tabPets,
        thumbRes = R.drawable.info_thumb_mascotas,
        bgRes = R.drawable.bg_info_mascotas
    ),
    InfoCategoryItem(
        id = "preguntas",
        title = strings.tabFaq,
        thumbRes = R.drawable.info_thumb_preguntas,
        bgRes = R.drawable.bg_info_preguntas
    )
)

/**
 * Diálogo Modal Rediseñado: "Información de tu estadía".
 *
 * Guía visual principal:
 * - Fotografía panorámica a pantalla completa con degradado azul marino oscuro para contraste.
 * - Información y datos integrados en el lado izquierdo sobre la imagen, sin paneles opacos pesados.
 * - Cabecera limpia con título "Información de tu estadía", subtítulo y botón "Cerrar".
 * - Franja horizontal inferior navegable con D-Pad del control remoto, con miniaturas y foco turquesa.
 * - 5 categorías exclusivas: Horarios, Normas, Tinajas, Mascotas, Preguntas (sin Juegos ni Servicios).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PropertyRulesDialog(
    propertyInfo: DynamicPropertyInfo,
    selectedLanguage: String = "ES",
    onDismiss: () -> Unit
) {
    val strings = remember(selectedLanguage) { AppStringsCatalog.get(selectedLanguage) }
    val infoCategories = remember(strings) { getInfoCategories(strings) }

    var isVisible by remember { mutableStateOf(false) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val closeFocusRequester = remember { FocusRequester() }
    val categoryFocusRequesters = remember { List(infoCategories.size) { FocusRequester() } }

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
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        BackHandler {
            onDismiss()
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.97f, animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)),
            exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.97f)
        ) {
            val activeCategory = infoCategories[selectedCategoryIndex]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF030712))
            ) {
                // =========================================================================
                // 1. FOTOGRAFÍA PANORÁMICA DE FONDO A PANTALLA COMPLETA CON CROSSFADE SUAVE
                // =========================================================================
                Crossfade(
                    targetState = activeCategory.bgRes,
                    animationSpec = tween(durationMillis = 260),
                    label = "InfoBackgroundCrossfade"
                ) { bgRes ->
                    AsyncImage(
                        model = bgRes,
                        contentDescription = activeCategory.title,
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
                                0.58f to Color(0x77030712),
                                0.85f to Color.Transparent
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
                    // BARRA SUPERIOR: TÍTULO, SUBTÍTULO Y BOTÓN CERRAR
                    // ---------------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.infoStayTitle,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.infoStaySubtitle,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.70f)
                            )
                        }

                        // Botón Cerrar
                        CloseHeaderButton(
                            focusRequester = closeFocusRequester,
                            label = strings.close,
                            onClick = onDismiss
                        )
                    }

                    // ---------------------------------------------------------------------
                    // CONTENIDO DINÁMICO SEGÚN LA CATEGORÍA SELECCIONADA (LADO IZQUIERDO)
                    // ---------------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 10.dp, bottom = 8.dp)
                    ) {
                        Crossfade(
                            targetState = selectedCategoryIndex,
                            animationSpec = tween(durationMillis = 200),
                            label = "CategoryContentCrossfade"
                        ) { catIndex ->
                            when (catIndex) {
                                0 -> HorariosSection(propertyInfo, strings)
                                1 -> NormasSection(strings)
                                2 -> TinajasSection(propertyInfo, strings)
                                3 -> MascotasSection(propertyInfo, strings)
                                4 -> PreguntasSection(propertyInfo, strings)
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
                        infoCategories.forEachIndexed { index, category ->
                            val isSelected = index == selectedCategoryIndex
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isFocused by itemInteraction.collectIsFocusedAsState()

                            // Sincronizar suavemente la categoría activa al recibir el foco del D-Pad
                            LaunchedEffect(isFocused) {
                                if (isFocused) {
                                    selectedCategoryIndex = index
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(136.dp)
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
                                // Miniatura con foco turquesa claro
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
                                    AsyncImage(
                                        model = category.thumbRes,
                                        contentDescription = category.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Nombre de categoría
                                Text(
                                    text = category.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isFocused -> Color(0xFF00E5FF)
                                        isSelected -> Color.White
                                        else -> Color.White.copy(alpha = 0.70f)
                                    },
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                // Indicador turquesa bajo la categoría seleccionada
                                Box(
                                    modifier = Modifier
                                        .width(38.dp)
                                        .height(2.2.dp)
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                            shape = RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SECCIONES DE CONTENIDO
// =============================================================================

/**
 * Pantalla Horarios:
 * - "Llegada, salida y descanso"
 * - Silencio (23:00 - 11:00)
 * - Check-in (15:00 - 22:00)
 * - Check-out (09:00 - 11:00)
 * - Estufa a leña (Leña disponible con costo adicional)
 */
@Composable
private fun HorariosSection(propertyInfo: DynamicPropertyInfo, strings: AppStrings) {
    val quietHoursFormatted = remember(propertyInfo.quietHours) {
        extractHours(propertyInfo.quietHours, default = "23:00 – 11:00")
    }
    val checkInFormatted = remember(propertyInfo.checkIn) {
        extractHours(propertyInfo.checkIn, default = "15:00 – 22:00")
    }
    val checkOutFormatted = remember(propertyInfo.checkOut) {
        extractHours(propertyInfo.checkOut, default = "09:00 – 11:00")
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 520.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.schedulesHeading,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. Silencio
        InfoRowItem(
            icon = { MoonCanvasIcon(color = Color(0xFFFBBF24), modifier = Modifier.size(24.dp)) },
            title = strings.silenceTitle,
            highlight = quietHoursFormatted,
            highlightColor = Color(0xFFFCD34D),
            description = strings.silenceDesc
        )

        ThinDivider()

        // 2. Check-in
        InfoRowItem(
            icon = { KeyCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) },
            title = strings.checkinTitle,
            highlight = checkInFormatted,
            highlightColor = Color.White,
            description = strings.checkinDesc
        )

        ThinDivider()

        // 3. Check-out
        InfoRowItem(
            icon = { DoorExitCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) },
            title = strings.checkoutTitle,
            highlight = checkOutFormatted,
            highlightColor = Color.White,
            description = strings.checkoutDesc
        )

        ThinDivider()

        // 4. Estufa a leña
        InfoRowItem(
            icon = { FlameCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) },
            title = strings.woodStoveTitle,
            highlight = strings.woodStoveNotice,
            highlightColor = Color.White,
            description = strings.woodStoveDesc
        )
    }
}

/**
 * Pantalla Normas: Convivencia familiar, velocidad en el predio, prevención de incendios y accesos.
 */
@Composable
private fun NormasSection(strings: AppStrings) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 520.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.rulesHeading,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. Convivencia
        InfoRowItem(
            icon = { UsersCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) },
            title = strings.familyEnvTitle,
            highlight = strings.familyEnvHighlight,
            highlightColor = Color.White,
            description = strings.familyEnvDesc
        )

        ThinDivider()

        // 2. Velocidad
        InfoRowItem(
            icon = { SpeedCanvasIcon(color = Color(0xFFFBBF24), modifier = Modifier.size(24.dp)) },
            title = strings.speedLimitTitle,
            highlight = strings.speedLimitHighlight,
            highlightColor = Color(0xFFFCD34D),
            description = strings.speedLimitDesc
        )

        ThinDivider()

        // 3. Prevención de incendios
        InfoRowItem(
            icon = { FlameAlertCanvasIcon(color = Color(0xFFF87171), modifier = Modifier.size(24.dp)) },
            title = strings.firePreventionTitle,
            highlight = strings.firePreventionHighlight,
            highlightColor = Color.White,
            description = strings.firePreventionDesc
        )

        ThinDivider()

        // 4. Portón y accesos
        InfoRowItem(
            icon = { LockCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) },
            title = strings.gateSecurityTitle,
            highlight = strings.gateSecurityHighlight,
            highlightColor = Color.White,
            description = strings.gateSecurityDesc
        )
    }
}

/**
 * Pantalla Tinajas: Información de tinajas a leña con tarjeta QR secundaria integrada.
 */
@Composable
private fun TinajasSection(propertyInfo: DynamicPropertyInfo, strings: AppStrings) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = strings.hotTubsHeading,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Tiempo de preparación
            InfoRowItem(
                icon = { ClockCanvasIcon(color = Color(0xFFFBBF24), modifier = Modifier.size(24.dp)) },
                title = strings.tubPrepTitle,
                highlight = strings.tubPrepHighlight,
                highlightColor = Color(0xFFFCD34D),
                description = propertyInfo.tinajasNotice.ifBlank {
                    strings.tubPrepNotice
                }
            )

            ThinDivider()

            // 2. Capacidad y sesión
            InfoRowItem(
                icon = { HotTubCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) },
                title = strings.tubFeatureTitle,
                highlight = strings.tubFeatureHighlight,
                highlightColor = Color.White,
                description = strings.tubFeatureDesc
            )

            ThinDivider()

            // 3. Agendamiento
            InfoRowItem(
                icon = { WhatsAppCanvasIcon(color = Color(0xFF34D399), modifier = Modifier.size(24.dp)) },
                title = strings.tubBookingTitle,
                highlight = "${strings.tubBookingHighlight} ${propertyInfo.hostWhatsapp}",
                highlightColor = Color(0xFF34D399),
                description = strings.tubBookingDesc
            )
        }

        Spacer(modifier = Modifier.width(36.dp))

        // Tarjeta QR limpia en la parte derecha
        Column(
            modifier = Modifier
                .width(190.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = strings.tubQrBadge,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF34D399)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(6.dp)
            ) {
                QrCodeView(
                    content = "https://wa.me/56972694597?text=Hola,%20quisiera%20agendar%20una%20tinaja%20de%20agua%20caliente",
                    modifier = Modifier.size(88.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.tubQrScan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = strings.tubQrLeadTime,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

/**
 * Pantalla Mascotas: Políticas pet-friendly, valores y normas de admisión.
 */
@Composable
private fun MascotasSection(propertyInfo: DynamicPropertyInfo, strings: AppStrings) {
    val feeFormatted = remember(propertyInfo.petFeePerNight) {
        "$" + propertyInfo.petFeePerNight.toString().reversed().chunked(3).joinToString(".").reversed()
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 520.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.petsHeading,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. Admisión
        InfoRowItem(
            icon = { PawCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) },
            title = strings.petAdmissionTitle,
            highlight = "${propertyInfo.petAllowedTypes} · Máx. ${propertyInfo.petMaxAllowed} mascota",
            highlightColor = Color.White,
            description = propertyInfo.petRequirement.ifBlank {
                strings.petAdmissionDesc
            }
        )

        ThinDivider()

        // 2. Tarifa por noche
        InfoRowItem(
            icon = { CoinCanvasIcon(color = Color(0xFFFBBF24), modifier = Modifier.size(24.dp)) },
            title = strings.petFeeTitle,
            highlight = "$feeFormatted ${strings.petFeePerNightSuffix}",
            highlightColor = Color(0xFFFCD34D),
            description = strings.petFeeDesc
        )

        ThinDivider()

        // 3. Reglas de convivencia
        InfoRowItem(
            icon = { ShieldCanvasIcon(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) },
            title = strings.petCareTitle,
            highlight = strings.petCareHighlight,
            highlightColor = Color.White,
            description = strings.petCareDesc
        )
    }
}

/**
 * Pantalla Preguntas: Preguntas frecuentes con respuestas claras.
 */
@Composable
private fun PreguntasSection(propertyInfo: DynamicPropertyInfo, strings: AppStrings) {
    val defaultFaqs = remember(propertyInfo, strings) {
        listOf(
            FaqItemData(
                question = strings.faqWifiQuestion,
                answer = try {
                    String.format(strings.faqWifiAnswer, propertyInfo.wifiNetwork, propertyInfo.wifiPassword)
                } catch (_: Exception) {
                    "Starlink Wi-Fi · ${propertyInfo.wifiNetwork} / ${propertyInfo.wifiPassword}"
                }
            ),
            FaqItemData(
                question = strings.faqWaterQuestion,
                answer = strings.faqWaterAnswer
            ),
            FaqItemData(
                question = strings.faqSuppliesQuestion,
                answer = strings.faqSuppliesAnswer
            ),
            FaqItemData(
                question = strings.faqParkingQuestion,
                answer = strings.faqParkingAnswer
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 540.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.faqHeading,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val faqsToShow = if (propertyInfo.faqs.isNotEmpty()) {
                propertyInfo.faqs.map { FaqItemData(it.question, it.answer) }
            } else {
                defaultFaqs
            }

            items(faqsToShow) { faq ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = faq.question,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = faq.answer,
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                }
            }
        }
    }
}

private data class FaqItemData(val question: String, val answer: String)

// =============================================================================
// COMPONENTES AUXILIARES Y FILAS DE DATOS
// =============================================================================

@Composable
private fun InfoRowItem(
    icon: @Composable () -> Unit,
    title: String,
    highlight: String,
    highlightColor: Color = Color.White,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = highlight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = highlightColor
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ThinDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

@Composable
private fun CloseHeaderButton(
    focusRequester: FocusRequester,
    label: String = "Cerrar",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) Color(0xFF00E5FF).copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "✕",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) Color(0xFF00E5FF) else Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFocused) Color(0xFF00E5FF) else Color.White
        )
    }
}

// =============================================================================
// ICONOGRAFÍA VECTORIAL LIMPIA (CANVAS NATIVO PARA ANDROID TV)
// =============================================================================

@Composable
private fun MoonCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.70f, h * 0.12f)
            cubicTo(w * 0.25f, h * 0.18f, w * 0.20f, h * 0.78f, w * 0.70f, h * 0.88f)
            cubicTo(w * 0.38f, h * 0.74f, w * 0.38f, h * 0.26f, w * 0.70f, h * 0.12f)
            close()
        }
        drawPath(path, color, style = stroke)
    }
}

@Composable
private fun KeyCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Cabeza de la llave
        drawCircle(
            color = color,
            radius = w * 0.22f,
            center = Offset(w * 0.35f, h * 0.35f),
            style = stroke
        )
        // Vástago de la llave
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.50f),
            end = Offset(w * 0.82f, h * 0.82f),
            strokeWidth = 2.2f * density,
            cap = StrokeCap.Round
        )
        // Dientes
        drawLine(
            color = color,
            start = Offset(w * 0.70f, h * 0.70f),
            end = Offset(w * 0.80f, h * 0.60f),
            strokeWidth = 2.2f * density,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.82f, h * 0.82f),
            end = Offset(w * 0.90f, h * 0.74f),
            strokeWidth = 2.2f * density,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun DoorExitCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Marco de la puerta
        val frame = Path().apply {
            moveTo(w * 0.55f, h * 0.16f)
            lineTo(w * 0.26f, h * 0.16f)
            lineTo(w * 0.26f, h * 0.84f)
            lineTo(w * 0.55f, h * 0.84f)
        }
        drawPath(frame, color, style = stroke)
        // Flecha de salida hacia la izquierda
        drawLine(
            color = color,
            start = Offset(w * 0.84f, h * 0.50f),
            end = Offset(w * 0.42f, h * 0.50f),
            strokeWidth = 2.2f * density,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.56f, h * 0.36f),
            end = Offset(w * 0.42f, h * 0.50f),
            strokeWidth = 2.2f * density,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.56f, h * 0.64f),
            end = Offset(w * 0.42f, h * 0.50f),
            strokeWidth = 2.2f * density,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun FlameCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            cubicTo(w * 0.56f, h * 0.28f, w * 0.82f, h * 0.45f, w * 0.82f, h * 0.66f)
            cubicTo(w * 0.82f, h * 0.86f, w * 0.68f, h * 0.90f, w * 0.50f, h * 0.90f)
            cubicTo(w * 0.32f, h * 0.90f, w * 0.18f, h * 0.86f, w * 0.18f, h * 0.66f)
            cubicTo(w * 0.18f, h * 0.45f, w * 0.44f, h * 0.28f, w * 0.50f, h * 0.12f)
            close()
        }
        drawPath(path, color, style = stroke)
        val innerPath = Path().apply {
            moveTo(w * 0.50f, h * 0.52f)
            cubicTo(w * 0.60f, h * 0.62f, w * 0.60f, h * 0.75f, w * 0.50f, h * 0.78f)
            cubicTo(w * 0.40f, h * 0.75f, w * 0.40f, h * 0.62f, w * 0.50f, h * 0.52f)
            close()
        }
        drawPath(innerPath, color, style = stroke)
    }
}

@Composable
private fun UsersCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Usuario principal centro
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.45f, h * 0.30f), style = stroke)
        val bodyPath = Path().apply {
            moveTo(w * 0.22f, h * 0.82f)
            cubicTo(w * 0.22f, h * 0.60f, w * 0.68f, h * 0.60f, w * 0.68f, h * 0.82f)
        }
        drawPath(bodyPath, color, style = stroke)
        // Segundo usuario a la derecha
        drawCircle(color, radius = w * 0.11f, center = Offset(w * 0.75f, h * 0.38f), style = stroke)
        val body2 = Path().apply {
            moveTo(w * 0.65f, h * 0.82f)
            cubicTo(w * 0.65f, h * 0.66f, w * 0.88f, h * 0.66f, w * 0.88f, h * 0.82f)
        }
        drawPath(body2, color, style = stroke)
    }
}

@Composable
private fun SpeedCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Arco del velocímetro
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.72f)
            cubicTo(w * 0.18f, h * 0.30f, w * 0.82f, h * 0.30f, w * 0.82f, h * 0.72f)
        }
        drawPath(path, color, style = stroke)
        // Aguja indicando baja velocidad hacia la izquierda
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.65f),
            end = Offset(w * 0.35f, h * 0.42f),
            strokeWidth = 2.4f * density,
            cap = StrokeCap.Round
        )
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.65f))
    }
}

@Composable
private fun FlameAlertCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Escudo de protección
        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.28f)
            lineTo(w * 0.82f, h * 0.56f)
            cubicTo(w * 0.82f, h * 0.78f, w * 0.50f, h * 0.88f, w * 0.50f, h * 0.88f)
            cubicTo(w * 0.50f, h * 0.88f, w * 0.18f, h * 0.78f, w * 0.18f, h * 0.56f)
            lineTo(w * 0.18f, h * 0.28f)
            close()
        }
        drawPath(shield, color, style = stroke)
        // Signo de exclamación dentro
        drawLine(color, Offset(w * 0.50f, h * 0.36f), Offset(w * 0.50f, h * 0.56f), strokeWidth = 2.4f * density, cap = StrokeCap.Round)
        drawCircle(color, radius = 2f * density, center = Offset(w * 0.50f, h * 0.68f))
    }
}

@Composable
private fun LockCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Grillete del candado
        val shackle = Path().apply {
            moveTo(w * 0.32f, h * 0.44f)
            lineTo(w * 0.32f, h * 0.26f)
            cubicTo(w * 0.32f, h * 0.14f, w * 0.68f, h * 0.14f, w * 0.68f, h * 0.26f)
            lineTo(w * 0.68f, h * 0.44f)
        }
        drawPath(shackle, color, style = stroke)
        // Cuerpo del candado
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.44f),
            size = Size(w * 0.56f, h * 0.44f),
            cornerRadius = CornerRadius(4f * density, 4f * density),
            style = stroke
        )
        // Ojo de la cerradura
        drawLine(color, Offset(w * 0.50f, h * 0.60f), Offset(w * 0.50f, h * 0.72f), strokeWidth = 2.2f * density, cap = StrokeCap.Round)
    }
}

@Composable
private fun ClockCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color, radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        // Manecillas del reloj (7 horas)
        drawLine(color, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.50f, h * 0.26f), strokeWidth = 2.2f * density, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.36f, h * 0.62f), strokeWidth = 2.2f * density, cap = StrokeCap.Round)
    }
}

@Composable
private fun HotTubCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Tina de madera
        val tub = Path().apply {
            moveTo(w * 0.16f, h * 0.50f)
            lineTo(w * 0.84f, h * 0.50f)
            lineTo(w * 0.78f, h * 0.86f)
            lineTo(w * 0.22f, h * 0.86f)
            close()
        }
        drawPath(tub, color, style = stroke)
        // Olas de vapor ascendente
        drawLine(color, Offset(w * 0.32f, h * 0.38f), Offset(w * 0.32f, h * 0.22f), strokeWidth = 2.2f * density, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.50f, h * 0.36f), Offset(w * 0.50f, h * 0.18f), strokeWidth = 2.2f * density, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.68f, h * 0.38f), Offset(w * 0.68f, h * 0.22f), strokeWidth = 2.2f * density, cap = StrokeCap.Round)
    }
}

@Composable
private fun WhatsAppCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Burbuja de chat con colita
        val bubble = Path().apply {
            moveTo(w * 0.50f, h * 0.16f)
            cubicTo(w * 0.74f, h * 0.16f, w * 0.86f, h * 0.32f, w * 0.86f, h * 0.50f)
            cubicTo(w * 0.86f, h * 0.68f, w * 0.74f, h * 0.84f, w * 0.50f, h * 0.84f)
            lineTo(w * 0.32f, h * 0.88f)
            lineTo(w * 0.36f, h * 0.74f)
            cubicTo(w * 0.22f, h * 0.68f, w * 0.14f, h * 0.60f, w * 0.14f, h * 0.50f)
            cubicTo(w * 0.14f, h * 0.32f, w * 0.26f, h * 0.16f, w * 0.50f, h * 0.16f)
            close()
        }
        drawPath(bubble, color, style = stroke)
        // Pequeño auricular interior
        drawLine(color, Offset(w * 0.40f, h * 0.44f), Offset(w * 0.60f, h * 0.56f), strokeWidth = 2.4f * density, cap = StrokeCap.Round)
    }
}

@Composable
private fun PawCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Almohadilla principal
        val pad = Path().apply {
            moveTo(w * 0.50f, h * 0.54f)
            cubicTo(w * 0.32f, h * 0.54f, w * 0.28f, h * 0.82f, w * 0.50f, h * 0.86f)
            cubicTo(w * 0.72f, h * 0.82f, w * 0.68f, h * 0.54f, w * 0.50f, h * 0.54f)
            close()
        }
        drawPath(pad, color, style = stroke)
        // 4 deditos
        drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.26f, h * 0.44f))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.42f, h * 0.30f))
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.58f, h * 0.30f))
        drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.74f, h * 0.44f))
    }
}

@Composable
private fun CoinCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color, radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        // Signo $
        drawLine(color, Offset(w * 0.50f, h * 0.26f), Offset(w * 0.50f, h * 0.74f), strokeWidth = 2f * density, cap = StrokeCap.Round)
        val sPath = Path().apply {
            moveTo(w * 0.62f, h * 0.38f)
            lineTo(w * 0.42f, h * 0.38f)
            lineTo(w * 0.42f, h * 0.50f)
            lineTo(w * 0.58f, h * 0.50f)
            lineTo(w * 0.58f, h * 0.62f)
            lineTo(w * 0.38f, h * 0.62f)
        }
        drawPath(sPath, color, style = stroke)
    }
}

@Composable
private fun ShieldCanvasIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.28f)
            lineTo(w * 0.82f, h * 0.56f)
            cubicTo(w * 0.82f, h * 0.78f, w * 0.50f, h * 0.88f, w * 0.50f, h * 0.88f)
            cubicTo(w * 0.50f, h * 0.88f, w * 0.18f, h * 0.78f, w * 0.18f, h * 0.56f)
            lineTo(w * 0.18f, h * 0.28f)
            close()
        }
        drawPath(path, color, style = stroke)
        // Checkmark dentro
        val check = Path().apply {
            moveTo(w * 0.36f, h * 0.50f)
            lineTo(w * 0.46f, h * 0.62f)
            lineTo(w * 0.66f, h * 0.38f)
        }
        drawPath(check, color, style = stroke)
    }
}

/**
 * Función utilitaria para extraer el rango horario ("HH:mm – HH:mm") respetando los datos del backend.
 */
private fun extractHours(raw: String, default: String): String {
    val regex = Regex("""(\d{1,2}:\d{2})\s*(?:a|to|-|–)\s*(\d{1,2}:\d{2})""")
    val match = regex.find(raw)
    return if (match != null) {
        "${match.groupValues[1]} – ${match.groupValues[2]}"
    } else {
        default
    }
}
