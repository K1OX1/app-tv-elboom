# HISTORIAL DE MODIFICACIONES (CHANGELOG)

---

## 2026-10-06 · Reestructuración y Modularización Arquitectónica

### Objetivo
Desacoplar completamente el proyecto Android TV para permitir modificaciones quirúrgicas inmediatas con mínimo contexto requerido por IA y desarrolladores, eliminando archivos monolíticos y preservando al 100% el diseño visual y las funcionalidades operativas.

### Archivos Creados
- `config/NavigationConfig.kt`: Configuración centralizada de las pestañas superiores del Launcher.
- `config/QuickAccessConfig.kt`: Catálogo de los 6 accesos directos principales con acciones desacopladas.
- `config/StreamingAppsConfig.kt`: Catálogo de plataformas de streaming con colores de marca y paquetes.
- `config/HotelServicesConfig.kt`: Catálogo de servicios de hotel (leña, tinajas, desayuno, etc.).
- `presentation/theme/TvTokens.kt`: Design Tokens unificados (colores, bordes de foco, curvaturas, motion).
- `presentation/theme/TvDimens.kt`: Dimensiones fijas de tarjetas y márgenes seguros de TV (Safe Area).
- `presentation/theme/TvResponsive.kt`: Breakpoints adaptativos para 720p, 1080p y 4K.
- `presentation/components/cards/PrimaryShortcutCard.kt`: Componente modular para tarjetas de acceso directo (230x125 dp).
- `presentation/components/cards/FeaturedAppCard.kt`: Componente modular para tarjetas de apps destacadas (160x90 dp).
- `presentation/components/feedback/CheckoutAlertBanner.kt`: Píldora de aviso de checkout y balance pendiente.
- `presentation/sections/header/LauncherTopHeader.kt` y `HeaderNavPill.kt`: Cabecera modular completa.
- `presentation/sections/hero/WelcomeHeroPanel.kt`: Tarjeta de bienvenida con cabaña y lema.
- `presentation/sections/hero/PersistentLiveTvFrame.kt`: Reproductor de TV en vivo ExoPlayer HLS aislado.
- `presentation/sections/shortcuts/PrimaryShortcutsCarousel.kt`: Carrusel horizontal de accesos rápidos.
- `presentation/sections/apps/FeaturedAppsCarousel.kt`: Carrusel horizontal de apps destacadas.
- `presentation/modals/base/BaseHotelDialog.kt`: Contenedor base de diálogos modales.
- `presentation/modals/base/DialogItemRow.kt`: Fila interactiva para listas de servicios.
- `presentation/modals/stay/MyStayDialog.kt`: Diálogo de detalles de estancia del huésped (Nobeds).
- `presentation/modals/apps/StreamingAppsDialog.kt`: Diálogo de catálogo completo de streaming.
- `presentation/modals/kiosk/ZappingKioskSetupDialog.kt`: Diálogo de configuración de modo kiosco.
- `presentation/modals/services/HotelServiceDialogs.kt`: Diálogos de Room Service, Restaurante, Spa y Actividades.
- `presentation/modals/info/HotelInfoDialogs.kt`: Diálogos de Wi-Fi, Mapa, Notificaciones, Agenda y Traslados.
- `docs/PROJECT_INDEX.md`, `docs/CHANGE_GUIDE.md`, `docs/ARCHITECTURE.md`, `docs/DEPENDENCIES.md`, `docs/CRITICAL_FILES.md`, `docs/ARCHITECTURE_SUMMARY.md`.

### Archivos Modificados
- `presentation/home/HomeScreen.kt`: Reducido de 1,339 líneas a ~230 líneas de composición limpia.
- `data/repository/AppRepository.kt`: Desacoplado para consumir `StreamingAppsConfig` y `HotelServicesConfig`.
- `gradlew.bat`: Corregido script del wrapper para permitir ejecución nativa de Gradle con JBR 21.

### Archivos Eliminados (Código Huérfano / Monolítico)
- `presentation/home/HotelModals.kt` (1,329 líneas sustituidas por submódulos en `presentation/modals/`).
- `presentation/components/LiveTvPipBanner.kt` (código huérfano reemplazado por `PersistentLiveTvFrame`).
- `presentation/components/TvCardItem.kt` (código huérfano reemplazado por `PrimaryShortcutCard`).
- `presentation/components/TvServiceCardItem.kt` (código huérfano).
- `presentation/components/LiquidGlassContainer.kt` (código huérfano).

### No Modificados (Preservación de Lógica y Hardware)
- `MainActivity.kt`
- `HomeViewModel.kt`
- `HomeUiState.kt`
- `MainActivityEvents.kt`
- `data/helper/AppLauncherHelper.kt`
- `data/helper/HotelPreferencesHelper.kt`
- `data/helper/KioskPermissionHelper.kt`
- `service/ZappingKioskAccessibilityService.kt`
