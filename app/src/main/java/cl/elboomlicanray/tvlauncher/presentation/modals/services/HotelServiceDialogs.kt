package cl.elboomlicanray.tvlauncher.presentation.modals.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.domain.model.GuestStayInfo
import cl.elboomlicanray.tvlauncher.presentation.components.qr.QrCodeView
import cl.elboomlicanray.tvlauncher.presentation.modals.base.BaseHotelDialog
import cl.elboomlicanray.tvlauncher.presentation.modals.base.DialogItemRow
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Diálogos Modales de Servicios del Hotel (Room Service, Restaurante, Spa, Actividades).
 *
 * Responsabilidad:
 * Presentar opciones de pedidos a la cabaña, recomendaciones gastronómicas,
 * reservas de tinajas/spa y turismo de aventura.
 */

@Composable
fun RoomServiceDialog(
    stay: GuestStayInfo,
    unitName: String,
    onDismiss: () -> Unit
) {
    val cleanPhone = remember(stay.hostWhatsapp) {
        val digits = stay.hostWhatsapp.filter { it.isDigit() }
        if (digits.startsWith("569") || digits.startsWith("56")) digits else "56$digits"
    }

    val whatsappUrl = remember(cleanPhone, unitName, stay.guestFirstName) {
        val guest = if (stay.guestFirstName.isNotBlank()) stay.guestFirstName else "Huésped"
        val message = "Hola recepción El Boom, les escribo desde $unitName (Huésped: $guest): "
        val encodedText = java.net.URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        "https://wa.me/$cleanPhone?text=$encodedText"
    }

    BaseHotelDialog(
        title = "Servicios y Asistencia",
        subtitle = "Contacto con recepción para pedidos a $unitName",
        icon = "🛎️",
        dialogWidth = 980.dp,
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.4f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DialogItemRow(
                    icon = "LEÑA",
                    title = "Reposición de Leña Seca",
                    desc = "Saco de leña seca para la estufa Bosca entregado en tu cabaña sin costo.",
                    badge = "SIN COSTO",
                    badgeColor = Color(0xFF34D399)
                )
                DialogItemRow(
                    icon = "TINAJA",
                    title = "Encendido de Tinaja Caliente",
                    desc = "Coordinamos el encendido a leña con al menos 7 horas de anticipación.",
                    badge = "AGENDAR",
                    badgeColor = Color(0xFFFBBF24)
                )
                DialogItemRow(
                    icon = "ROPA",
                    title = "Toallas y Ropa de Cama",
                    desc = "Cambio de toallas, sábanas o almohadas adicionales.",
                    badge = "A PEDIDO",
                    badgeColor = TvTokens.CyanBrand
                )
            }

            // QR WhatsApp para pedidos inmediatos
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.5.dp, Color(0xFF25D366).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF25D366).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF25D366), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "WHATSAPP RECEPCIÓN",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF25D366),
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeView(
                        content = whatsappUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stay.hostWhatsappFormatted.ifEmpty { "+56 9 7269 4597" },
                    fontSize = 12.5.sp,
                    color = TvTokens.TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Apunta la cámara de tu celular para chatear",
                    fontSize = 10.sp,
                    color = TvTokens.TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RestaurantDialog(onDismiss: () -> Unit) {
    BaseHotelDialog(
        title = "Gastronomía Recomendada en Lican Ray",
        subtitle = "Puntos culinarios imperdibles a pocos pasos de la cabaña",
        icon = "🍽️",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "🥩",
                title = "Asador Sureño & Carnes a las Brasas",
                desc = "Cortes premium a la parrilla, empanadas de pino y truchas frescas del Lago Calafquén.",
                badge = "TRADICIÓN SUREÑA",
                badgeColor = Color(0xFFFBBF24)
            )
            DialogItemRow(
                icon = "🍕",
                title = "Pizzería Artesanal a la Leña",
                desc = "Masas madre crujientes horneadas a la leña, tablas de quesos locales y cervezas artesanales.",
                badge = "HORNO A LEÑA",
                badgeColor = TvTokens.CyanBrand
            )
            DialogItemRow(
                icon = "☕",
                title = "Cafeterías & Repostería Alemana",
                desc = "Kuchen de murta y frambuesa, pan amasado caliente recién salido del horno y café de grano.",
                badge = "REPOSTERÍA",
                badgeColor = Color(0xFF34D399)
            )
        }
    }
}

@Composable
fun SpaDialog(onDismiss: () -> Unit) {
    BaseHotelDialog(
        title = "Bienestar, Tinajas & Relax",
        subtitle = "Experiencias de aguas termales y relajación en el bosque",
        icon = "♨️",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "♨️",
                title = "Tinajas Privadas de Madera Nativa",
                desc = "Agua caliente a 39°C rodeada de arrayanes y coihues. Sesiones nocturnas de 2 horas con copas de cortesía.",
                badge = "EN EL PREDIO",
                badgeColor = Color(0xFFFBBF24)
            )
            DialogItemRow(
                icon = "🌿",
                title = "Masajes Descontracturantes & Relajantes",
                desc = "Terapeutas certificadas con aceites esenciales de hierbas de la cordillera.",
                badge = "A DOMICILIO",
                badgeColor = Color(0xFF34D399)
            )
            DialogItemRow(
                icon = "🧖",
                title = "Circuito Termal de la Zona",
                desc = "Convenios con Termas Geométricas, Vergara y Coñaripe con descuentos exclusivos para huéspedes.",
                badge = "DESCUENTO 15%",
                badgeColor = TvTokens.CyanBrand
            )
        }
    }
}

@Composable
fun ActivitiesDialog(onDismiss: () -> Unit) {
    BaseHotelDialog(
        title = "Aventura & Naturaleza en Lican Ray",
        subtitle = "Deportes, lago Calafquén y vida al aire libre",
        icon = "🏃",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "🛶",
                title = "Arriendo de Kayaks y Stand Up Paddle",
                desc = "En Playa Chica con chalecos certificados y rutas guiadas por las bahías del Lago Calafquén.",
                badge = "PLAYA CHICA",
                badgeColor = TvTokens.CyanBrand
            )
            DialogItemRow(
                icon = "🚲",
                title = "Cicloturismo por la Ruta de los Lagos",
                desc = "Senderos panorámicos hacia Coñaripe y bosques nativos de la precordillera.",
                badge = "RUTAS PANORÁMICAS",
                badgeColor = Color(0xFF34D399)
            )
            DialogItemRow(
                icon = "🎣",
                title = "Pesca Deportiva Embarcada",
                desc = "Salidas matinales en bote con guías locales especializados en captura y devolución de truchas.",
                badge = "GUÍA LOCAL",
                badgeColor = Color(0xFFFBBF24)
            )
        }
    }
}
