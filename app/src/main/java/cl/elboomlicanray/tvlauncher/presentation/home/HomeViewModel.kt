package cl.elboomlicanray.tvlauncher.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.elboomlicanray.tvlauncher.data.helper.AppLauncherHelper
import cl.elboomlicanray.tvlauncher.data.helper.HotelPreferencesHelper
import cl.elboomlicanray.tvlauncher.data.repository.AppRepository
import cl.elboomlicanray.tvlauncher.data.repository.HotelTvRepository
import cl.elboomlicanray.tvlauncher.domain.model.AppItem
import cl.elboomlicanray.tvlauncher.domain.model.QuickServiceItem
import cl.elboomlicanray.tvlauncher.domain.model.ServiceActionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import android.content.Context
import cl.elboomlicanray.tvlauncher.data.helper.NetworkMonitor
import cl.elboomlicanray.tvlauncher.data.helper.SmartDnsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val appLauncherHelper: AppLauncherHelper,
    private val preferencesHelper: HotelPreferencesHelper,
    private val hotelTvRepository: HotelTvRepository,
    private val smartDnsManager: SmartDnsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _allInstalledApps = MutableStateFlow<List<AppItem>>(emptyList())
    val allInstalledApps: StateFlow<List<AppItem>> = _allInstalledApps.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private fun getLocaleForLanguage(langCode: String): Locale {
        return when (langCode.uppercase()) {
            "EN" -> Locale.US
            "PT" -> Locale("pt", "BR")
            "FR" -> Locale.FRANCE
            "DE" -> Locale.GERMANY
            else -> Locale("es", "CL")
        }
    }

    private fun getDateFormatForLanguage(langCode: String): SimpleDateFormat {
        val pattern = when (langCode.uppercase()) {
            "EN" -> "EEEE, MMMM d"
            "DE" -> "EEEE, d. MMMM"
            else -> "EEEE, d 'de' MMMM"
        }
        return SimpleDateFormat(pattern, getLocaleForLanguage(langCode))
    }

    private var screensaverJob: Job? = null
    private val SCREENSAVER_TIMEOUT_MS = 180_000L // 3 minutos de inactividad

    init {
        appLauncherHelper.onBeforeAppLaunch = {
            MainActivityEvents.triggerReleaseDecoders()
        }
        loadInitialData()
        startClockUpdates()
        startPeriodicSync()
        scheduleNoonTransition()
        resetScreensaverTimer()
    }

    private fun loadInitialData() {
        val configuredUnit = preferencesHelper.getConfiguredUnit()
        val savedLang = preferencesHelper.getSelectedLanguage()
        val now = Date()
        val dateFmt = getDateFormatForLanguage(savedLang)

        val isDnsActive = smartDnsManager.isEnabled()
        val dnsIp = smartDnsManager.getTargetDnsIp()

        _uiState.update {
            it.copy(
                configuredUnit = configuredUnit,
                selectedLanguage = savedLang,
                currentTimeString = timeFormat.format(now),
                currentDateString = dateFmt.format(now).replaceFirstChar { c -> c.uppercase() },
                isSmartDnsEnabled = isDnsActive,
                smartDnsIp = dnsIp
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val installed = appLauncherHelper.getInstalledTvApps()
            _allInstalledApps.value = installed

            val selectedPkgs = preferencesHelper.getSelectedAppPackages()
            val appsToShow = if (selectedPkgs.isNotEmpty()) {
                val filtered = installed.filter { selectedPkgs.contains(it.packageName) }
                if (filtered.isNotEmpty()) filtered else installed
            } else {
                installed
            }

            _uiState.update { it.copy(streamingApps = appsToShow) }
        }

        viewModelScope.launch {
            appRepository.getCabinServices().collect { services ->
                _uiState.update { it.copy(cabinServices = services) }
            }
        }

        viewModelScope.launch {
            MainActivityEvents.dismissScreensaverEvents.collect {
                dismissScreensaver()
            }
        }

        viewModelScope.launch {
            NetworkMonitor.observeNetwork(context).collect { netStatus ->
                _uiState.update { it.copy(networkStatus = netStatus) }
            }
        }

        refreshHotelAndWeatherData()
    }

    fun saveSelectedApps(selectedPackages: Set<String>) {
        preferencesHelper.setSelectedAppPackages(selectedPackages)
        val allApps = _allInstalledApps.value
        val appsToShow = if (selectedPackages.isNotEmpty()) {
            allApps.filter { selectedPackages.contains(it.packageName) }
        } else {
            allApps
        }
        _uiState.update { it.copy(streamingApps = appsToShow) }
    }

    fun refreshHotelAndWeatherData() {
        val unit = preferencesHelper.getConfiguredUnit()
        viewModelScope.launch {
            // 1. Stay en vivo (Nobeds PMS)
            val stayResult = hotelTvRepository.fetchHotelStayData()
            stayResult.onSuccess { stayInfo ->
                // Sanitización quirúrgica de privacidad de huéspedes
                val sanitizedStay = if (!stayInfo.isOccupied) {
                    stayInfo.copy(
                        orderId = "",
                        guestFullName = "",
                        guestFirstName = "Estimado Huésped",
                        guestPhone = "",
                        guestEmail = "",
                        guestOriginCity = "Chile",
                        specialRequests = "",
                        vehiclePlate = "Estacionamiento techado Cabaña N°${unit.id}"
                    )
                } else {
                    stayInfo
                }
                _uiState.update { it.copy(guestStay = sanitizedStay) }
            }

            // 2. Clima Lican Ray
            val weatherResult = hotelTvRepository.fetchLicanRayWeather()
            weatherResult.onSuccess { weatherInfo ->
                _uiState.update { it.copy(weather = weatherInfo) }
            }

            // 3. Cabañas y Showcase Dinámico (Fotos HD, Video Tour, Camas)
            val accommodationsResult = hotelTvRepository.fetchDynamicAccommodations()
            accommodationsResult.onSuccess { cabins ->
                val currentCabin = cabins.firstOrNull { it.id == unit.id } ?: cabins.firstOrNull()
                _uiState.update {
                    it.copy(
                        allCabins = cabins,
                        currentCabinDetails = currentCabin
                    )
                }
            }

            // 4. Guía Turística Dinámica (Termas, Senderos, Playas, Actividades)
            val guideResult = hotelTvRepository.fetchDynamicTouristGuide()
            guideResult.onSuccess { attractions ->
                _uiState.update { it.copy(touristAttractions = attractions) }
            }

            // 5. Normas, Reglas, Tinajas, Sala de Juegos y FAQs
            val propertyResult = hotelTvRepository.fetchDynamicProperty()
            propertyResult.onSuccess { propInfo ->
                _uiState.update { it.copy(propertyInfo = propInfo) }
            }
        }
    }

    /**
     * Polling periódico cada 2 minutos (120 segundos) para reflejar cambios en Nobeds
     * y rotaciones de huéspedes sin necesidad de reiniciar la app.
     */
    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (isActive) {
                delay(120_000L)
                refreshHotelAndWeatherData()
            }
        }
    }

    /**
     * Programación de rotación matemática exacta a las 12:00:00 hrs del mediodía.
     */
    private fun scheduleNoonTransition() {
        viewModelScope.launch {
            while (isActive) {
                val calNow = Calendar.getInstance()
                val calNoon = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (calNow.after(calNoon)) {
                    calNoon.add(Calendar.DAY_OF_YEAR, 1)
                }

                val msUntilNoon = calNoon.timeInMillis - calNow.timeInMillis
                if (msUntilNoon > 0) {
                    delay(msUntilNoon)
                    // En punto a las 12:00:00 rota de inmediato
                    refreshHotelAndWeatherData()
                } else {
                    delay(5_000L)
                }
            }
        }
    }

    private fun startClockUpdates() {
        viewModelScope.launch {
            while (isActive) {
                val now = Date()
                val currentLang = _uiState.value.selectedLanguage
                val formattedTime = timeFormat.format(now)
                val formattedDate = getDateFormatForLanguage(currentLang).format(now).replaceFirstChar { it.uppercase() }

                if (formattedTime != _uiState.value.currentTimeString || formattedDate != _uiState.value.currentDateString) {
                    _uiState.update {
                        it.copy(currentTimeString = formattedTime, currentDateString = formattedDate)
                    }
                }
                delay(15_000L) // Ahorro de batería y CPU
            }
        }
    }

    /**
     * Monitoreo de inactividad para Salvapantallas "Lican Ray Paraíso".
     */
    fun onUserInteraction() {
        if (_uiState.value.isScreensaverActive) {
            _uiState.update { it.copy(isScreensaverActive = false) }
            MainActivityEvents.isScreensaverShowing = false
        }
        resetScreensaverTimer()
    }

    private fun resetScreensaverTimer() {
        screensaverJob?.cancel()
        screensaverJob = viewModelScope.launch {
            delay(SCREENSAVER_TIMEOUT_MS)
            _uiState.update { it.copy(isScreensaverActive = true) }
            MainActivityEvents.isScreensaverShowing = true
        }
    }

    fun dismissScreensaver() {
        _uiState.update { it.copy(isScreensaverActive = false) }
        MainActivityEvents.isScreensaverShowing = false
        resetScreensaverTimer()
    }

    fun openAdminDialog() {
        _uiState.update { it.copy(isAdminDialogOpen = true) }
    }

    fun closeAdminDialog() {
        _uiState.update { it.copy(isAdminDialogOpen = false) }
    }

    fun verifyAdminPin(pin: String): Boolean {
        return preferencesHelper.verifyPin(pin)
    }

    fun isPinLockedOut(): Boolean {
        return preferencesHelper.isPinLockedOut()
    }

    fun getPinLockoutRemainingSeconds(): Int {
        return preferencesHelper.getLockoutRemainingSeconds()
    }

    fun saveConfiguredUnit(unitId: Int) {
        preferencesHelper.setConfiguredUnitId(unitId)
        val unit = preferencesHelper.getConfiguredUnit()
        val currentCabin = _uiState.value.allCabins.firstOrNull { it.id == unit.id }
        _uiState.update {
            it.copy(
                configuredUnit = unit,
                currentCabinDetails = currentCabin ?: it.currentCabinDetails
            )
        }
        refreshHotelAndWeatherData()
    }

    fun onAppClicked(app: AppItem) {
        onUserInteraction()
        appLauncherHelper.launchApp(app.packageName, app.name)
    }

    /**
     * Lanzamiento inmediato y directo de Zapping TV (Requisito imperativo del cliente).
     */
    fun launchZappingDirectly() {
        onUserInteraction()
        appLauncherHelper.launchApp("com.eltelon.zapping", "Zapping")
    }

    fun onServiceClicked(service: QuickServiceItem) {
        onUserInteraction()
        when (service.actionType) {
            ServiceActionType.SHOW_WIFI_QR -> {
                _uiState.update { it.copy(isWifiDialogVisible = true) }
            }
            ServiceActionType.SHOW_RECEPTION_CONTACT,
            ServiceActionType.SHOW_BREAKFAST_MENU,
            ServiceActionType.SHOW_TOURIST_GUIDE,
            ServiceActionType.SHOW_HOT_SPRINGS_INFO -> {
                _uiState.update { it.copy(isGuideDialogVisible = true) }
            }
            ServiceActionType.OPEN_FIREPLACE_MODE -> {
                appLauncherHelper.launchApp("com.google.android.youtube.tv", "YouTube 4K Fireplace")
            }
        }
    }

    fun dismissWifiDialog() {
        _uiState.update { it.copy(isWifiDialogVisible = false) }
    }

    fun dismissGuideDialog() {
        _uiState.update { it.copy(isGuideDialogVisible = false) }
    }

    fun isKioskPromptDismissed(): Boolean {
        return preferencesHelper.isKioskPromptDismissed()
    }

    fun setKioskPromptDismissed(dismissed: Boolean) {
        preferencesHelper.setKioskPromptDismissed(dismissed)
    }

    fun getSelectedLanguage(): String {
        return preferencesHelper.getSelectedLanguage()
    }

    fun setSelectedLanguage(languageCode: String) {
        val code = languageCode.uppercase()
        preferencesHelper.setSelectedLanguage(code)
        val now = Date()
        val dateFmt = getDateFormatForLanguage(code)
        _uiState.update {
            it.copy(
                selectedLanguage = code,
                currentDateString = dateFmt.format(now).replaceFirstChar { c -> c.uppercase() }
            )
        }
    }

    fun toggleSmartDns(enabled: Boolean) {
        _uiState.update { it.copy(isSmartDnsEnabled = enabled) }
        smartDnsManager.setDnsEnabled(enabled) { success ->
            _uiState.update { it.copy(isSmartDnsEnabled = enabled) }
        }
    }

    fun openTvNetworkSettings() {
        smartDnsManager.openNetworkSettings(context)
    }
}
