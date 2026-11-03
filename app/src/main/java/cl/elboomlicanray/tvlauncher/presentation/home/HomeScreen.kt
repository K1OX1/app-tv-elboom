package cl.elboomlicanray.tvlauncher.presentation.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cl.elboomlicanray.tvlauncher.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import coil.request.ImageRequest
import cl.elboomlicanray.tvlauncher.config.QuickAccessAction
import cl.elboomlicanray.tvlauncher.config.QuickAccessItem
import cl.elboomlicanray.tvlauncher.data.helper.KioskPermissionHelper
import cl.elboomlicanray.tvlauncher.presentation.components.AmbientScreensaver
import cl.elboomlicanray.tvlauncher.presentation.components.feedback.CheckoutAlertBanner
import cl.elboomlicanray.tvlauncher.presentation.modals.apps.StreamingAppsDialog
import cl.elboomlicanray.tvlauncher.presentation.modals.info.*
import cl.elboomlicanray.tvlauncher.presentation.modals.kiosk.ZappingKioskSetupDialog
import cl.elboomlicanray.tvlauncher.presentation.modals.services.*
import cl.elboomlicanray.tvlauncher.presentation.modals.stay.MyStayDialog
import cl.elboomlicanray.tvlauncher.presentation.modals.stay.PaymentDialog
import cl.elboomlicanray.tvlauncher.presentation.modals.weather.WeatherWeeklyForecastDialog
import cl.elboomlicanray.tvlauncher.presentation.sections.header.LauncherTopHeader
import cl.elboomlicanray.tvlauncher.presentation.sections.hero.PersistentLiveTvFrame
import cl.elboomlicanray.tvlauncher.presentation.sections.hero.WelcomeHeroPanel
import cl.elboomlicanray.tvlauncher.presentation.sections.shortcuts.PrimaryShortcutsCarousel
import cl.elboomlicanray.tvlauncher.data.update.OtaUpdateManager
import cl.elboomlicanray.tvlauncher.data.update.UpdateInfo
import cl.elboomlicanray.tvlauncher.presentation.home.components.OtaUpdateDialog
import cl.elboomlicanray.tvlauncher.presentation.theme.TvDimens
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * PANTALLA PRINCIPAL (HomeScreen Orchestrator).
 *
 * Responsabilidad:
 * Exclusivamente COMPOSICIÓN de alto nivel y orquestación de estado UI.
 *
 * Cada sección vive en su propio archivo modular:
 * - Header -> presentation/sections/header/LauncherTopHeader.kt
 * - Hero Bienvenida -> presentation/sections/hero/WelcomeHeroPanel.kt
 * - TV Persistente -> presentation/sections/hero/PersistentLiveTvFrame.kt
 * - Accesos Directos -> presentation/sections/shortcuts/PrimaryShortcutsCarousel.kt
 * - Diálogos y Modales Apple TV -> presentation/modals/
 * - Tokens y Medidas -> presentation/theme/TvTokens.kt & TvDimens.kt
 */
