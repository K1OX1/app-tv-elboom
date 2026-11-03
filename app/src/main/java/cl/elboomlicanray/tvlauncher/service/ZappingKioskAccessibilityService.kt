package cl.elboomlicanray.tvlauncher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import cl.elboomlicanray.tvlauncher.MainActivity

/**
 * Servicio de Accesibilidad para Control Remoto Kiosco (Cabañas El Boom Lican Ray).
 *
 * Filtra y re-mapea teclas físicas a nivel de sistema operativo exclusivamente
 * cuando el huésped se encuentra dentro de la aplicación Zapping TV:
 * 1. Teclas de Volumen (+, -, Mute): Pasan siempre al sistema de audio de la TV.
 * 2. Flechas Izquierda y Derecha: 100% bloqueadas (previene acceso a menús y configuración).
 * 3. Flechas Arriba y Abajo: Re-mapeadas a Channel Up y Channel Down (con fallback nativo).
 * 4. Botón Atrás (Back): Actúa como Home, regresando al huésped de inmediato a nuestro Launcher.
 */
class ZappingKioskAccessibilityService : AccessibilityService() {

    private var currentForegroundPackage: String = ""
    private var lastChannelSwitchTime = 0L
    private val CHANNEL_COOLDOWN_MS = 250L

    companion object {
        private const val TAG = "KioskService"

        private val ZAPPING_IDENTIFIERS = listOf(
            "com.eltelon.zapping",
            "com.zappingtv.tv",
            "com.zapping.tv",
            "com.zapping.android",
            "com.zappingtv",
            "zapping"
        )
    }

    /**
     * CONFIGURACIÓN DINÁMICA VITAL:
     * En Android, para que el sistema entregue eventos de hardware a onKeyEvent(),
     * es ESTRICTAMENTE OBLIGATORIO asignar serviceInfo en tiempo de ejecución con
     * la bandera FLAG_REQUEST_FILTER_KEY_EVENTS.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                notificationTimeout = 50
            }
            serviceInfo = info
            Log.i(TAG, "ZappingKioskAccessibilityService CONECTADO EXITOSAMENTE con FLAG_REQUEST_FILTER_KEY_EVENTS")
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando serviceInfo", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg.isNotEmpty()) {
            val isZappingPkg = ZAPPING_IDENTIFIERS.any { pkg.contains(it, ignoreCase = true) }

            if (isZappingPkg) {
                KioskState.isZappingForeground = true
                currentForegroundPackage = pkg
            } else if (pkg.contains("elboomlicanray", ignoreCase = true) ||
                pkg.contains("launcher", ignoreCase = true) ||
                pkg.contains("netflix", ignoreCase = true) ||
                pkg.contains("youtube", ignoreCase = true)
            ) {
                KioskState.isZappingForeground = false
                currentForegroundPackage = pkg
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "ZappingKioskAccessibilityService interrumpido")
    }

    /**
     * Interceptación global de eventos de hardware del control remoto.
     * Retornar `true` destruye el evento (la app de Zapping nunca lo recibe).
     * Retornar `false` permite que el evento fluya normalmente.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // En Android TV, consultamos la ventana interactiva actual y el estado
        val topPkg = rootInActiveWindow?.packageName?.toString() ?: currentForegroundPackage
        val isZapping = KioskState.isZappingForeground ||
                ZAPPING_IDENTIFIERS.any { topPkg.contains(it, ignoreCase = true) }

        // Si NO estamos dentro de Zapping, el control remoto opera 100% normal (en Netflix, YouTube o Launcher)
        if (!isZapping) {
            return super.onKeyEvent(event)
        }

        Log.d(TAG, "onKeyEvent Zapping: keyCode=${event.keyCode}, action=${event.action}")

        // ==============================================================
        // REGLAS ESTRICTAS DENTRO DE ZAPPING (MODO KIOSCO HOTELERO)
        // ==============================================================
        when (event.keyCode) {
            // 1. VOLUMEN: Permitir siempre que el huésped controle el sonido
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                return false // Pasa normal a la TV
            }

            // 2. FLECHA ARRIBA: Convertir a Channel Up (Código 166) para cambio directo sin menús
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastChannelSwitchTime >= CHANNEL_COOLDOWN_MS) {
                        lastChannelSwitchTime = now
                        KioskShellCommander.sendKeyEvent(KeyEvent.KEYCODE_CHANNEL_UP)
                    }
                }
                return true // Consume la flecha para saltarse el menú de Zapping
            }

            // 3. FLECHA ABAJO: Convertir a Channel Down (Código 167) para cambio directo sin menús
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastChannelSwitchTime >= CHANNEL_COOLDOWN_MS) {
                        lastChannelSwitchTime = now
                        KioskShellCommander.sendKeyEvent(KeyEvent.KEYCODE_CHANNEL_DOWN)
                    }
                }
                return true // Consume la flecha para saltarse el menú de Zapping
            }

            // 4. FLECHAS IZQUIERDA Y DERECHA: Completamente deshabilitadas
            // Los huéspedes NO pueden abrir menús laterales, ajustes ni desconfigurar la cuenta.
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Se descartan tanto ACTION_DOWN como ACTION_UP.
                return true // 100% bloqueado
            }

            // 5. BOTÓN ATRÁS: Convertir en HOME para volver de inmediato a nuestro Launcher
            KeyEvent.KEYCODE_BACK -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    returnToLauncher()
                }
                return true // Consume el botón atrás
            }

            // 6. TECLA HOME: Asegurar regreso limpio al Launcher
            KeyEvent.KEYCODE_HOME -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    returnToLauncher()
                }
                return true
            }
        }

        // Cualquier otra tecla (como OK / DPAD_CENTER para confirmar) pasa normalmente
        return super.onKeyEvent(event)
    }

    /**
     * Regresa al Launcher principal de manera inmediata y limpia.
     */
    private fun returnToLauncher() {
        KioskState.isZappingForeground = false

        try {
            // Intento 1: Acción global HOME del sistema Android
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (_: Exception) {}

        try {
            // Intento 2: Intent directo a MainActivity de El Boom Launcher
            val launcherIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            startActivity(launcherIntent)
        } catch (_: Exception) {}
    }
}
