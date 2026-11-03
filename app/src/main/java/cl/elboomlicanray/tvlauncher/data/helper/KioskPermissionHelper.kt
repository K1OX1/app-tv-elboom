package cl.elboomlicanray.tvlauncher.data.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import cl.elboomlicanray.tvlauncher.service.ZappingKioskAccessibilityService
import java.io.DataOutputStream

/**
 * Gestor utilitario para verificar, activar y guiar el permiso del Servicio de Accesibilidad
 * para el modo Kiosco del control remoto en Android TV.
 */
object KioskPermissionHelper {

    private const val SERVICE_CLASS_NAME = "cl.elboomlicanray.tvlauncher.service.ZappingKioskAccessibilityService"
    private const val SERVICE_PACKAGE_NAME = "cl.elboomlicanray.tvlauncher"
    private val SERVICE_FLAT_COMPONENT = "$SERVICE_PACKAGE_NAME/$SERVICE_CLASS_NAME"

    /**
     * Comprueba si el Servicio de Accesibilidad ya está activo y funcionando en el televisor.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        try {
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            )
            if (accessibilityEnabled != 1) return false

            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(SERVICE_FLAT_COMPONENT, ignoreCase = true) ||
                    componentName.contains("ZappingKioskAccessibilityService", ignoreCase = true)
                ) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Intenta activación silenciosa automática si el TV Box cuenta con privilegios Root o depuración local.
     * Retorna true si se activó automáticamente con éxito sin necesidad de abrir menús.
     */
    fun tryAutoEnableViaRoot(context: Context): Boolean {
        try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("settings put secure enabled_accessibility_services $SERVICE_FLAT_COMPONENT\n")
                os.writeBytes("settings put secure accessibility_enabled 1\n")
                os.writeBytes("exit\n")
                os.flush()
            }
            val exitCode = process.waitFor()
            if (exitCode == 0 && isAccessibilityServiceEnabled(context)) {
                Toast.makeText(context, "Modo Kiosco activado automáticamente con éxito", Toast.LENGTH_SHORT).show()
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Abre directamente el panel de ajustes de Accesibilidad de Android TV para que el usuario
     * solo presione el botón "Activar" en 1 clic.
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            // Intento 1: Ajustes de accesibilidad específicos de Leanback / Android TV
            val tvIntent = Intent("android.settings.ACCESSIBILITY_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(tvIntent)
        } catch (e: Exception) {
            try {
                // Intento 2: Ajustes de accesibilidad estándar de Android
                val fallbackIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "Abre Ajustes de la TV > Accesibilidad para activar el servicio", Toast.LENGTH_LONG).show()
            }
        }
    }
}
