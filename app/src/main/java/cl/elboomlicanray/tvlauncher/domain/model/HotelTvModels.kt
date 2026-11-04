package cl.elboomlicanray.tvlauncher.domain.model

import androidx.compose.runtime.Immutable

/**
 * Catálogo maestro de unidades físicas de Alojamientos El Boom Lican Ray
 * Mapeado 1:1 con el Channel Manager Nobeds (Room IDs oficiales y características físicas).
 */
@Immutable
data class AccommodationUnit(
    val id: Int,
    val nobedsId: String,
    val name: String,
    val capacity: Int,
    val type: String,
    val area: Int = 80,
    val bathrooms: Int = 2,
    val bedsDescription: String = "Camas equipadas",
    val view: String = "Bosque Nativo y Jardines",
    val parkingNumber: String = "Estacionamiento techado Cabaña N°1",
    val amenities: List<String> = emptyList(),
    val hasTinaja: Boolean = true
)

val OFFICIAL_ACCOMMODATION_UNITS = listOf(
    AccommodationUnit(
        1, "2379407", "Cabaña N°1", 8, "Cabaña Premium",
        area = 80, bathrooms = 2, bedsDescription = "1 Cama King + 3 Camas 1.5p + 1 Sofá Cama",
        parkingNumber = "Estacionamiento techado Cabaña N°1",
        amenities = listOf("Tinaja Privada de Madera Nativa", "Wi-Fi Satelital Starlink", "Cocina americana 100% equipada", "Calefacción Bosca a Leña", "Parrilla techada exterior")
    ),
    AccommodationUnit(
        2, "2379409", "Cabaña N°2", 8, "Cabaña Premium",
        area = 80, bathrooms = 2, bedsDescription = "1 Cama King + 3 Camas 1.5p + 1 Sofá Cama",
        parkingNumber = "Estacionamiento techado Cabaña N°2",
        amenities = listOf("Tinaja Privada de Madera Nativa", "Wi-Fi Satelital Starlink", "Cocina americana 100% equipada", "Calefacción Bosca a Leña", "Parrilla techada exterior")
    ),
    AccommodationUnit(
        3, "2379564", "Cabaña N°3", 8, "Cabaña Premium",
        area = 80, bathrooms = 2, bedsDescription = "1 Cama King + 3 Camas 1.5p + 1 Sofá Cama",
        parkingNumber = "Estacionamiento techado Cabaña N°3",
        amenities = listOf("Tinaja Privada de Madera Nativa", "Wi-Fi Satelital Starlink", "Cocina americana 100% equipada", "Calefacción Bosca a Leña", "Parrilla techada exterior")
    ),
    AccommodationUnit(
        4, "2379568", "Habitación N°4 Triple", 3, "Habitación Triple",
        area = 28, bathrooms = 1, bedsDescription = "1 Cama 2 Plazas + 1 Cama 1.5p",
        parkingNumber = "Estacionamiento techado Habitación N°4",
        amenities = listOf("Frigobar", "Baño en Suite", "Wi-Fi Starlink", "Hervidor y vajilla")
    ),
    AccommodationUnit(
        5, "2379567", "Habitación N°5 Doble", 2, "Habitación Matrimonial",
        area = 24, bathrooms = 1, bedsDescription = "1 Cama 2 Plazas",
        parkingNumber = "Estacionamiento techado Habitación N°5",
        amenities = listOf("Frigobar", "Baño en Suite", "Wi-Fi Starlink", "Hervidor y vajilla")
    ),
    AccommodationUnit(
        6, "2379566", "Habitación N°6 Doble", 2, "Habitación Matrimonial",
        area = 24, bathrooms = 1, bedsDescription = "1 Cama 2 Plazas",
        parkingNumber = "Estacionamiento techado Habitación N°6",
        amenities = listOf("Frigobar", "Baño en Suite", "Wi-Fi Starlink", "Hervidor y vajilla")
    ),
    AccommodationUnit(
        7, "2379565", "Habitación N°7 Doble", 2, "Habitación Matrimonial",
        area = 24, bathrooms = 1, bedsDescription = "1 Cama 2 Plazas",
        parkingNumber = "Estacionamiento techado Habitación N°7",
        amenities = listOf("Frigobar", "Baño en Suite", "Wi-Fi Starlink", "Hervidor y vajilla")
    ),
    AccommodationUnit(
        9, "2487416", "Cabaña N°9 Familiar", 10, "Cabaña Familiar",
        area = 110, bathrooms = 2, bedsDescription = "2 Camas Matrimoniales + 4 Camas 1.5p + 1 Sofá Cama",
        parkingNumber = "Estacionamiento techado Cabaña N°9",
        amenities = listOf("Tinaja Privada de Madera Nativa", "Wi-Fi Satelital Starlink", "Cocina americana amplia", "Estufa Bosca a Leña grande", "Parrilla techada")
    ),
    AccommodationUnit(
        10, "2438474", "Cabaña N°10 Premium", 7, "Cabaña Premium",
        area = 85, bathrooms = 2, bedsDescription = "1 Cama King + 3 Camas 1.5p",
        parkingNumber = "Estacionamiento techado Cabaña N°10",
        amenities = listOf("Tinaja Privada de Madera Nativa", "Wi-Fi Satelital Starlink", "Cocina americana 100% equipada", "Calefacción Bosca a Leña", "Parrilla techada exterior")
    ),
    AccommodationUnit(
        12, "2438478", "Cabaña N°12 Master", 12, "Cabaña Master",
        area = 140, bathrooms = 3, bedsDescription = "3 Camas Matrimoniales + 4 Camas 1.5p + 2 Camas Nido",
        parkingNumber = "Estacionamiento techado Cabaña N°12",
        amenities = listOf("Tinaja Privada Gigante", "Wi-Fi Satelital Starlink", "Gran cocina y living", "Estufa Bosca a Leña", "Parrilla doble techada")
    )
)