@kotlin.OptIn(ExperimentalTvMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val initialFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val currentLanguage = uiState.selectedLanguage

    // Enlace oficial de pago personal para la reserva activa
    val paymentUrl = remember(uiState.guestStay.orderId, uiState.configuredUnit.id) {
        val idParam = if (uiState.guestStay.orderId.isNotBlank()) uiState.guestStay.orderId else uiState.configuredUnit.id.toString()
        "https://elboom.cl/mi-reserva?id=$idParam"
    }

    // Estado de modales
    var activeModal by remember { mutableStateOf("none") }

    // Estado del sistema de auto-actualización OTA
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateProgress by remember { mutableStateOf<Float?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Verificación reforzada y continua de actualizaciones OTA
    val performUpdateCheck: suspend () -> Unit = remember(context) {
        {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toLong()
                }
                val update = OtaUpdateManager.checkUpdate(context, currentCode)
                if (update != null) {
                    val shownCode = availableUpdate?.versionCode ?: 0
                    if (update.versionCode > shownCode || availableUpdate == null) {
                        availableUpdate = update
                        updateProgress = null
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // 1. Verificación inmediata al arrancar y periódica continua (cada 3 minutos)
    LaunchedEffect(Unit) {
        while (isActive) {
            performUpdateCheck()
            kotlinx.coroutines.delay(180_000L)
        }
    }

    // 2. Verificación inmediata cada vez que el Launcher pasa a primer plano (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    performUpdateCheck()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Foco inicial en el primer acceso ("TV en vivo") y restauración al cerrar modales o salvapantallas
    LaunchedEffect(activeModal, uiState.isScreensaverActive) {
        if (activeModal == "none" && !uiState.isAdminDialogOpen && !uiState.isScreensaverActive) {
            try {
                initialFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Atajo del control remoto '0000' para menú administrador
    LaunchedEffect(Unit) {
        MainActivityEvents.adminDialogRequests.collect {
            viewModel.openAdminDialog()
        }
    }

    // Reseteo del salvapantallas ante cualquier interacción
    LaunchedEffect(Unit) {
        MainActivityEvents.userInteractionEvents.collect {
            viewModel.onUserInteraction()
        }
    }

    // Lanzamiento inmediato y directo de Zapping TV (TV en Vivo)
    val onOpenZapping = remember(activeModal, uiState.isScreensaverActive) {
        {
            if (activeModal == "none" && !uiState.isScreensaverActive) {
                viewModel.launchZappingDirectly()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvTokens.BgDeepDark)
    ) {
        // Fondo sobrio estilo Apple TV: Negro obsidiana profundo con sutil gradiente radial estático (0% carga GPU)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF070E1C),
                            Color(0xFF030710),
                            Color(0xFF01040A)
                        ),
                        radius = 1200f
                    )
                )
        )

        // CONTENEDOR PRINCIPAL CON BARRA SUPERIOR FIJA
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = TvDimens.SafeMarginHorizontal,
                    end = TvDimens.SafeMarginHorizontal,
                    top = TvDimens.SafeMarginTop
                )
        ) {
            // BARRA SUPERIOR FIJA: ESTADO Y MARCA HOSPITALARIA
            LauncherTopHeader(
                currentTime = uiState.currentTimeString,
                currentDate = uiState.currentDateString.ifEmpty { "Mar 6 May" },
                temperature = "${uiState.weather.temperatureC.toInt()}°C",
                selectedLanguage = currentLanguage,
                weatherConditionIcon = uiState.weather.conditionIcon,
                weatherDescription = uiState.weather.conditionDescription,
                onWeatherClick = { activeModal = "weather" },
                onLanguageClick = { activeModal = "language" },
                onAdminRequested = { viewModel.openAdminDialog() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CONTENEDOR VERTICAL NAVEGABLE POR D-PAD
            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = TvDimens.SafeMarginBottom),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

            // ITEM 1: ALERTA DE SALIDA (CHECKOUT DAY - Solo visible desde las 08:30 hasta las 11:00 AM)
            if (uiState.guestStay.isCheckoutDay) {
                val cal = java.util.Calendar.getInstance()
                val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMin = cal.get(java.util.Calendar.MINUTE)
                val isMorningCheckoutWindow = (currentHour == 8 && currentMin >= 30) || (currentHour in 9..10)

                if (isMorningCheckoutWindow) {
                    val alertText = if (currentHour == 10) {
                        val remainingMin = 60 - currentMin
                        "⏳ Quedan $remainingMin minutos para el check-out (11:00 hrs) · ¡Muchas gracias por tu visita!"
                    } else {
                        uiState.guestStay.checkoutAlert ?: "☀️ Hoy es tu día de salida · Horario oficial de entrega: 11:00 hrs"
                    }

                    item {
                        CheckoutAlertBanner(
                            alert = alertText,
                            balance = uiState.guestStay.balanceMessage,
                            isPaid = uiState.guestStay.isPaid
                        )
                    }
                }
            }

            // ITEM 2: HERO CENTRAL (BIENVENIDA 42% + TV PERSISTENTE 58% 16:9)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TvDimens.HeroRowHeight + 12.dp)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.HeroRowSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val realCabinPhoto = uiState.currentCabinDetails?.images?.firstOrNull { 
                        !it.contains("KvkXjb86") && !it.endsWith("/1.jpg") 
                    } ?: uiState.currentCabinDetails?.images?.getOrNull(1)
                      ?: uiState.propertyInfo.heroBgImage.ifBlank { null }

                    WelcomeHeroPanel(
                        unitName = uiState.configuredUnit.name,
                        guestStay = uiState.guestStay,
                        cabinImageUrl = realCabinPhoto,
                        selectedLanguage = currentLanguage,
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        onPayBalance = {
                            activeModal = "payment"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        onDismissCleaningOrWelcomeGuest = {
                            viewModel.activateGuestModeOrMarkClean()
                        }
                    )

                    PersistentLiveTvFrame(
                        onOpenFullscreen = onOpenZapping,
                        isPlaybackActive = !uiState.isScreensaverActive && activeModal == "none",
                        selectedLanguage = currentLanguage,
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    )
                }
            }

            // ITEM 3: CARRUSEL DE ACCESOS DIRECTOS PRINCIPALES (5 CARDS EXACTAS)
            item {
                PrimaryShortcutsCarousel(
                    selectedLanguage = currentLanguage,
                    initialFocusRequester = initialFocusRequester,
                    onShortcutClick = { shortcut ->
                        if (activeModal == "none" && !uiState.isScreensaverActive) {
                            when (shortcut.actionType) {
                                QuickAccessAction.LAUNCH_LIVE_TV -> onOpenZapping()
                                QuickAccessAction.OPEN_STREAMING_MODAL -> activeModal = "streaming"
                                QuickAccessAction.OPEN_STAY_MODAL -> activeModal = "stay"
                                QuickAccessAction.OPEN_TOURIST_GUIDE_MODAL -> activeModal = "tourist_guide"
                                QuickAccessAction.OPEN_RULES_MODAL -> activeModal = "rules"
                            }
                        }
                    }
                )
            }
        }
    }

        // ==============================================================
        // GESTIÓN DE MODALES AISLADOS
        // ==============================================================
        when (activeModal) {
            "payment" -> PaymentDialog(
                stay = uiState.guestStay,
                unitName = uiState.configuredUnit.name,
                paymentUrl = paymentUrl,
                onDismiss = { activeModal = "none" }
            )
            "language" -> LanguageDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { viewModel.setSelectedLanguage(it) },
                onDismiss = { activeModal = "none" }
            )
            "stay" -> MyStayDialog(
                unit = uiState.configuredUnit,
                stay = uiState.guestStay,
                dynamicCabin = uiState.currentCabinDetails,
                propertyInfo = uiState.propertyInfo,
                selectedLanguage = currentLanguage,
                onDismiss = { activeModal = "none" }
            )
            "weather" -> WeatherWeeklyForecastDialog(
                weather = uiState.weather,
                onDismiss = { activeModal = "none" }
            )
            "tourist_guide" -> TouristGuideDialog(
                attractions = uiState.touristAttractions,
                selectedLanguage = currentLanguage,
                onDismiss = { activeModal = "none" }
            )
            "rules" -> PropertyRulesDialog(
                propertyInfo = uiState.propertyInfo,
                selectedLanguage = currentLanguage,
                onDismiss = { activeModal = "none" }
            )
            "roomservice" -> RoomServiceDialog(
                stay = uiState.guestStay,
                unitName = uiState.configuredUnit.name,
                onDismiss = { activeModal = "none" }
            )
            "streaming" -> {
                StreamingAppsDialog(
                    apps = uiState.streamingApps,
                    selectedLanguage = currentLanguage,
                    isSmartDnsEnabled = uiState.isSmartDnsEnabled,
                    onAppClick = { app ->
                        activeModal = "none"
                        viewModel.onAppClicked(app)
                    },
                    onReorderApps = { newOrder ->
                        viewModel.reorderApps(newOrder)
                    },
                    onDismiss = { activeModal = "none" }
                )
            }
            "kiosk_setup" -> ZappingKioskSetupDialog(
                onLaunchZapping = {
                    activeModal = "none"
                    viewModel.setKioskPromptDismissed(true)
                    val zappingApp = uiState.streamingApps.firstOrNull { it.id == "zapping" }
                    if (zappingApp != null) {
                        viewModel.onAppClicked(zappingApp)
                    } else {
                        Toast.makeText(context, "Abriendo Zapping...", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = {
                    activeModal = "none"
                    viewModel.setKioskPromptDismissed(true)
                }
            )
        }

        // Menú de administrador protegido por PIN
        if (uiState.isAdminDialogOpen) {
            val allInstalled by viewModel.allInstalledApps.collectAsStateWithLifecycle()
            AdminSettingsDialog(
                currentUnitId = uiState.configuredUnit.id,
                availableApps = allInstalled,
                initiallySelectedApps = uiState.streamingApps.map { it.packageName }.toSet(),
                onVerifyPin = { pin -> viewModel.verifyAdminPin(pin) },
                isPinLockedOut = { viewModel.isPinLockedOut() },
                getLockoutRemainingSeconds = { viewModel.getPinLockoutRemainingSeconds() },
                onSaveUnit = { unitId -> viewModel.saveConfiguredUnit(unitId) },
                onSaveApps = { selectedPkgs -> viewModel.saveSelectedApps(selectedPkgs) },
                onOpenKioskSetup = { activeModal = "kiosk_setup" },
                isSmartDnsEnabled = uiState.isSmartDnsEnabled,
                smartDnsIp = uiState.smartDnsIp,
                onToggleSmartDns = { enabled -> viewModel.toggleSmartDns(enabled) },
                isUnitForceClean = viewModel.isUnitForceClean(),
                onToggleForceClean = { viewModel.toggleUnitForceClean() },
                onOpenNetworkSettings = { viewModel.openTvNetworkSettings() },
                onCheckUpdateNow = {
                    coroutineScope.launch {
                        Toast.makeText(context, "Verificando actualizaciones en la nube...", Toast.LENGTH_SHORT).show()
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        val currentCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            pInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            pInfo.versionCode.toLong()
                        }
                        val update = OtaUpdateManager.checkUpdate(context, currentCode)
                        if (update != null) {
                            availableUpdate = update
                            updateProgress = null
                            viewModel.closeAdminDialog()
                            Toast.makeText(context, "¡Nueva versión encontrada: v${update.versionName}!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Tu versión v${pInfo.versionName} ya está actualizada al día.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = { viewModel.closeAdminDialog() }
            )
        }

        // Diálogo de Auto-Actualización OTA
        availableUpdate?.let { info ->
            OtaUpdateDialog(
                updateInfo = info,
                downloadProgress = updateProgress,
                onConfirmUpdate = {
                    coroutineScope.launch {
                        updateProgress = 0.01f
                        val success = OtaUpdateManager.downloadAndInstallApk(
                            context = context,
                            apkUrl = info.apkUrl,
                            expectedSha256 = info.sha256,
                            onProgress = { progress ->
                                updateProgress = progress
                            }
                        )
                        if (!success) {
                            updateProgress = null
                            android.widget.Toast.makeText(
                                context,
                                "Error al descargar la actualización. Reintentando...",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onDismiss = {
                    availableUpdate = null
                    updateProgress = null
                }
            )
        }

        // Salvapantallas Ambiental "Lican Ray Paraíso"
        if (uiState.isScreensaverActive) {
            AmbientScreensaver(
                unitName = uiState.configuredUnit.name,
                currentTime = uiState.currentTimeString,
                currentDate = uiState.currentDateString,
                temperatureText = "${uiState.weather.conditionIcon} ${uiState.weather.temperatureC.toInt()}°C · ${uiState.weather.conditionDescription}",
                modifier = Modifier.fillMaxSize(),
                onDismiss = { viewModel.dismissScreensaver() }
            )
        }
    }
}
