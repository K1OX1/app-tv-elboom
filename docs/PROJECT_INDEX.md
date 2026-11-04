# PROJECT INDEX

> **Punto de Entrada Rápido para IA y Desarrolladores**  
> *Alojamientos El Boom Lican Ray - Android TV Launcher Kiosk*

Este índice permite localizar de inmediato el archivo responsable de cualquier funcionalidad o elemento visual sin tener que escanear todo el proyecto.

---

## 1. Entry Points
- `app/src/main/java/cl/elboomlicanray/tvlauncher/MainActivity.kt`: Ciclo de vida Android, bloqueo kiosk Back, atajo `0000`, anti-rebote D-Pad.
- `app/src/main/java/cl/elboomlicanray/tvlauncher/TvLauncherApp.kt`: Inicialización Application Hilt y Coil ImageLoader.

---

## 2. Configuración Centralizada (Datos Editables)
Ruta: `app/src/main/java/cl/elboomlicanray/tvlauncher/config/`
- `NavigationConfig.kt`: Pestañas del header superior (Inicio, Apps, Mi estancia, Servicios, Información).
- `QuickAccessConfig.kt`: Atajos de la fila central (TV en vivo, Apps, Mi estancia, Servicios, Info, Mapa).
- `StreamingAppsConfig.kt`: Catálogo de apps de streaming (Zapping, Netflix, YouTube, Spotify, Max, etc.).
- `HotelServicesConfig.kt`: Servicios de la cabaña (Leña, Tinajas, Desayuno, Wi-Fi).

---

## 3. Design Tokens & Responsive
Ruta: `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/theme/`
- `TvTokens.kt`: Colores, bordes de foco, radio de esquinas, halos, elevación, animaciones D-Pad.
- `TvDimens.kt`: Medidas de tarjetas (AppCard, ShortcutCard), márgenes seguros (Safe Areas).
- `TvResponsive.kt`: Breakpoints para resoluciones de TV (720p, 1080p, 4K).

---

## 4. UI Sections (Composición Home)
Ruta: `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/sections/`
- `header/LauncherTopHeader.kt`: Barra superior con marca del hotel, reloj en vivo, clima y Wi-Fi.
- `header/HeaderNavPill.kt`: Botones píldora de navegación interactiva superior.
- `hero/WelcomeHeroPanel.kt`: Tarjeta izquierda de bienvenida (Nombre huésped, cabaña, lema).
- `hero/PersistentLiveTvFrame.kt`: Reproductor 16:9 persistente de TV en vivo con ExoPlayer HLS.
- `shortcuts/PrimaryShortcutsCarousel.kt`: Carrusel horizontal de accesos rápidos (`TvLazyRow`).
- `apps/FeaturedAppsCarousel.kt`: Carrusel horizontal de aplicaciones destacadas (`TvLazyRow`).

---

## 5. UI Components Reutilizables
Ruta: `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/components/`
- `cards/PrimaryShortcutCard.kt`: Tarjeta individual de acceso directo (230x125 dp).
- `cards/FeaturedAppCard.kt`: Tarjeta individual de aplicación con foco y logo (160x90 dp).
- `cards/AppleTvAppCard.kt`: Tarjeta individual usada en el modal de catálogo de apps.
- `feedback/CheckoutAlertBanner.kt`: Píldora de alerta de salida y saldo pendiente.
- `icons/TvIcons.kt`: Iconos vectoriales nativos en Canvas (cero emojis).
- `qr/QrCodeView.kt`: Generador visual de códigos QR para Wi-Fi y WhatsApp.
- `screensaver/AmbientScreensaver.kt`: Salvapantallas con reloj ambiental tras inactividad.

---

## 6. Modales y Diálogos
Ruta: `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/modals/`
- `base/BaseHotelDialog.kt`: Contenedor base de diálogo TV con foco en botón cerrar.
- `base/DialogItemRow.kt`: Fila interactiva para listas de servicios o información.
- `stay/MyStayDialog.kt`: Información de estancia (Nobeds API, camas, parking, cobros).
- `apps/StreamingAppsDialog.kt`: Modal con todas las apps de streaming disponibles.
- `kiosk/ZappingKioskSetupDialog.kt`: Guía paso a paso para servicio de accesibilidad kiosco.
- `services/HotelServiceDialogs.kt`: Room service, restaurante, tinajas/spa, actividades.
- `info/HotelInfoDialogs.kt`: Clave Wi-Fi Starlink, mapa, notificaciones, traslados, playas.
- `admin/AdminSettingsDialog.kt`: Selector de Cabaña N°1 a 12 con PIN de seguridad '0000'.

---

## 7. State & ViewModel
Ruta: `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/home/`
- `HomeScreen.kt`: Orquestador principal de pantalla (sólo composición limpia).
- `HomeViewModel.kt`: Lógica de sincronización periódica (Nobeds, Open-Meteo, reloj, rotación 12:00).
- `HomeUiState.kt`: Estado inmutable de la pantalla.
- `MainActivityEvents.kt`: Bus de eventos para PIN 0000 e interacciones de usuario.

---

## 8. Integraciones y Servicios Android
Rutas: `data/` y `service/`
- `data/helper/AppLauncherHelper.kt`: Caché de intents, ejecución de Leanback TV intents y fallback Play Store.
- `data/helper/HotelPreferencesHelper.kt`: SharedPreferences (Cabaña configurada, PIN, API URL).
- `data/helper/KioskPermissionHelper.kt`: Comprobación de servicio de accesibilidad y auto-enable root.
- `data/repository/HotelTvRepository.kt`: Cliente HTTP Nobeds API, CMS Dinámico y Open-Meteo.
- `data/update/OtaUpdateManager.kt`: Gestor de actualizaciones automáticas OTA con validación SHA-256.
- `service/ZappingKioskAccessibilityService.kt`: Servicio de accesibilidad para re-mapeo D-Pad en Zapping TV.
- `service/KioskShellCommander.kt`: Ejecución de comandos root para TV sticks preparados.

---

## 9. Documentación y Contratos de API Externa
- `docs/API_ELBOOM.md`: Especificación técnica exhaustiva de todas las consultas HTTP a `https://elboom.cl` (`/api/hotel-tv`, `/api/accommodations`, `/api/tourist-guide`, `/api/property`, `/api/hotel-tv/send-attraction`, `/tv/version.json`), tokens de cabaña, schemas JSON de respuesta/petición y prompt listo para IAs backend.
- `docs/ARCHITECTURE.md`: Principios arquitectónicos, responsabilidades modulares y diseño 10-Foot UI.
- `docs/TV_PERFORMANCE.md`: Auditoría de consumo de RAM, aceleración GPU y optimización para hardware 1GB/2GB.
