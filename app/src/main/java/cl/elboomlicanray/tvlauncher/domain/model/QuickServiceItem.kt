package cl.elboomlicanray.tvlauncher.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class QuickServiceItem(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val iconEmoji: String,
    val accentColor: Color = Color(0xFFE5B358),
    val actionType: ServiceActionType
)

enum class ServiceActionType {
    SHOW_WIFI_QR,
    SHOW_RECEPTION_CONTACT,
    SHOW_TOURIST_GUIDE,
    SHOW_HOT_SPRINGS_INFO,
    SHOW_BREAKFAST_MENU,
    OPEN_FIREPLACE_MODE
}
