# DEPENDENCIAS DEL SISTEMA (DEPENDENCY TREE)

Este documento detalla la jerarquía de dependencias entre componentes, módulos y servicios del proyecto.

---

## 1. Árbol de Composición UI

```
MainActivity
 └── HomeScreen
      ├── LauncherTopHeader
      │    ├── NavigationConfig (Config)
      │    ├── HeaderNavPill (UI)
      │    └── TvIcons (Canvas)
      │
      ├── CheckoutAlertBanner (Feedback UI)
      │
      ├── Hero Section
      │    ├── WelcomeHeroPanel (UI)
      │    │    └── TvIcons (Canvas)
      │    └── PersistentLiveTvFrame (ExoPlayer HLS)
      │
      ├── PrimaryShortcutsCarousel (TvLazyRow)
      │    ├── QuickAccessConfig (Config)
      │    └── PrimaryShortcutCard (Card UI)
      │         └── TvIcons (Canvas)
      │
      ├── FeaturedAppsCarousel (TvLazyRow)
      │    └── FeaturedAppCard (Card UI)
      │
      └── Modales (Diálogos independientes)
           ├── MyStayDialog -> BaseHotelDialog
           ├── StreamingAppsDialog -> AppleTvAppCard
           ├── ZappingKioskSetupDialog -> DialogItemRow
           ├── HotelServiceDialogs -> BaseHotelDialog, QrCodeView
           └── HotelInfoDialogs -> BaseHotelDialog, QrCodeView
```

---

## 2. Dependencias de Estado y Datos

```
HomeScreen
 └── HomeViewModel
      ├── AppRepository
      │    ├── StreamingAppsConfig
      │    └── HotelServicesConfig
      │
      ├── HotelTvRepository
      │    ├── HotelPreferencesHelper (SharedPreferences)
      │    ├── Open-Meteo API (HTTP)
      │    └── Nobeds API (HTTP)
      │
      ├── AppLauncherHelper
      │    └── PackageManager (Android TV Leanback Intent Cache)
      │
      └── MainActivityEvents (SharedFlow D-Pad / PIN 0000)
```

---

## 3. Dependencias de Estilos y Tokens

```
Todos los componentes visuales
 ├── TvTokens (Colores, Bordes, Foco, Curvaturas, Motion)
 ├── TvDimens (Dimensiones de tarjetas, Márgenes seguros)
 └── TvResponsive (Adaptación de breakpoints TV)
```