enum class TvConnectionState {
    CONNECTED,      // 🟢 200 OK, Token Válido
    AUTH_ERROR,     // 🔴 401 Unauthorized, Token Inválido o Modificado en admin
    NO_INTERNET,    // 🔴 Sin conexión a Internet / Timeout
    UNCONFIGURED    // ⚪ Sin Cabaña / Token guardado en SharedPreferences
}

@Immutable
data class ConnectionStatusInfo(
    val state: TvConnectionState = TvConnectionState.UNCONFIGURED,
    val unitId: Int = 1,
    val unitName: String = "Cabaña N°1",
    val message: String = "Sin configurar",
    val detail: String = "Ingresa el PIN de acceso para conectar con elboom.cl",
    val httpCode: Int = 0,
    val lastVerifiedTimestamp: Long = 0L
)

class AuthTokenException(message: String = "Se requiere un Token válido. El token fue modificado en administración o es incorrecto.") : Exception(message)
class NetworkOfflineException(message: String = "Sin conexión a internet") : Exception(message)

enum class ScreenStayState {
    STANDBY,
    CLEANING,
    PREPARED,
    IN_STAY,
    CHECKOUT_DAY
}

@Immutable
data class CleaningInfo(
    val sheetTotal: Int? = null,
    val sheetDetails: String = "",
    val readyToken: String = "",
    val isDirty: Boolean = false,
    val status: String = "LIMPIA", // "SUCIA" o "LIMPIA"
    val sheetsCount: Int = 0,
    val notes: String = ""
)

@Immutable
data class NextGuestInfo(
    val peopleLabel: String = "",
    val phone: String = "",
    val pets: Boolean? = null,
    val fullName: String = "",
    val checkinTime: String = "15:00 hrs",
    val adultsCount: Int = 2,
    val childrenCount: Int = 0,
    val totalGuests: Int = 2,
    val sheetsCount: Int = 0,
    val specialRequests: String = ""
)

@Immutable
data class GuestStayInfo(
    val isOccupied: Boolean = false,
    val screenState: ScreenStayState = ScreenStayState.STANDBY,
    val title: String = "Bienvenido a Cabaña N°1",
    val subtitle: String = "Alojamientos El Boom · Lican Ray",
    val orderId: String = "",
    val guestFullName: String = "",
    val guestFirstName: String = "",
    val guestPhone: String = "",
    val guestEmail: String = "",
    val guestOriginCity: String = "Chile",
    val adultsCount: Int = 2,
    val childrenCount: Int = 0,
    val totalGuests: Int = 2,
    val bookingSource: String = "Directo Web El Boom",
    val vehiclePlate: String = "Estacionamiento Asignado",
    val selectedBedsSummary: String = "Configuración estándar",
    val specialRequests: String = "",
    val checkinDate: String = "",
    val checkoutDate: String = "",
    val nights: Int = 1,
    val stayProgress: String = "Estadía en curso",
    val totalAmount: Long = 0L,
    val prepaidAmount: Long = 0L,
    val balance: Long = 0L,
    val isPaid: Boolean = true,
    val balanceMessage: String = "Cuenta al día",
    val isCheckoutDay: Boolean = false,
    val checkinOfficialTime: String = "15:00 hrs",
    val checkoutOfficialTime: String = "11:00 hrs",
    val rotationTime: String = "12:00 hrs",
    val checkoutAlert: String? = null,
    val wifiNetwork: String = "El Boom Starlink",
    val wifiPassword: String = "licanray_paraiso",
    val wifiSpeedText: String = "Starlink Satelital Alta Velocidad",
    val cleaningInfo: CleaningInfo = CleaningInfo(),
    val nextGuestInfo: NextGuestInfo? = null,
    val hostWhatsapp: String = "+56972694597",
    val hostWhatsappFormatted: String = "+56 9 7269 4597"
)

@Immutable
data class DayForecast(
    val dateString: String = "",
    val dayOfWeek: String = "LUN",
    val dayNumber: String = "1",
    val weatherCode: Int = 0,
    val conditionDescription: String = "Despejado",
    val conditionIcon: String = "☀️",
    val maxTempC: Int = 18,
    val minTempC: Int = 8,
    val windKmH: Int = 10
)

@Immutable
data class WeatherInfo(
    val temperatureC: Double = 18.0,
    val conditionDescription: String = "Lican Ray",
    val conditionIcon: String = "☀️",
    val isDay: Boolean = true,
    val weeklyForecast: List<DayForecast> = emptyList()
)
