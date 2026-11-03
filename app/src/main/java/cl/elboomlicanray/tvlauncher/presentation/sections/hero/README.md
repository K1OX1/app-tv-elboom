# Módulo Sections / Hero (`cl.elboomlicanray.tvlauncher.presentation.sections.hero`)

## Propósito
Alojar los dos paneles prominentes superiores de la pantalla principal: el panel de bienvenida al huésped y el visor persistente de TV en vivo (16:9).

## Archivos del Módulo
- `WelcomeHeroPanel.kt`: Tarjeta izquierda de bienvenida que muestra el nombre del complejo ("Alojamientos El Boom Lican Ray"), la unidad/cabaña asignada (sincronizada desde Nobeds API) y el mensaje de cortesía ("Disfruta de una estadía inolvidable junto al lago...").
- `PersistentLiveTvFrame.kt`: Marco derecho 16:9 con ExoPlayer integrado para reproducción de stream HLS en vivo (Zapping TV / feed local), con overlay de canal, estado de foco y acción de pantalla completa.

## Pautas de Mantenimiento
- Para modificar el texto de bienvenida o la frase lema, editar exclusivamente `WelcomeHeroPanel.kt`.
- Para cambiar la URL de streaming, buffers de ExoPlayer o relación de aspecto de la TV, editar exclusivamente `PersistentLiveTvFrame.kt`.
- Ambos componentes son independientes y se colocan en `HomeScreen.kt` mediante un `Row` con peso equilibrado.
