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

    val guestGreeting = if (guestStay.isOccupied && guestStay.guestFirstName.isNotBlank()) {
        "${guestStay.guestFirstName}!"
    } else {
        "${strings.guestFallback}!"
    }

    // Formateo de fechas
    val checkinText = if (guestStay.isOccupied && guestStay.checkinDate.isNotBlank()) {
        if (guestStay.checkinDate.equals("hoy", ignoreCase = true) || guestStay.checkinDate.isBlank()) {
            "${strings.today} · ${guestStay.checkinOfficialTime.replace(" hrs", "")}"
        } else {
            "${guestStay.checkinDate} · ${guestStay.checkinOfficialTime.replace(" hrs", "")}"
        }
    } else {
        "${strings.official} · 15:00"
    }

    val checkoutText = if (guestStay.isOccupied && guestStay.checkoutDate.isNotBlank()) {
        "${guestStay.checkoutDate} · ${guestStay.checkoutOfficialTime.replace(" hrs", "")}"
    } else {
        "${strings.official} · 11:00"
    }

    // Formateo de saldo
    val hasPendingBalance = guestStay.isOccupied && guestStay.balance > 0L && !guestStay.isPaid
    val clpFormatter = remember { NumberFormat.getIntegerInstance(Locale("es", "CL")) }
    val balanceText = if (hasPendingBalance) {
        "$${clpFormatter.format(guestStay.balance)} CLP"
    } else {
        strings.noPendingBalance
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
                isCleaning -> "ALISTAMIENTO · CAMARERAS"
                isPrepared -> strings.welcomePrefix
                else -> strings.welcomePrefix
            }

            val headerPrefixColor = when {
                isCleaning -> Color(0xFFF59E0B)
                isPrepared -> TvTokens.CyanNeon
                else -> TvTokens.CyanNeon
            }

            val headerMainTitle = when {
                isCleaning -> "Aseo en proceso"
                isPrepared -> if (guestStay.guestFirstName.isNotBlank()) "${guestStay.guestFirstName}!" else strings.guestFallback
                isStandby -> "Alojamiento disponible"
                else -> guestGreeting
            }

            val badgeText = when {
                isCleaning -> "FICHA DE ALISTAMIENTO DE ALOJAMIENTO"
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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = headerMainTitle,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fila Tu Alojamiento
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TvIcon(
                        name = "home",
                        size = 18.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                            fontSize = 14.sp,
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
                // FICHA OPERATIVA DE ASEO PARA PERSONAL DE LIMPIEZA
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛏️ Sábanas a ingresar", color = TvTokens.TextSlate, fontSize = 11.sp)
                        Text(
                            text = "${guestStay.cleaningInfo.sheetsCount} juegos",
                            color = Color(0xFFFDE047),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val ngTotal = guestStay.nextGuestInfo?.totalGuests ?: guestStay.cleaningInfo.sheetsCount
                    val ngAdults = guestStay.nextGuestInfo?.adultsCount ?: 2
                    val ngChildren = guestStay.nextGuestInfo?.childrenCount ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "👥 Huéspedes (Toallas)", color = TvTokens.TextSlate, fontSize = 11.sp)
                        Text(
                            text = "$ngTotal pers ($ngAdults Ad · $ngChildren Niñ)",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!guestStay.nextGuestInfo?.specialRequests.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚠️ Petición especial", color = Color(0xFFF59E0B), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
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
                            Text(text = "🕒 Próximo ingreso", color = TvTokens.TextSlate, fontSize = 11.sp)
                            Text(
                                text = "15:00 hrs · ${guestStay.nextGuestInfo?.fullName ?: ""}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else if (isPrepared) {
                // ALOJAMIENTO PREPARADO
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "👤 Próximo Huésped", color = TvTokens.TextSlate, fontSize = 11.sp)
                        Text(
                            text = guestStay.nextGuestInfo?.fullName?.ifBlank { "Por confirmar" } ?: "Por confirmar",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🕒 Ingreso Oficial", color = TvTokens.TextSlate, fontSize = 11.sp)
                        Text(text = "Hoy · 15:00 hrs", color = Color(0xFF4ADE80), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🕒 Salida Oficial", color = TvTokens.TextSlate, fontSize = 11.sp)
                        Text(text = "11:00 hrs", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // REGULAR / STANDBY / IN_STAY
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Check-in
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(name = "calendar", size = 13.dp, color = TvTokens.TextSlate)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strings.checkinLabel, color = TvTokens.TextSlate, fontSize = 11.sp)
                        }
                        Text(text = checkinText, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Check-out
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(name = "calendar", size = 13.dp, color = TvTokens.TextSlate)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strings.checkoutLabel, color = TvTokens.TextSlate, fontSize = 11.sp)
                        }
                        Text(text = checkoutText, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Saldo pendiente
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvIcon(name = "card", size = 13.dp, color = TvTokens.TextSlate)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strings.pendingBalance, color = TvTokens.TextSlate, fontSize = 11.sp)
                        }
                        Text(
                            text = balanceText,
                            color = if (hasPendingBalance) TvTokens.AmberPending else TvTokens.EmeraldSuccessText,
                            fontSize = 11.5.sp,
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

                val guestTargetName = guestStay.nextGuestInfo?.fullName?.trim()?.split("\\s+".toRegex())?.firstOrNull() ?: ""
                val buttonLabel = if (guestTargetName.isNotBlank()) {
                    "✨ Huésped en cabaña · Ver Bienvenida ($guestTargetName)"
                } else {
                    "✓ Marcar Aseo Terminado · Alojamiento Listo"
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
