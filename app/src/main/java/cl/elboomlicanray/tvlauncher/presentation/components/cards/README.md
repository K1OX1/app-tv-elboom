# Módulo Componentes / Cards (`cl.elboomlicanray.tvlauncher.presentation.components.cards`)

## Propósito
Alojar los componentes atómicos reutilizables de tarjetas de la interfaz TV. Cada tarjeta gestiona su propio estado de foco D-Pad, dibujo de bordes, escala interactiva y renderizado de texto sin elipsis.

## Archivos del Módulo
- `PrimaryShortcutCard.kt`: Tarjeta horizontal principal (230×125dp) con icono Canvas vectorial integrado, badge de estado, título legible y descripción completa sin corte de texto.
- `FeaturedAppCard.kt`: Tarjeta compacta de aplicación (160×90dp) con isotipo estilizado Canvas, nombre de app y feedback visual de foco D-Pad.

## Reglas Críticas
1. **NUNCA agregar `TextOverflow.Ellipsis` ni `"..."`**: Si el texto es largo, la tarjeta debe renderizarlo en múltiples líneas (`maxLines = 2` o `3`) con espaciado adecuado.
2. **Dimensiones desacopladas**: Usar siempre las constantes de `TvDimens` (`TvDimens.PrimaryShortcutWidth`, `TvDimens.PrimaryShortcutHeight`, etc.).
3. **Control D-Pad accesible**: Todo item clickeable debe implementar `focusable()` y exponer `onFocus` y `onClick`.

## Qué NO debe colocarse aquí
- Contenedores de fila horizontal o LazyRow (eso pertenece a `sections/`).
- Diálogos modales (eso pertenece a `modals/`).
- Llamadas a APIs o lógica de persistencia.
