# Módulo Config (`cl.elboomlicanray.tvlauncher.config`)

## Propósito
Centralizar todas las listas estáticas, metadatos, catálogos de contenido y configuraciones del launcher en Kotlin puro. Ningún componente visual debe tener listas de aplicaciones o accesos directos "hardcodeados".

## Archivos del Módulo
- `NavigationConfig.kt`: Pestañas del header superior (`topNavItems`) e identificadores de navegación.
- `QuickAccessConfig.kt`: Atajos de la fila principal (`defaultShortcuts`, `QuickAccessItem`, `QuickAccessAction`).
- `StreamingAppsConfig.kt`: Catálogo de apps de streaming (`defaultStreamingApps`, `StreamingAppConfig`).
- `HotelServicesConfig.kt`: Catálogo de servicios de cabañas (`defaultCabinServices`, `HotelServiceConfig`).

## Qué DEBE colocarse aquí
- Catálogos de apps y paquetes Android (Package names).
- Enlaces de streaming / deep links.
- Títulos, descripciones y tipos de acción de accesos directos.
- Listas de opciones de menú o navegación estática.

## Qué NO debe colocarse aquí
- Código UI / Composables de Jetpack Compose (`@Composable`).
- Manejo de estados (`remember`, `mutableStateOf`).
- Lógica de red, Retrofit o llamadas a APIs (eso pertenece a `data/`).
- Lógica de lanzamiento de Intents o ExoPlayer (eso pertenece a `presentation/` o `MainActivity`).
