package cl.elboomlicanray.tvlauncher.presentation.sections.hero

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.R
import cl.elboomlicanray.tvlauncher.domain.model.GuestStayInfo
import cl.elboomlicanray.tvlauncher.domain.model.ScreenStayState
import cl.elboomlicanray.tvlauncher.presentation.components.TvIcon
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens
import cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

/**
 * Panel Hero Izquierdo: Bienvenida y Estado de la Reserva.
 *
 * Basado estrictamente en la imagen de referencia:
 * - Capa oscura sobre fotografía real de cabaña para legibilidad perfecta.
 * - "¡Bienvenido, huésped!" (o nombre dinámico).
 * - "Tu alojamiento" y nombre real de la unidad ("Cabaña N°1").
 * - Insignia "✓ RESERVA CONFIRMADA".
 * - Filas de Check-in, Check-out y Saldo pendiente con datos reales.
 * - Acción interactiva con D-Pad "Pagar saldo en el sitio web" cuando existe saldo pendiente.
 */
/**
 * Desglosa la descripción de camas en la cantidad y tipos de sábanas requeridas para el personal de preparación.
 */
private fun getBedsheetBreakdown(bedsDesc: String): List<String> {
    if (bedsDesc.isBlank()) return listOf("• Sábanas según capacidad del alojamiento")

    val items = bedsDesc.split(Regex("[|+]+"))
        .map { it.replace(Regex("[🛏️*]"), "").trim() }
        .filter { it.isNotBlank() && !it.equals("Camas equipadas", ignoreCase = true) }

    if (items.isEmpty()) return listOf("• Sábanas según distribución del alojamiento")

    val result = mutableListOf<String>()
    for (item in items) {
        val lower = item.lowercase()
        val text = when {
            lower.contains("king") -> "• 1 juego King (2.0 x 2.0 m)"
            lower.contains("queen") -> "• 1 juego Queen (1.6 x 2.0 m)"
            lower.contains("2 camas de 2 plazas") || lower.contains("2 camas matrimoniales") -> "• 2 juegos de 2 Plazas (Matrimonial)"
            lower.contains("3 camas matrimoniales") -> "• 3 juegos de 2 Plazas (Matrimonial)"
            lower.contains("1 cama 2 plazas") || lower.contains("1 cama matrimonial") -> "• 1 juego de 2 Plazas (Matrimonial)"
            lower.contains("2 literas") || lower.contains("2 literas de 1½ plazas") || lower.contains("2 literas de 1.5") -> "• 4 juegos de 1½ Plazas (Literas)"
            lower.contains("1 litera") -> "• 2 juegos de 1½ Plazas (Litera)"
            lower.contains("4 camas 1.5") -> "• 4 juegos de 1½ Plazas"
            lower.contains("3 camas 1.5") -> "• 3 juegos de 1½ Plazas"
            lower.contains("2 camas 1.5") -> "• 2 juegos de 1½ Plazas"
            lower.contains("1 cama 1.5") -> "• 1 juego de 1½ Plazas"
            lower.contains("2 camas nido") -> "• 2 juegos de 1 Plaza (Nido)"
            lower.contains("1 cama nido") -> "• 1 juego de 1 Plaza (Nido)"
            lower.contains("sofá cama") || lower.contains("sofa cama") -> "• 1 juego para Sofá Cama"
            else -> "• $item"
        }
        if (!result.contains(text)) {
            result.add(text)
        }
    }
    return if (result.isNotEmpty()) result else listOf("• Sábanas según distribución del alojamiento")
}

