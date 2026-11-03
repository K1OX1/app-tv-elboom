package cl.elboomlicanray.tvlauncher.presentation.modals.info

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
 * Diálogos Modales Informativos (Wi-Fi, Mapa, Notificaciones, Agenda, Traslados, Playas, Excursiones).
 *
 * Responsabilidad:
 * Presentar guías informativas de interés turístico, conectividad y normas del complejo.
 */

@Composable
fun WifiInfoDialog(
    ssid: String,
    password: String,
    onDismiss: () -> Unit
) {
    val wifiQrString = remember(ssid, password) {
        "WIFI:T:WPA;S:$ssid;P:$password;;"
    }

    BaseHotelDialog(
        title = "Internet Satelital Starlink",
        subtitle = "Conexión de alta velocidad y baja latencia en todo el complejo",
        icon = "📡",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF111A2C))
                .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(18.dp))
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(10.dp)
            ) {
                QrCodeView(
                    content = wifiQrString,
                    modifier = Modifier.size(130.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📡 Red Wi-Fi:", fontSize = 12.sp, color = TvTokens.TextMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.20f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = "200+ MBPS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TvTokens.CyanBrand)
                    }
                }
                Text(text = ssid, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TvTokens.TextWhite)

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "🔑 Contraseña:", fontSize = 12.sp, color = TvTokens.TextMuted)
                Text(text = password, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TvTokens.CyanBrand)

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✨", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apunta la cámara de tu teléfono al código QR para conectarte automáticamente sin escribir la clave.",
                        fontSize = 11.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MapDialog(onDismiss: () -> Unit) {
    BaseHotelDialog(
        title = "Mapa de Alojamientos El Boom & Lican Ray",
        subtitle = "Puntos de interés y distancias clave a pie",
        icon = "🗺️",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "🏖️",
                title = "Playa Chica de Lican Ray",
                desc = "Acceso peatonal directo a 4 minutos caminando. Aguas templadas, muelle familiar y arriendo de botes.",
                badge = "300 METROS",
                badgeColor = TvTokens.CyanBrand
            )
            DialogItemRow(
                icon = "🌊",
                title = "Playa Grande & Península",
                desc = "Extensa playa con deportes acuáticos, vista panorámica al Volcán Villarrica y feria artesanal.",
                badge = "600 METROS",
                badgeColor = TvTokens.CyanBrand
            )
            DialogItemRow(
                icon = "🏪",
                title = "Supermercados & Gastronomía",
                desc = "Supermercado El Roble, carnicerías, panaderías con pan amasado caliente y farmacia a 5 minutos a pie.",
                badge = "400 METROS",
                badgeColor = Color(0xFF34D399)
            )
            DialogItemRow(
                icon = "🏥",
                title = "Servicios de Urgencia & Seguridad",
                desc = "CESFAM Lican Ray (800 m) · Retén de Carabineros (500 m) · Bomberos (600 m).",
                badge = "URGENCIAS",
                badgeColor = Color(0xFFF87171)
            )
        }
    }
}

