# GUÍA RÁPIDA DE MODIFICACIÓN (CHANGE GUIDE)

> **Regla de Oro:** Modificar **únicamente** los archivos listados para cada tipo de cambio.  
> No es necesario escanear ni modificar el resto del proyecto.

---

### 🎨 Cambiar Colores Globales, Bordes o Foco D-Pad
- **Archivo principal:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/theme/TvTokens.kt`
- **Variables clave:**
  - Color de foco D-Pad: `TvTokens.BorderFocused`, `TvTokens.FocusGlowColor`
  - Color Cyan institucional: `TvTokens.CyanBrand`, `TvTokens.CyanSubtle`
  - Fondos de tarjetas: `TvTokens.CardSurface`, `TvTokens.CardSurfaceFocused`
  - Radios de curvatura: `TvTokens.RadiusCard`, `TvTokens.RadiusModal`

---

### 📏 Cambiar Tamaño de Tarjetas o Márgenes de TV
- **Archivo principal:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/theme/TvDimens.kt`
- **Variables clave:**
  - Tarjetas de apps destacadas: `TvDimens.FeaturedAppCardWidth`, `TvDimens.FeaturedAppCardHeight`
  - Tarjetas de accesos directos: `TvDimens.ShortcutCardWidth`, `TvDimens.ShortcutCardHeight`
  - Altura del Hero (TV y Bienvenida): `TvDimens.HeroRowHeight`
  - Márgenes seguros (Safe Area): `TvDimens.SafeMarginHorizontal`, `TvDimens.SafeMarginBottom`

---

### 📱 Cambiar Tarjeta de App Destacada (Visual)
- **Componente:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/components/cards/FeaturedAppCard.kt`
- **Carrusel:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/sections/apps/FeaturedAppsCarousel.kt`

---

### 🚀 Añadir, Quitar o Modificar Apps de Streaming
- **Archivo de datos:** `app/src/main/java/cl/elboomlicanray/tvlauncher/config/StreamingAppsConfig.kt`
  - Para cambiar paquete, nombre o colores de una app de streaming, edite `defaultStreamingApps`.

---

### 🏠 Cambiar Texto o Apariencia de Bienvenida
- **Componente:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/sections/hero/WelcomeHeroPanel.kt`
  - Para cambiar el lema ("Naturaleza, descanso y hospitalidad") o la foto ambiental de fondo.

---

### 📺 Cambiar la TV en Vivo (Canales, Stream o Player)
- **Componente:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/sections/hero/PersistentLiveTvFrame.kt`
  - Para cambiar la lista `tvChannels` (nombres de canales, números y URLs HLS m3u8).

---

### 🧭 Cambiar Pestañas del Menú Superior (Header)
- **Archivo de configuración:** `app/src/main/java/cl/elboomlicanray/tvlauncher/config/NavigationConfig.kt`
  - Edite la lista `topNavItems` para agregar o cambiar pestañas.
- **Componente Header:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/sections/header/LauncherTopHeader.kt`

---

### ⚡ Cambiar Atajos Principales (TV en vivo, Apps, Estancia, etc.)
- **Archivo de configuración:** `app/src/main/java/cl/elboomlicanray/tvlauncher/config/QuickAccessConfig.kt`
  - Edite la lista `defaultShortcuts` para reordenar o cambiar títulos/subtítulos.
- **Componente Tarjeta:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/components/cards/PrimaryShortcutCard.kt`

---

### 🛎️ Cambiar Servicios de la Cabaña (Leña, Desayuno, etc.)
- **Configuración de servicios:** `app/src/main/java/cl/elboomlicanray/tvlauncher/config/HotelServicesConfig.kt`
- **Diálogos de servicios:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/modals/services/HotelServiceDialogs.kt`

---

### 📶 Cambiar Clave de Wi-Fi o Diálogo de Wi-Fi
- **Diálogo:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/modals/info/HotelInfoDialogs.kt` (`WifiInfoDialog`)
- **Datos por defecto:** `app/src/main/java/cl/elboomlicanray/tvlauncher/data/repository/HotelTvRepository.kt`

---

### 🔑 Cambiar PIN de Administrador o Gestión de Cabañas
- **PIN y preferencias:** `app/src/main/java/cl/elboomlicanray/tvlauncher/data/helper/HotelPreferencesHelper.kt`
- **Catálogo de Cabañas:** `app/src/main/java/cl/elboomlicanray/tvlauncher/domain/model/HotelTvModels.kt` (`OFFICIAL_ACCOMMODATION_UNITS`)
- **Diálogo Administrador:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/home/AdminSettingsDialog.kt`

---

### 🔒 Cambiar Modo Kiosco y Protección de Control Remoto
- **Servicio:** `app/src/main/java/cl/elboomlicanray/tvlauncher/service/ZappingKioskAccessibilityService.kt`
- **Diálogo de activación:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/modals/kiosk/ZappingKioskSetupDialog.kt`
- **Permisos y root:** `app/src/main/java/cl/elboomlicanray/tvlauncher/data/helper/KioskPermissionHelper.kt`

---

### 🖥️ Cambiar Breakpoints de Pantallas TV (720p, 1080p, 4K)
- **Archivo:** `app/src/main/java/cl/elboomlicanray/tvlauncher/presentation/theme/TvResponsive.kt`
