package cl.elboomlicanray.tvlauncher.domain.model

import androidx.compose.runtime.Immutable

/**
 * Modelos de datos dinámicos extraídos en tiempo real desde la web oficial (https://elboom.cl).
 *
 * Se actualizan automáticamente cuando el administrador realiza cambios en el CMS web:
 * - /api/accommodations: Fotos reales HD, video tour de YouTube, camas y amenidades de cada cabaña.
 * - /api/tourist-guide: Atractivos turísticos (termas, parques, playas), distancia, tiempo y QR Google Maps.
 * - /api/property: Horarios, horario de silencio, tinajas a leña, sala de juegos, pet policy y FAQs.
 */

@Immutable
data class DynamicBedroom(
    val id: String = "",
    val name: String = "",
    val bedsDescription: String = ""
)

@Immutable
data class DynamicCabinDetails(
    val id: Int,
    val nobedsId: String = "",
    val name: String = "",
    val type: String = "Cabaña",
    val area: Int = 80,
    val capacity: Int = 8,
    val bathrooms: Int = 2,
    val bedsSummary: String = "",
    val view: String = "Bosque Nativo y Jardines",
    val description: String = "",
    val youtubeVideoId: String = "",
    val images: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val tinajaDetails: String = "",
    val bedrooms: List<DynamicBedroom> = emptyList(),
    val videoUrl: String = ""
)

@Immutable
data class DynamicTouristAttraction(
    val id: String,
    val name: String,
    val category: String, // "TERMAS", "PARQUES Y SENDEROS", "ACTIVIDADES", "PLAYAS", "CIUDADES"
    val subcategory: String = "",
    val distanceKm: String = "",
    val drivingTime: String = "",
    val descriptionShort: String = "",
    val descriptionLong: String = "",
    val googleMapsUrl: String = "",
    val phone: String = "",
    val price: String = "",
    val hours: String = "",
    val season: String = "",
    val difficulty: String = "",
    val recommendedByElBoom: Boolean = false,
    val tags: List<String> = emptyList(),
    val mainImageUrl: String = "",
    val gallery: List<String> = emptyList(),
    val youtubeUrl: String = "",
    val videoUrl: String = "",
    val rating: String = "4.8"
)

