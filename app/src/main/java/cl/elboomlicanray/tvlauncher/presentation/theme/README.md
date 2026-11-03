# Módulo Theme & Tokens (`cl.elboomlicanray.tvlauncher.presentation.theme`)

## Propósito
Centralizar todas las constantes de diseño, paleta cromática, bordes de foco D-Pad, elevaciones, radios de esquina, escalas de animación y métricas de pantalla (1080p Android TV).

## Archivos del Módulo
- `Color.kt`: Definición base de colores HEX Compose (`TvColor`).
- `TvTokens.kt`: Tokens semánticos de la aplicación (colores de fondo, foco dorado `#E0A96D`, bordes, sombras, alphas y `TvMotionTokens`).
- `TvDimens.kt`: Medidas de layout, dimensiones fijas de tarjetas (`PrimaryShortcutWidth = 230.dp`, `PrimaryShortcutHeight = 125.dp`, `FeaturedAppWidth = 160.dp`, `FeaturedAppHeight = 90.dp`), espaciados y padding de seguridad TV (Overscan 42dp horiz, 36dp vert).
- `TvResponsive.kt`: Categorización de pantallas y multiplicadores de escala para resoluciones 720p, 1080p y 4K.
- `Theme.kt`: Wrapper de tema Material.
- `Type.kt`: Configuración tipográfica.

## Qué DEBE colocarse aquí
- Nuevos colores corporativos o de estado.
- Ajustes de dimensiones, paddings o radios de tarjetas.
- Ajustes de la escala de zoom en foco D-Pad (`TvMotionTokens.FocusScale = 1.05f`).

## Qué NO debe colocarse aquí
- Composables visuales completos o tarjetas.
- Lógica de eventos de usuario o clicks.
- Strings traducibles o textos de contenido.
