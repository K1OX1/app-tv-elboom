package cl.elboomlicanray.tvlauncher.config

import androidx.compose.runtime.Immutable

/**
 * Configuración centralizada de las pestañas de navegación superior (Header).
 *
 * Conectado a los módulos dinámicos:
 * - Mi estancia: Fotos reales, video tour y reserva Nobeds.
 * - Guía Turística: Termas, senderos y atractivos en vivo.
 * - Servicios: Room service y leña.
 * - Reglas: Horarios, silencio, tinajas y FAQs.
 */
@Immutable
data class NavigationItem(
    val id: String,
    val title: String,
    val iconName: String,
    val targetModal: String? = null
)

object NavigationConfig {
    val topNavItems = listOf(
        NavigationItem(
            id = "inicio",
            title = "Inicio",
            iconName = "home",
            targetModal = null
        ),
        NavigationItem(
            id = "apps",
            title = "Apps",
            iconName = "apps",
            targetModal = "streaming"
        ),
        NavigationItem(
            id = "estancia",
            title = "Mi cabaña",
            iconName = "bed",
            targetModal = "stay"
        ),
        NavigationItem(
            id = "guia",
            title = "Guía turística",
            iconName = "map",
            targetModal = "tourist_guide"
        ),
        NavigationItem(
            id = "servicios",
            title = "Servicios",
            iconName = "bell",
            targetModal = "roomservice"
        ),
        NavigationItem(
            id = "info",
            title = "Información",
            iconName = "info",
            targetModal = "rules"
        )
    )

    fun getTopNavItems(language: String = "ES"): List<NavigationItem> {
        val s = cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog.get(language)
        return listOf(
            NavigationItem(id = "inicio", title = s.navHome, iconName = "home", targetModal = null),
            NavigationItem(id = "apps", title = s.navApps, iconName = "apps", targetModal = "streaming"),
            NavigationItem(id = "estancia", title = s.navCabin, iconName = "bed", targetModal = "stay"),
            NavigationItem(id = "guia", title = s.navGuide, iconName = "map", targetModal = "tourist_guide"),
            NavigationItem(id = "servicios", title = s.navServices, iconName = "bell", targetModal = "roomservice"),
            NavigationItem(id = "info", title = s.navInfo, iconName = "info", targetModal = "rules")
        )
    }
}
