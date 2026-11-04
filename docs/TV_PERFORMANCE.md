# Validación en televisores de 1–2 GB

## Cambios

- TV de inicio: un único efecto decide cuándo preparar el canal. No reproduce con un modal abierto ni con la actividad pausada. Un solo intento de canal alternativo por sesión evita bucles de reconexión.
- Media3: buffer inicial 750 ms, recuperación 1.500 ms, ventana 3–10 segundos, sin buffer hacia atrás. Presupuesto de muestras: 4 MiB para la TV de inicio; 8 MiB para videos en equipos de hasta 2 GiB o marcados low-RAM; 16 MiB en otros equipos. No es un límite de memoria total del reproductor.
- Selección de pistas: preferencia máxima de 480p en la vista previa y 720p/30 fps en recorridos. Si el origen ofrece una única pista, estos parámetros no convierten ni reducen su resolución.
- Imágenes: caché de 12 MiB en televisores pequeños y 24 MiB en otros; sin fundido global. El fondo del alojamiento se solicita a 1280 × 720.
- Sin llamadas explícitas a System.gc al cerrar videos o recibir presión de memoria.
- Videos y WebView se pausan al salir de la actividad. Se destruye WebView al cerrar el recorrido.

## Recorridos directos

El objeto de cada alojamiento en `/api/accommodations` admite el campo opcional `videoUrl`, con una URL HTTP(S) de MP4 o HLS. Tiene prioridad sobre `youtubeVideoId`; si falta, se conserva el comportamiento anterior. Este cambio no modifica el servidor ni carga archivos de video.

Para equipos limitados, servir H.264/AAC, hasta 720p y 30 fps, con MP4 preparado para inicio progresivo (faststart) o HLS con variantes. No pasar una página HTML como video directo.

YouTube integrado conserva su selección automática de calidad; su API ya no admite setPlaybackQuality. Los presupuestos de Media3 no se aplican al navegador de YouTube.

## Prueba pendiente en hardware real

1. Usar el mismo televisor, red y video para comparar versión anterior y nueva. Registrar 5 aperturas en frío y 5 en caliente, desde pulsar Reproducir hasta el primer fotograma.
2. Registrar memoria del proceso con `adb shell dumpsys meminfo cl.elboomlicanray.tvlauncher`: inicio, alojamiento, video y después de 10 ciclos de abrir/cerrar. Separar la memoria de procesos WebView.
3. Abrir alojamiento, fotos, camas y video con el control remoto. Confirmar foco visible, regreso con Atrás y textos sin recortes a 720p y 1080p.
4. Ir a Inicio durante el video y regresar. No debe sonar la TV de fondo ni superponerse al recorrido.
5. Desconectar la red: confirmar que el canal alternativo no entra en un bucle. Los recorridos todavía dependen del manejo de errores del reproductor correspondiente.

No se ha medido una mejora porcentual ni se ha certificado el consumo total en una TV física de 1 GB.

## Verificación de esta revisión

- assembleDebug completado y APK instalado en el emulador. Inicio, TV en vivo y apertura de Mi alojamiento comprobados; falta certificación en TV física.
- lintDebug detecta tres errores RestrictedApi en MainActivity.dispatchKeyEvent (declaración y llamada a super), presentes en el código anterior.
