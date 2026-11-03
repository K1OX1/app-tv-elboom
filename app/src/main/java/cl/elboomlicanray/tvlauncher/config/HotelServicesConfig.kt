package cl.elboomlicanray.tvlauncher.config

import androidx.compose.ui.graphics.Color
import cl.elboomlicanray.tvlauncher.domain.model.QuickServiceItem
import cl.elboomlicanray.tvlauncher.domain.model.ServiceActionType

/**
 * Catálogo centralizado de Servicios de Cabañas y Atención al Huésped.
 *
 * Responsabilidad:
 * Definir los servicios ofrecidos en el complejo (Wi-Fi, Recepción, Desayuno, Termas, etc.).
 * Modifique este archivo para actualizar descripciones, etiquetas o añadir nuevos servicios.
 */
object HotelServicesConfig {
    val defaultCabinServices = listOf(
        QuickServiceItem(
            id = "wifi",
            title = "Wi-Fi & Código QR",
            description = "Escanear código QR en pantalla con tu celular",
            tag = "Conexión Rápida",
            iconEmoji = "📶",
            accentColor = Color(0xFF00C9A7),
            actionType = ServiceActionType.SHOW_WIFI_QR
        ),
        QuickServiceItem(
            id = "reception",
            title = "Recepción & Leña",
            description = "Pedidos de leña, toallas termales y conserjería",
            tag = "Atención Huésped",
            iconEmoji = "🛎️",
            accentColor = Color(0xFFE5B358),
            actionType = ServiceActionType.SHOW_RECEPTION_CONTACT
        ),
        QuickServiceItem(
            id = "breakfast",
            title = "Desayuno Campestre",
            description = "Pan amasado, mermeladas y café de grano",
            tag = "08:30 a 11:30",
            iconEmoji = "☕",
            accentColor = Color(0xFFFF9E00),
            actionType = ServiceActionType.SHOW_BREAKFAST_MENU
        ),
        QuickServiceItem(
            id = "termas",
            title = "Termas & Turismo",
            description = "Termas Geométricas y paseos Lago Calafquén",
            tag = "Paseos y Rutas",
            iconEmoji = "♨️",
            accentColor = Color(0xFFFF6B4A),
            actionType = ServiceActionType.SHOW_HOT_SPRINGS_INFO
        ),
        QuickServiceItem(
            id = "fireplace",
            title = "Modo Chimenea",
            description = "Fuego 4K y lluvia sureña para relajarse",
            tag = "Ambiente",
            iconEmoji = "🔥",
            accentColor = Color(0xFFFF8500),
            actionType = ServiceActionType.OPEN_FIREPLACE_MODE
        )
    )
}
