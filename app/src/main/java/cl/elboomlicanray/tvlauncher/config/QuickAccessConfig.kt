package cl.elboomlicanray.tvlauncher.config

import androidx.compose.runtime.Immutable

/**
 * Configuración centralizada de las 5 tarjetas de navegación principales.
 *
 * Exactamente 5 categorías según referencia:
 * 1. TV en vivo — Ver canales
 * 2. Apps & Streaming — Tus apps favoritas
 * 3. Mi Alojamiento — Fotos HD, tour y reserva
 * 4. Guía Turística — Termas, senderos y GPS
 * 5. Información — Horarios, normas y tinajas
 */
@Immutable
data class QuickAccessItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val actionType: QuickAccessAction
)

enum class QuickAccessAction {
    LAUNCH_LIVE_TV,
    OPEN_STREAMING_MODAL,
    OPEN_STAY_MODAL,
    OPEN_TOURIST_GUIDE_MODAL,
    OPEN_RULES_MODAL
}

object QuickAccessConfig {
    val defaultShortcuts = listOf(
        QuickAccessItem(
            id = "live_tv",
            title = "TV en vivo",
            subtitle = "Ver canales",
            iconKey = "tv",
            actionType = QuickAccessAction.LAUNCH_LIVE_TV
        ),
        QuickAccessItem(
            id = "apps",
            title = "Apps & Streaming",
            subtitle = "Tus apps favoritas",
            iconKey = "apps",
            actionType = QuickAccessAction.OPEN_STREAMING_MODAL
        ),
        QuickAccessItem(
            id = "stay",
            title = "Mi Alojamiento",
            subtitle = "Fotos HD, tour y reserva",
            iconKey = "home",
            actionType = QuickAccessAction.OPEN_STAY_MODAL
        ),
        QuickAccessItem(
            id = "guide",
            title = "Guía Turística",
            subtitle = "Termas, senderos y GPS",
            iconKey = "map",
            actionType = QuickAccessAction.OPEN_TOURIST_GUIDE_MODAL
        ),
        QuickAccessItem(
            id = "rules",
            title = "Información",
            subtitle = "Horarios, normas y tinajas",
            iconKey = "info",
            actionType = QuickAccessAction.OPEN_RULES_MODAL
        )
    )

    fun getShortcuts(language: String = "ES"): List<QuickAccessItem> {
        val s = cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog.get(language)
        return listOf(
            QuickAccessItem(
                id = "live_tv",
                title = s.shortcutTvTitle,
                subtitle = s.shortcutTvSubtitle,
                iconKey = "tv",
                actionType = QuickAccessAction.LAUNCH_LIVE_TV
            ),
            QuickAccessItem(
                id = "apps",
                title = s.shortcutAppsTitle,
                subtitle = s.shortcutAppsSubtitle,
                iconKey = "apps",
                actionType = QuickAccessAction.OPEN_STREAMING_MODAL
            ),
            QuickAccessItem(
                id = "stay",
                title = s.shortcutStayTitle,
                subtitle = s.shortcutStaySubtitle,
                iconKey = "home",
                actionType = QuickAccessAction.OPEN_STAY_MODAL
            ),
            QuickAccessItem(
                id = "guide",
                title = s.shortcutGuideTitle,
                subtitle = s.shortcutGuideSubtitle,
                iconKey = "map",
                actionType = QuickAccessAction.OPEN_TOURIST_GUIDE_MODAL
            ),
            QuickAccessItem(
                id = "rules",
                title = s.shortcutInfoTitle,
                subtitle = s.shortcutInfoSubtitle,
                iconKey = "info",
                actionType = QuickAccessAction.OPEN_RULES_MODAL
            )
        )
    }
}
