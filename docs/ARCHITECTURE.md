# ARQUITECTURA DEL SISTEMA ANDROID TV

> **Alojamientos El Boom Lican Ray · Smart TV Launcher**  
> Arquitectura Modular para 10-Foot UI (Jetpack Compose para TV / Leanback)

---

## 1. Visión y Principios Arquitectónicos

1. **Separación Estricta de Responsabilidades (Single Responsibility Principle):**
   - Presentación, Lógica, Datos, Configuración y Estilos viven en capas independientes.
   - La pantalla `HomeScreen.kt` actúa exclusivamente como **Orquestador** de alto nivel (~150 líneas).
2. **Desacoplamiento de Datos y Presentación:**
   - La lista de aplicaciones, servicios, atajos y navegación residen en el paquete `config/`.
   - Modificar textos o catálogos no requiere alterar la lógica de renderizado.
3. **Design Tokens Centralizados:**
   - Colores, curvaturas, bordes de foco D-Pad y medidas de tarjetas están unificados en `presentation/theme/TvTokens.kt` y `presentation/theme/TvDimens.kt`.
4. **Reglas Absolutas 10-Foot UI:**
   - **El contenido determina el tamaño:** Cero `TextOverflow.Ellipsis`, cero compresión forzada, dimensiones holgadas en todas las tarjetas.
   - **Scroll bidireccional suave:** Carruseles horizontales (`TvLazyRow`) anidados en una columna vertical con márgenes seguros (`TvLazyColumn`).

---

## 2. Mapa de Capas del Proyecto

```
app/src/main/java/cl/elboomlicanray/tvlauncher/
│
├── config/                          <-- DATOS EDITABLES Y CONFIGURACIONES
│   ├── NavigationConfig.kt          (Menú de navegación superior)
│   ├── QuickAccessConfig.kt         (Atajos de la fila principal)
│   ├── StreamingAppsConfig.kt       (Catálogo de apps de streaming)
│   └── HotelServicesConfig.kt       (Servicios ofrecidos en cabaña)
│
├── presentation/
│   ├── theme/                       <-- DESIGN TOKENS Y RESPONSIVE
│   │   ├── Color.kt                 (Paleta de color base)
│   │   ├── TvTokens.kt              (Design Tokens: Colores TV, Bordes, Foco, Motion)
│   │   ├── TvDimens.kt              (Dimensiones de tarjetas y márgenes seguros)
│   │   ├── TvResponsive.kt          (Breakpoints para 720p / 1080p / 4K)
│   │   └── Type.kt                  (Tipografía)
│   │
│   ├── components/                  <-- COMPONENTES BASE
│   │   ├── cards/                   (PrimaryShortcutCard, FeaturedAppCard, AppleTvAppCard)
│   │   ├── feedback/                (CheckoutAlertBanner)
│   │   ├── icons/                   (TvIcons en Canvas nativo)
│   │   ├── qr/                      (QrCodeView)
│   │   └── screensaver/             (AmbientScreensaver)
│   │
│   ├── sections/                    <-- SECCIONES MODULARES DEL HOME
│   │   ├── header/                  (LauncherTopHeader, HeaderNavPill)
│   │   ├── hero/                    (WelcomeHeroPanel, PersistentLiveTvFrame)
│   │   ├── shortcuts/               (PrimaryShortcutsCarousel)
│   │   └── apps/                    (FeaturedAppsCarousel)
│   │
│   ├── modals/                      <-- DIÁLOGOS MODULARES
│   │   ├── base/                    (BaseHotelDialog, DialogItemRow)
│   │   ├── stay/                    (MyStayDialog)
│   │   ├── apps/                    (StreamingAppsDialog)
│   │   ├── kiosk/                   (ZappingKioskSetupDialog)
│   │   ├── services/                (HotelServiceDialogs: RoomService, Restaurant, Spa, Activities)
│   │   ├── info/                    (HotelInfoDialogs: Wifi, Map, Notifications, Agenda, Transfers)
│   │   └── admin/                   (AdminSettingsDialog con PIN '0000')
│   │
│   └── home/                        <-- ORQUESTACIÓN DE PANTALLA
│       ├── HomeScreen.kt            (Composición limpia)
│       ├── HomeViewModel.kt         (Lógica de negocio y sincronización Nobeds)
│       ├── HomeUiState.kt           (Estado inmutable)
│       └── MainActivityEvents.kt    (Eventos de D-Pad y atajos de control remoto)
│
├── domain/model/                    <-- MODELOS DE DOMINIO INMUTABLES
│   ├── AccommodationUnit            (Catálogo físico de Cabañas N°1 a 12)
│   ├── GuestStayInfo                (Datos de reserva sincronizados con Nobeds)
│   ├── AppItem                      (Representación de aplicaciones)
│   └── QuickServiceItem             (Representación de servicios de hotel)
│
├── data/
│   ├── repository/                  (HotelTvRepository, AppRepository)
│   └── helper/                      (AppLauncherHelper, HotelPreferencesHelper, KioskPermissionHelper)
│
└── service/                         <-- SERVICIOS ANDROID KIOSK
    ├── ZappingKioskAccessibilityService.kt
    ├── KioskShellCommander.kt
    └── KioskState.kt
```

---

## 3. Flujo de Navegación y Foco (D-Pad Focus Management)

1. **Inicio de la aplicación:**
   - `HomeScreen.kt` solicita foco automático en la primera tarjeta (`FocusRequester` inicial en "TV en vivo").
2. **Navegación Vertical:**
   - El contenedor `TvLazyColumn` gestiona el foco entre el Header superior, el banner de checkout (si aplica), la fila Hero, los accesos rápidos y las apps destacadas.
3. **Navegación Horizontal:**
   - Los carruseles `TvLazyRow` desplazan suavemente las tarjetas garantizando que la tarjeta activa reciba la escala (`1.04f` a `1.05f`), el borde Cyan iluminado y el halo de sombra.
4. **Modales:**
   - Al pulsar Enter/D-Pad Center sobre cualquier tarjeta de acceso, se abre el diálogo correspondiente de `presentation/modals/`.
   - `BaseHotelDialog` enfoca de inmediato el botón de cierre para navegación fluida con una sola mano.

---

## 4. Integración de TV en Vivo (ExoPlayer HLS)

- El componente `PersistentLiveTvFrame.kt` crea una instancia liviana de `ExoPlayer` optimizada para TV de gama baja:
  - Bitrate máximo acotado (1.2 Mbps a 480p/720p).
  - Buffer mínimo adaptativo (1 a 3 segundos) para arranque instantáneo.
  - Liberación segura del hardware `MediaCodec` en `onPause` y `onStop` de la Activity para no bloquear aplicaciones externas como Zapping o YouTube.
