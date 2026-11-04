# Revisión de reproducción y recursos — 2026-11-04

## Alcance y límites
Análisis estático Android Lint del módulo completo y búsquedas globales de reproductores, tareas periódicas, trabajo bloqueante, memoria, WebView y red. Revisión manual de reproducción compartida, preview, ciclo de vida, administrador, salvapantallas, preferencias y sincronización. No equivale a probar cada flujo ni certifica ausencia de fallas. No se dispone del TCL físico.

## Cambios
- Integrado por defecto. YouTube externo solo por elección del administrador, persistente y local a cada TV. Si no está disponible, aviso y fallback integrado. URLs directas MP4/HLS mantienen el reproductor nativo independientemente de esta preferencia.
- Preview: libera reproductor, decoder y vista al abrir secciones o abandonar la actividad. Recreación al volver; callbacks antiguos no inician otra reproducción.
- WebView: sin capa GPU forzada adicional; mantiene origen elboom.cl; HTTPS sin contenido mixto; validación exacta de dominios. Callbacks JavaScript se desactivan al cerrar. Detecta falta de progreso durante 30 segundos; ofrece reintento manual, sin bucle de recargas. Maneja terminación del proceso web mostrando error recuperable.
- Videos directos: preferencia 480p/30 fps/1,2 Mbps en poca memoria; buffers temporales ampliados conservando topes de bytes. No transcodifica fuentes fijas.
- Salvapantallas: imágenes limitadas a 1280×720 y sin mezcla de dos fondos en poca memoria. Consume también la liberación de la tecla que lo cierra.
- PIN: elimina espera bloqueante de 250 ms en el hilo de interfaz; conserva comparación de tiempo constante y bloqueo por intentos.
- Lint RestrictedApi: excepción localizada y comentada para override de Activity.dispatchKeyEvent heredado a través de AndroidX; no se desactiva la inspección global.

## Limitaciones y pendientes relevantes
- YouTube IFrame no permite imponer resolución mediante setPlaybackQuality; añadir esa llamada sería ineficaz. Calidad y codecs dependen de YouTube/WebView/dispositivo.
- Para reproducción integrada controlable, publicar videos propios/autorizados en MP4 H.264 + AAC (480p/720p, 30 fps) o HLS con esas variantes y entregar su URL HTTPS en videoUrl. No sustituir el enlace por una página web ni extraer URLs privadas temporales de YouTube.
- Probar en TCL: 10 minutos por video, adelantar/retroceder, cerrar/reabrir, abrir otra sección, salir y volver; probar ambos modos y persistencia tras reinicio.
- Se mantienen avisos de Lint sobre dependencias, recursos/iconos y otras recomendaciones; no se actualizan todas las bibliotecas indiscriminadamente.
- Release todavía no usa reducción R8 y necesita validación propia antes de activarla; APK de prueba debug no es una medición representativa de rendimiento final.
- Revisión adicional de distribución: configuración de firma contiene credenciales locales en Gradle. Migrarlas a secretos de entorno requiere mantener la clave de firma existente para no romper actualizaciones.
- La red/API y las acciones de aseo/WhatsApp no se prueban enviando cambios reales a huéspedes.

## Verificación
- assembleDebug y lintDebug completados correctamente. Lint: 0 errores y 122 avisos.
- Verificadas las anotaciones JavascriptInterface en los tres métodos del puente compilado.
- APK instalado y actividad iniciada en emulador. Pendientes pruebas de fluidez y navegación de los dos modos en TCL físico.