@Composable
fun NotificationsDialog(
    stay: GuestStayInfo,
    unitName: String,
    onDismiss: () -> Unit
) {
    BaseHotelDialog(
        title = "Centro de Mensajes & Notificaciones",
        subtitle = "Avisos de conserjería para $unitName",
        icon = "🔔",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "👋",
                title = "¡Bienvenido a $unitName, ${stay.guestFirstName.ifEmpty { "Estimado Huésped" }}!",
                desc = "Agradecemos tu visita a Alojamientos El Boom Lican Ray. Si necesitas ayuda, nuestro equipo está 100% a tu disposición.",
                badge = "RECEPCIÓN",
                badgeColor = Color(0xFF34D399)
            )
            DialogItemRow(
                icon = "🪵",
                title = "Calefacción a Leña Disponible Sin Costo",
                desc = "Tu cabaña cuenta con estufa Bosca a combustión lenta. Puedes solicitar reposición de leña seca en cualquier momento por WhatsApp.",
                badge = "INCLUIDO",
                badgeColor = Color(0xFFFBBF24)
            )
            DialogItemRow(
                icon = "🚀",
                title = "Conexión Starlink de Alta Velocidad Activa",
                desc = "Tu pantalla y dispositivos móviles disfrutan de internet satelital de baja latencia con cobertura en todo el predio.",
                badge = "200+ MBPS",
                badgeColor = TvTokens.CyanBrand
            )
            if (stay.isCheckoutDay) {
                DialogItemRow(
                    icon = "⚠️",
                    title = "Recordatorio de Salida: Hoy a las 11:00 hrs",
                    desc = "Recuerda entregar las llaves en recepción antes de las 11:00 hrs para permitir la limpieza del alojamiento. ¡Buen viaje de regreso!",
                    badge = "CHECK-OUT 11:00",
                    badgeColor = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
fun AgendaDialog(onDismiss: () -> Unit) {
    BaseHotelDialog(
        title = "Agenda de Actividades & Experiencias",
        subtitle = "Horarios y panoramas recomendados durante tu estadía",
        icon = "📅",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "♨️",
                title = "Tinajas Privadas de Agua Caliente",
                desc = "Sesiones bajo las estrellas en el bosque de El Boom. Agua termal a 39°C. Coordinar en recepción con 2 horas de anticipación.",
                badge = "19:00 - 23:00 HRS",
                badgeColor = Color(0xFFFBBF24)
            )
            DialogItemRow(
                icon = "🪵",
                title = "Reparto de Leña Nativa Seca",
                desc = "Entrega de leña seca para la estufa de combustión lenta directo en la puerta de tu cabaña (Servicio gratuito incluido).",
                badge = "17:00 - 19:30 HRS",
                badgeColor = Color(0xFF34D399)
            )
            DialogItemRow(
                icon = "⛵",
                title = "Playa Chica & Deportes Náuticos",
                desc = "Aguas calmas y arena volcánica a 300 metros. Arriendo de kayaks, paseos en lancha y pedaleras familiares en el lago.",
                badge = "11:00 - 18:00 HRS",
                badgeColor = TvTokens.CyanBrand
            )
            DialogItemRow(
                icon = "🌋",
                title = "Tours Matinales · Termas Geométricas & Volcán Villarrica",
                desc = "Convenios con transporte directo saliendo a las 09:30 hrs. Entradas preferenciales y guías de montaña.",
                badge = "SALIDA 09:30 HRS",
                badgeColor = Color(0xFFA78BFA)
            )
        }
    }
}

@Composable
fun TransfersDialog(onDismiss: () -> Unit) {
    BaseHotelDialog(
        title = "Traslados & Conectividad Regional",
        subtitle = "Rutas, aeropuertos y conexiones terrestres",
        icon = "🚐",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "✈️",
                title = "Aeropuerto La Araucanía (ZCO - Temuco Freire)",
                desc = "Distancia: 95 km (1 hora 15 minutos por ruta asfaltada hacia el norte).",
                badge = "95 KM · 1H 15M",
                badgeColor = TvTokens.CyanBrand
            )
            DialogItemRow(
                icon = "🚐",
                title = "Transfer Privado de Alojamientos El Boom",
                desc = "Coordinación de van directa hacia aeropuerto o terminales al +56 9 7269 4597.",
                badge = "CONVENIO",
                badgeColor = Color(0xFFFBBF24)
            )
            DialogItemRow(
                icon = "🛣️",
                title = "Ruta Termas de Coñaripe & Liquiñe",
                desc = "Acceso directo por ruta costera pavimentada (18 km a Coñaripe y 32 km a Termas Geométricas).",
                badge = "PAVIMENTADO",
                badgeColor = Color(0xFF34D399)
            )
            DialogItemRow(
                icon = "🚌",
                title = "Terminal de Buses Lican Ray",
                desc = "Salidas diarias frecuentes a Villarrica, Pucón, Temuco, Valdivia y Santiago.",
                badge = "600 METROS",
                badgeColor = Color(0xFFA78BFA)
            )
        }
    }
}

@Composable
fun BeachDialog(onDismiss: () -> Unit) {
    BaseHotelDialog(
        title = "Playas & Lago Calafquén",
        subtitle = "Balnearios de arena volcánica y aguas cristalinas",
        icon = "🌊",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "🏖️",
                title = "Playa Chica de Lican Ray",
                desc = "Bahía protegida con aguas calmas y templadas, ideal para niños y descanso bajo los árboles.",
                badge = "300M · FAMILIAR",
                badgeColor = TvTokens.CyanBrand
            )
            DialogItemRow(
                icon = "⛵",
                title = "Playa Grande & Península de Lican Ray",
                desc = "Más de 1 kilómetro de playa volcánica con vista directa al Volcán Villarrica y deportes náuticos.",
                badge = "600M · VISTA VOLCÁN",
                badgeColor = Color(0xFFFBBF24)
            )
            DialogItemRow(
                icon = "🌲",
                title = "Mirador de la Península",
                desc = "Sendero de 15 minutos en el bosque nativo con vista de 360 grados sobre el lago Calafquén.",
                badge = "SENDERISMO",
                badgeColor = Color(0xFF34D399)
            )
        }
    }
}

@Composable
fun ExcursionsDialog(onDismiss: () -> Unit) {
    BaseHotelDialog(
        title = "Excursiones & Convenios Exclusivos",
        subtitle = "Parques nacionales, volcanes y termas en la Araucanía",
        icon = "📍",
        dialogWidth = 920.dp,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogItemRow(
                icon = "♨️",
                title = "Termas Geométricas (Pase Preferente)",
                desc = "60 pozones termales naturales encajonados en una quebrada de bosque nativo. Traslado coordinado.",
                badge = "CONVENIO VIP",
                badgeColor = Color(0xFFFBBF24)
            )
            DialogItemRow(
                icon = "🌋",
                title = "Ascenso Guiado al Volcán Villarrica",
                desc = "Trekking de montaña con equipo técnico completo hasta el cráter activo con guías certificados.",
                badge = "ALTA MONTAÑA",
                badgeColor = Color(0xFFF87171)
            )
            DialogItemRow(
                icon = "🌲",
                title = "Parque Nacional Villarrica & Huilo Huilo",
                desc = "Sendero Los Pozones, cascadas gigantes y reservas biológicas de la selva valdiviana.",
                badge = "RESERVA BIOLÓGICA",
                badgeColor = Color(0xFF34D399)
            )
        }
    }
}