val DEFAULT_OFFICIAL_ATTRACTIONS = listOf(
    DynamicTouristAttraction(
        id = "LOC-001",
        name = "Licán Ray",
        category = "CIUDADES Y LOCALIDADES",
        subcategory = "Pueblo Lacustre",
        distanceKm = "4.7",
        drivingTime = "10 min",
        descriptionShort = "Un tranquilo balneario a orillas del Lago Calafquén, a 10 minutos de El Boom.",
        descriptionLong = "Licán Ray (Flor de Roca en mapudungun) es el epicentro turístico familiar de la zona. Destaca por sus playas de arena negra (Grande y Chica) y la icónica Península Ziwilwe.",
        googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=-39.489706,-72.155378",
        price = "Gratis",
        hours = "Acceso libre",
        season = "Todo el año",
        recommendedByElBoom = true,
        tags = listOf("⭐ IMPERDIBLE", "👨‍👩‍👧 FAMILIAR", "🛶 LAGO", "💰 ECONÓMICO"),
        mainImageUrl = "https://images.unsplash.com/photo-1439066615861-d1af74d74000?auto=format&fit=crop&w=1200&q=80",
        youtubeUrl = "https://www.youtube.com/watch?v=x9q8d7F9z0M",
        videoUrl = "https://www.youtube.com/watch?v=x9q8d7F9z0M",
        rating = "4.7"
    ),
    DynamicTouristAttraction(
        id = "NAT-001",
        name = "Península Ziwilwe",
        category = "ATRACTIVOS TURÍSTICOS",
        subcategory = "Mirador y Parque",
        distanceKm = "5.2",
        drivingTime = "13 min",
        descriptionShort = "Mirador natural administrado por la comunidad Mapuche. Divide la Playa Grande de la Chica y ofrece vistas insuperables de islas y el volcán.",
        descriptionLong = "A escasos minutos de El Boom, esta península boscosa divide geográficamente las dos playas principales de Licán Ray. Cuenta con senderos muy fáciles y bien delimitados.",
        googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=-39.492,-72.16",
        price = "Aporte voluntario",
        hours = "08:00 - 20:30 hrs",
        season = "Todo el año",
        recommendedByElBoom = true,
        tags = listOf("❤️ FAVORITO DE EL BOOM", "🌅 ATARDECERES", "👨‍👩‍👧 FAMILIAR"),
        mainImageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80",
        youtubeUrl = "",
        videoUrl = "",
        rating = "4.9"
    ),
    DynamicTouristAttraction(
        id = "LOC-002",
        name = "Villarrica",
        category = "CIUDADES Y LOCALIDADES",
        subcategory = "Ciudad Principal",
        distanceKm = "31",
        drivingTime = "37 min",
        descriptionShort = "Ciudad histórica con la mejor costanera de la región, excelentes restaurantes, grandes supermercados y servicios de salud completos.",
        descriptionLong = "Villarrica es la ciudad principal de referencia para los huéspedes de El Boom. Cuenta con una hermosa costanera ideal para pasear al atardecer mirando al volcán.",
        googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=-39.278104,-72.227492",
        price = "Gratis",
        hours = "09:00 a 20:00 hrs",
        season = "Todo el año",
        recommendedByElBoom = true,
        tags = listOf("👨‍👩‍👧 FAMILIAR", "🏙️ CIUDAD", "🌅 ATARDECERES"),
        mainImageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80",
        youtubeUrl = "",
        videoUrl = "",
        rating = "4.7"
    ),
    DynamicTouristAttraction(
        id = "TER-001",
        name = "Termas Geométricas",
        category = "TERMAS",
        subcategory = "Termas de Diseño",
        distanceKm = "37.9",
        drivingTime = "52 min",
        descriptionShort = "Las termas más fotogénicas de Chile. 20 piscinas de piedra unidas por pasarelas rojas de madera sobre una quebrada de bosque nativo.",
        descriptionLong = "Diseñadas por el arquitecto Germán del Sol, las Termas Geométricas son un imperdible mundial. Cuentan con 20 pozones tallados en la roca natural de una quebrada profunda.",
        googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=-39.501943,-71.874943",
        price = "Desde $35.000 CLP",
        hours = "10:00 - 19:00 hrs",
        season = "Todo el año",
        recommendedByElBoom = true,
        tags = listOf("⭐ IMPERDIBLE", "❤️ FAVORITO DE EL BOOM", "📸 FOTOGÉNICO", "♨️ RELAX"),
        mainImageUrl = "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=1200&q=80",
        youtubeUrl = "https://www.youtube.com/watch?v=0kG3n4f5zY8",
        videoUrl = "https://www.youtube.com/watch?v=0kG3n4f5zY8",
        rating = "4.9"
    ),
    DynamicTouristAttraction(
        id = "PAR-002",
        name = "Reserva Biológica Huilo Huilo",
        category = "PARQUES Y SENDEROS",
        subcategory = "Reserva Privada",
        distanceKm = "80.9",
        drivingTime = "1 h 22 min",
        descriptionShort = "Reserva natural de fama mundial conocida por sus hoteles con formas mágicas, sus increíbles cascadas y el Teleférico Cóndor Andino.",
        descriptionLong = "Excursión ideal de día completo desde El Boom. La Reserva Biológica Huilo Huilo protege miles de hectáreas de bosque nativo.",
        googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=-39.851098,-71.948003",
        price = "Desde $4.000 CLP",
        hours = "09:00 - 20:00 hrs",
        season = "Todo el año",
        recommendedByElBoom = true,
        tags = listOf("⭐ IMPERDIBLE", "🌲 NATURALEZA", "📸 FOTOGÉNICO", "👨‍👩‍👧 FAMILIAR"),
        mainImageUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1200&q=80",
        youtubeUrl = "",
        videoUrl = "",
        rating = "4.9"
    ),
    DynamicTouristAttraction(
        id = "ACT-001",
        name = "Transbordador Hua Hum (Puerto Fuy)",
        category = "ACTIVIDADES",
        subcategory = "Navegación",
        distanceKm = "87.7",
        drivingTime = "1 h 31 min",
        descriptionShort = "Increíble viaje en barcaza cruzando las aguas cristalinas del lago Pirehueico, inmerso en plena cordillera.",
        descriptionLong = "Ubicado al final de la ruta por Huilo Huilo, Puerto Fuy es el punto de embarque de los transbordadores que cruzan el Lago Pirehueico hacia la frontera con Argentina.",
        googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=-39.873761,-71.894033",
        price = "Aprox $3.000 CLP",
        hours = "Salidas programadas",
        season = "Todo el año",
        recommendedByElBoom = true,
        tags = listOf("🛶 LAGO", "📸 FOTOGÉNICO", "👨‍👩‍👧 FAMILIAR"),
        mainImageUrl = "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?auto=format&fit=crop&w=1200&q=80",
        youtubeUrl = "",
        videoUrl = "",
        rating = "4.8"
    )
)

@Immutable
data class DynamicTinajaOption(
    val id: String,
    val name: String,
    val capacity: Int,
    val photoUrl: String,
    val priceClp: Long,
    val description: String,
    val durationHours: Int = 3
)

@Immutable
data class DynamicFaq(
    val question: String,
    val answer: String
)

@Immutable
data class DynamicPropertyInfo(
    val checkIn: String = "15:00 a 22:00 hrs",
    val checkOut: String = "09:00 a 11:00 hrs",
    val quietHours: String = "23:00 a 11:00 hrs (estrictamente prohibida música alta o ruidos molestos)",
    val tinajasTitle: String = "Tinajas de Agua Caliente a Leña",
    val tinajasNotice: String = "Llenar y calentar una tinaja toma 7 horas de fuego continuo para asegurar 38.5°C.",
    val tinajaOptions: List<DynamicTinajaOption> = emptyList(),
    val indoorGames: String = "Mesa de Pool reglamentaria, Máquina Arcade multijuegos retro ($100), Mesa de Ping Pong profesional y Taca-Taca.",
    val kayaks: String = "Disponibles en Enero y Febrero en Playa Ensenada.",
    val outdoorPark: String = "Columpios, Sube y baja, Cama elástica protegida y Casa de juegos de madera.",
    val petFeePerNight: Long = 4000L,
    val petMaxAllowed: Int = 1,
    val petAllowedTypes: String = "Razas dóciles pequeñas a medianas",
    val petRequirement: String = "Enviar fotografía previa por WhatsApp para autorización.",
    val wifiNetwork: String = "El Boom Starlink",
    val wifiPassword: String = "licanray_paraiso",
    val hostWhatsapp: String = "+56972694597",
    val whatsappUrl: String = "https://wa.me/56972694597",
    val heroBgImage: String = "https://i.postimg.cc/KvkXjb86/1.jpg",
    val faqs: List<DynamicFaq> = emptyList()
)
