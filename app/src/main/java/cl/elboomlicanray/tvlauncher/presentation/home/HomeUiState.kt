package cl.elboomlicanray.tvlauncher.presentation.home

import androidx.compose.runtime.Immutable
import cl.elboomlicanray.tvlauncher.data.helper.NetworkStatus
import cl.elboomlicanray.tvlauncher.domain.model.AccommodationUnit
import cl.elboomlicanray.tvlauncher.domain.model.AppItem
import cl.elboomlicanray.tvlauncher.domain.model.DynamicCabinDetails
import cl.elboomlicanray.tvlauncher.domain.model.DynamicPropertyInfo
import cl.elboomlicanray.tvlauncher.domain.model.DynamicTouristAttraction
import cl.elboomlicanray.tvlauncher.domain.model.GuestStayInfo
import cl.elboomlicanray.tvlauncher.domain.model.OFFICIAL_ACCOMMODATION_UNITS
import cl.elboomlicanray.tvlauncher.domain.model.QuickServiceItem
import cl.elboomlicanray.tvlauncher.domain.model.WeatherInfo

/**
 * Estado inmutable de la pantalla principal para Android TV de alto rendimiento.
 * Conectado en tiempo real a los endpoints dinámicos del CMS web (elboom.cl).
 */
@Immutable
data class HomeUiState(
    val streamingApps: List<AppItem> = emptyList(),
    val cabinServices: List<QuickServiceItem> = emptyList(),
    val currentTimeString: String = "12:00",
    val currentDateString: String = "",
    val selectedLanguage: String = "ES",
    val configuredUnit: AccommodationUnit = OFFICIAL_ACCOMMODATION_UNITS.first(),
    val guestStay: GuestStayInfo = GuestStayInfo(),
    val weather: WeatherInfo = WeatherInfo(),
    val networkStatus: NetworkStatus = NetworkStatus(),
    val isScreensaverActive: Boolean = false,
    val isAdminDialogOpen: Boolean = false,
    val isWifiDialogVisible: Boolean = false,
    val isGuideDialogVisible: Boolean = false,
    val isLoading: Boolean = false,

    // CMS Dinámico en Vivo
    val currentCabinDetails: DynamicCabinDetails? = null,
    val allCabins: List<DynamicCabinDetails> = emptyList(),
    val touristAttractions: List<DynamicTouristAttraction> = emptyList(),
    val propertyInfo: DynamicPropertyInfo = DynamicPropertyInfo(),

    // Desbloqueo Streaming USA (Smart DNS)
    val isSmartDnsEnabled: Boolean = false,
    val smartDnsIp: String = "104.198.180.125"
)
