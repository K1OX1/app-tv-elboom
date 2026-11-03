package cl.elboomlicanray.tvlauncher.service

/**
 * Estado en memoria global para coordinar el Launcher con el Servicio de Accesibilidad.
 */
object KioskState {
    @Volatile
    var isZappingForeground: Boolean = false
}