@Composable
fun WelcomeHeroPanel(
    unitName: String,
    guestStay: GuestStayInfo,
    cabinImageUrl: String? = null,
    selectedLanguage: String = "ES",
    modifier: Modifier = Modifier,
    onPayBalance: () -> Unit = {},
    onDismissCleaningOrWelcomeGuest: () -> Unit = {}
) {
    val strings = remember(selectedLanguage) { AppStringsCatalog.get(selectedLanguage) }

    // Nombre completo y elegante del huésped (ej: "Edoaurt Williams Galvez")
    val guestDisplayName = remember(guestStay.guestFullName, guestStay.guestFirstName) {
        if (guestStay.guestFullName.isNotBlank()) {
            val parts = guestStay.guestFullName.trim().split(Regex("\\s+"))
            if (parts.size >= 4) {
                "${parts[0]} ${parts[1]} ${parts[2]}"
            } else {
                guestStay.guestFullName.trim()
            }
        } else if (guestStay.guestFirstName.isNotBlank()) {
            guestStay.guestFirstName
        } else {
            strings.guestFallback
        }
    }

    // Formateo de fechas detalladas y comprensibles
    val checkinText = remember(guestStay.checkinDate, guestStay.checkinOfficialTime) {
        val officialTime = guestStay.checkinOfficialTime.ifBlank { "15:00 hrs" }
        val timeClean = if (officialTime.endsWith("hrs", ignoreCase = true)) officialTime else "$officialTime hrs"
        if (guestStay.isOccupied && guestStay.checkinDate.isNotBlank()) {
            if (guestStay.checkinDate.equals("hoy", ignoreCase = true)) {
                "Hoy · A partir de las $timeClean"
            } else {
                "${guestStay.checkinDate} · Desde las $timeClean"
            }
        } else {
            "Oficial · Desde las 15:00 hrs"
        }
    }

    val checkoutText = remember(guestStay.checkoutDate, guestStay.checkoutOfficialTime) {
        val officialTime = guestStay.checkoutOfficialTime.ifBlank { "11:00 hrs" }
        val timeClean = if (officialTime.endsWith("hrs", ignoreCase = true)) officialTime else "$officialTime hrs"
        if (guestStay.isOccupied && guestStay.checkoutDate.isNotBlank() && !guestStay.checkoutDate.contains("Salida", ignoreCase = true)) {
            "${guestStay.checkoutDate} · Hasta las $timeClean"
        } else {
            "Entrega oficial hasta las $timeClean"
        }
    }

    // Formateo de saldo
    val hasPendingBalance = guestStay.isOccupied && guestStay.balance > 0L && !guestStay.isPaid
    val clpFormatter = remember { NumberFormat.getIntegerInstance(Locale("es", "CL")) }
    val balanceText = if (hasPendingBalance) {
        "$${clpFormatter.format(guestStay.balance)} CLP"
    } else {
        "Cuenta al día ✓"
    }

    val heroShape = remember { RoundedCornerShape(20.dp) }

    Box(
        modifier = modifier
            .clip(heroShape)
            .background(TvTokens.CardBackgroundRest)
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                heroShape
            )
    ) {
        // 1. Fotografía real de cabaña alineada a la derecha
        val photoModel = if (!cabinImageUrl.isNullOrBlank() && !cabinImageUrl.contains("postimg.cc")) {
            cabinImageUrl
        } else {
            R.drawable.bg_cabana
        }
        AsyncImage(
            model = photoModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.82f }
        )

        // 2. Degradado horizontal oscuro para legibilidad garantizada desde el sofá
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color(0xFF030816),
                        0.46f to Color(0xFF030816).copy(alpha = 0.98f),
                        0.68f to Color(0xFF030816).copy(alpha = 0.52f),
                        1.0f to Color(0xFF030816).copy(alpha = 0.08f)
                    )
                )
        )

        // 3. Bisel especular de cristal Apple TV (Frosted glass sheen superior)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 4. Contenido interior estructurado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val isCleaning = guestStay.screenState == ScreenStayState.CLEANING
            val isPrepared = guestStay.screenState == ScreenStayState.PREPARED
            val isStandby = guestStay.screenState == ScreenStayState.STANDBY

            val headerPrefix = when {
                isCleaning -> "PREPARACIÓN DE ALOJAMIENTO"
                else -> strings.welcomePrefix
            }

            val headerPrefixColor = when {
                isCleaning -> Color(0xFFF59E0B)
                else -> TvTokens.CyanNeon
            }

            val headerMainTitle = when {
                isCleaning -> "Aseo y Preparación"
                isStandby -> "Alojamiento disponible"
                else -> guestDisplayName
            }

            val badgeText = when {
                isCleaning -> "🧹 PREPARACIÓN Y ASEO PENDIENTE"
                isPrepared -> "✓ ALOJAMIENTO PREPARADO · BIENVENIDO"
                isStandby -> "ALOJAMIENTO DISPONIBLE"
                else -> strings.bookingConfirmed
            }

            val badgeColor = when {
                isCleaning -> Color(0xFFF59E0B)
                isPrepared -> Color(0xFF10B981)
                isStandby -> Color(0xFF38BDF8)
                else -> Color(0xFF10B981)
            }

            val badgeBg = when {
                isCleaning -> Color(0x33F59E0B)
                isPrepared -> Color(0x3310B981)
                isStandby -> Color(0x3338BDF8)
                else -> Color(0x3310B981)
            }

            val badgeTextColor = when {
                isCleaning -> Color(0xFFFDE047)
                isPrepared -> TvTokens.EmeraldSuccessText
                isStandby -> TvTokens.BlueSky
                else -> TvTokens.EmeraldSuccessText
            }

            // TÍTULO Y ALOJAMIENTO
            Column {
                Text(
                    text = headerPrefix,
                    color = headerPrefixColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = headerMainTitle,
                    color = Color.White,
                    fontSize = if (headerMainTitle.length > 20) 24.sp else 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    lineHeight = if (headerMainTitle.length > 20) 28.sp else 34.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Fila Tu Alojamiento
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TvIcon(
                        name = "home",
                        size = 17.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Column {
                        Text(
                            text = strings.yourAccommodation,
                            color = TvTokens.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = unitName,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ESTADO DE RESERVA ESTILO VIDRIO ESMERILADO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(badgeBg)
                    .border(1.dp, badgeColor.copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(horizontal = 11.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    TvIcon(
                        name = if (isCleaning) "settings" else "check",
                        size = 9.dp,
                        color = Color.White,
                        strokeWidth = 2.6f
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            if (isCleaning) {
                // FICHA DE PREPARACIÓN DE ALOJAMIENTO (SÁBANAS DETALLADAS Y TOALLAS)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val bedsSummary = guestStay.selectedBedsSummary.ifBlank { unitName }
                    val sheetItems = remember(bedsSummary) { getBedsheetBreakdown(bedsSummary) }

                    Text(
                        text = "🛏️ Sábanas requeridas para camas:",
                        color = Color(0xFFFDE047),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    sheetItems.forEach { sheetItem ->
                        Text(
                            text = sheetItem,
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(1.dp))

                    val ngTotal = guestStay.nextGuestInfo?.totalGuests ?: guestStay.cleaningInfo.sheetsCount
                    val ngAdults = guestStay.nextGuestInfo?.adultsCount ?: 2
                    val ngChildren = guestStay.nextGuestInfo?.childrenCount ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛁 Toallas de baño:", color = Color(0xFFFDE047), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$ngTotal toallas ($ngAdults Ad${if (ngChildren > 0) " · $ngChildren Niñ" else ""})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!guestStay.nextGuestInfo?.specialRequests.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚠️ Petición especial:", color = Color(0xFFF59E0B), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = guestStay.nextGuestInfo?.specialRequests ?: "",
                                color = Color(0xFFFDE047),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🕒 Próximo ingreso:", color = TvTokens.TextSlate, fontSize = 11.sp)
                            Text(
                                text = "15:00 hrs · ${guestStay.nextGuestInfo?.fullName ?: ""}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // FICHA DE HUÉSPED (EN ESTADÍA / PREPARADO / ACTIVO)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Check-in detallado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(name = "calendar", size = 13.dp, color = TvTokens.CyanNeon)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Check-in (Ingreso)", color = TvTokens.TextSlate, fontSize = 11.sp)
                        }
                        Text(text = checkinText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Check-out detallado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(name = "calendar", size = 13.dp, color = TvTokens.TextSlate)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Check-out (Salida)", color = TvTokens.TextSlate, fontSize = 11.sp)
                        }
                        Text(text = checkoutText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Huéspedes / Ocupantes
                    val totalP = guestStay.totalGuests.coerceAtLeast(1)
                    val adP = guestStay.adultsCount.coerceAtLeast(1)
                    val chP = guestStay.childrenCount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(name = "user", size = 13.dp, color = TvTokens.TextSlate)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Ocupantes", color = TvTokens.TextSlate, fontSize = 11.sp)
                        }
                        Text(
                            text = "$totalP personas ($adP Ad${if (chP > 0) " · $chP Niñ" else ""})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Saldo pendiente / Estado de cuenta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(name = "card", size = 13.dp, color = TvTokens.TextSlate)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Estado de cuenta", color = TvTokens.TextSlate, fontSize = 11.sp)
                        }
                        Text(
                            text = balanceText,
                            color = if (hasPendingBalance) TvTokens.AmberPending else TvTokens.EmeraldSuccessText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // BOTÓN DE ACCIÓN RÁPIDA (PAGAR SALDO O ACTIVAR BIENVENIDA DE HUÉSPED)
            if (isCleaning) {
                val cleanActionInteractionSource = remember { MutableInteractionSource() }
                val isCleanActionFocused by cleanActionInteractionSource.collectIsFocusedAsState()

                val cleanScale by animateFloatAsState(
                    targetValue = if (isCleanActionFocused) 1.03f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                    label = "clean_scale"
                )

                val guestTargetName = guestStay.nextGuestInfo?.fullName?.trim()?.split(Regex("\\s+"))?.let { parts ->
                    if (parts.size >= 2) "${parts[0]} ${parts[1]}" else parts.firstOrNull() ?: ""
                } ?: ""
                val buttonLabel = if (guestTargetName.isNotBlank()) {
                    "✨ Huésped en alojamiento · Iniciar Bienvenida ($guestTargetName)"
                } else {
                    "✓ Marcar Preparación Lista · Alojamiento Listo"
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = cleanScale
                                scaleY = cleanScale
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCleanActionFocused) Color(0x6610B981) else Color(0x3310B981),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isCleanActionFocused) 1.8.dp else 1.dp,
                                color = if (isCleanActionFocused) Color(0xFF4ADE80) else Color(0xFF10B981).copy(alpha = 0.70f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .focusable(interactionSource = cleanActionInteractionSource)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp && (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)) {
                                    onDismissCleaningOrWelcomeGuest()
                                    true
                                } else false
                            }
                            .clickable(
                                interactionSource = cleanActionInteractionSource,
                                indication = null,
                                onClick = onDismissCleaningOrWelcomeGuest
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(
                                name = "check",
                                size = 14.dp,
                                color = if (isCleanActionFocused) Color(0xFF4ADE80) else Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buttonLabel,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TvIcon(
                            name = "chevron_right",
                            size = 11.dp,
                            color = Color(0xFF4ADE80)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Presiona OK en el control si ya estás en tu alojamiento",
                        color = TvTokens.TextMuted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            } else if (hasPendingBalance && !isPrepared) {
                val payInteractionSource = remember { MutableInteractionSource() }
                val isPayFocused by payInteractionSource.collectIsFocusedAsState()

                val payScale by animateFloatAsState(
                    targetValue = if (isPayFocused) 1.03f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                    label = "pay_scale"
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = payScale
                                scaleY = payScale
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isPayFocused) Color(0x5200E5FF) else Color(0x2400E5FF),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isPayFocused) 1.8.dp else 1.dp,
                                color = if (isPayFocused) TvTokens.CyanNeon else TvTokens.CyanNeon.copy(alpha = 0.70f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .focusable(interactionSource = payInteractionSource)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp && (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)) {
                                    onPayBalance()
                                    true
                                } else false
                            }
                            .clickable(
                                interactionSource = payInteractionSource,
                                indication = null,
                                onClick = onPayBalance
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(
                                name = "external",
                                size = 14.dp,
                                color = if (isPayFocused) TvTokens.CyanNeon else Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.payBalanceWeb,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TvIcon(
                            name = "chevron_right",
                            size = 11.dp,
                            color = TvTokens.CyanNeon
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Enlace personal de tu reserva",
                        color = TvTokens.TextMuted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            } else {
                // Espacio inferior limpio para preservar altura exacta y balance visual
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
