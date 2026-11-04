package cl.elboomlicanray.tvlauncher

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.tv.material3.ExperimentalTvMaterial3Api
import cl.elboomlicanray.tvlauncher.presentation.home.HomeScreen
import cl.elboomlicanray.tvlauncher.presentation.home.MainActivityEvents
import cl.elboomlicanray.tvlauncher.presentation.theme.ElBoomTvTheme
import coil.Coil
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad Principal y Punto de Entrada del Kiosco TV Launcher de Alto Rendimiento.
 *
 * Configurado con launchMode="singleTask" en el AndroidManifest.
 * Incluye:
 * 1. Bloqueo de Back para prevenir cierres accidentales en el Home.
 * 2. Filtro Anti-Rebote (Debounce) de D-Pad para mandos de TV lentos.
 * 3. Atajo secreto de control remoto: 4 veces el botón '0' ('0000') abre el Menú Administrador.
 * 4. Gestión agresiva de memoria mediante onTrimMemory para funcionamiento continuo 24/7.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var screensaverDismissKey: Int? = null
    private var zeroKeyPressCount = 0
    private var lastZeroKeyPressTime = 0L

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Comportamiento de kiosco: No-op en la raíz del launcher
            }
        })

        setContent {
            ElBoomTvTheme {
                HomeScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cl.elboomlicanray.tvlauncher.service.KioskState.isZappingForeground = false
    }

    /**
     * Interceptación global de eventos del control remoto:
     * - Reseteo de salvapantallas en cualquier pulsación
     * - Detección del atajo secreto '0000' para menú administrador
     * - Filtro anti-rebote D-Pad para 60 FPS estables
     */
    // Framework Activity override; AndroidX Core annotates its inherited implementation as restricted.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (screensaverDismissKey == event.keyCode) {
            if (event.action == KeyEvent.ACTION_UP) screensaverDismissKey = null
            return true
        }
        // Si el salvapantallas está activo, CUALQUIER tecla lo descarta y se consume por completo
        // evitando que se abra la app o acción seleccionada de fondo (solución a bug de D-pad center).
        if (MainActivityEvents.isScreensaverShowing) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                screensaverDismissKey = event.keyCode
                MainActivityEvents.dismissScreensaver()
            }
            return true
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            MainActivityEvents.notifyUserInteraction()

            // Abrir menú de configuración con tecla MENU o SETTINGS del control remoto
            if (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_SETTINGS) {
                MainActivityEvents.triggerAdminDialog()
                return true
            }

            // Detección del atajo secreto '0000' (4 ceros seguidos)
            if (event.keyCode == KeyEvent.KEYCODE_0) {
                val now = SystemClock.uptimeMillis()
                if (now - lastZeroKeyPressTime < 3000L) {
                    zeroKeyPressCount++
                } else {
                    zeroKeyPressCount = 1
                }
                lastZeroKeyPressTime = now

                if (zeroKeyPressCount >= 4) {
                    zeroKeyPressCount = 0
                    MainActivityEvents.triggerAdminDialog()
                    return true
                }
            } else {
                zeroKeyPressCount = 0
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            try {
                Coil.imageLoader(this).memoryCache?.clear()
            } catch (_: Exception) {}
        }
    }
}
