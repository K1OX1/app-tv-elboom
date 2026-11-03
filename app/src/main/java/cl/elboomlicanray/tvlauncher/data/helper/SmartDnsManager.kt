package cl.elboomlicanray.tvlauncher.data.helper

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor del Desbloqueo Streaming USA (Smart DNS) para Android TV y TV Boxes.
 *
 * Permite alternar con un solo clic entre la resolución DNS local chilena y
 * el servidor DNS Cloud proxy en EE.UU. (104.198.180.125) para cuentas de Netflix USA.
 *
 * Estrategias de aplicación en tiempo de ejecución:
 * 1. Comandos Shell con Superusuario (su / root de TV Boxes de fábrica):
 *    - Inyecta `setprop net.dns1` y reglas NAT de iptables para redirigir el puerto 53 UDP.
 * 2. Intent directo a Ajustes de Red del Sistema Operativo para configuración con un clic.
 */
@Singleton
class SmartDnsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesHelper: HotelPreferencesHelper
) {
    companion object {
        private const val TAG = "SmartDnsManager"
        const val DEFAULT_DNS_IP = "104.198.180.125"
        const val FALLBACK_DNS_IP = "1.1.1.1"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    fun isEnabled(): Boolean {
        return preferencesHelper.isSmartDnsEnabled()
    }

    fun getTargetDnsIp(): String {
        return preferencesHelper.getSmartDnsIp().ifBlank { DEFAULT_DNS_IP }
    }

    fun setDnsEnabled(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        preferencesHelper.setSmartDnsEnabled(enabled)
        val targetDns = if (enabled) getTargetDnsIp() else FALLBACK_DNS_IP

        Thread {
            val success = applyDnsSettings(enabled, targetDns)
            onComplete?.let { cb ->
                mainHandler.post { cb(success) }
            }
        }.start()
    }

    /**
     * Aplica las reglas a nivel de kernel y propiedades del sistema (TV Boxes con su/root).
     */
    private fun applyDnsSettings(enabled: Boolean, dnsIp: String): Boolean {
        return try {
            val commands = if (enabled) {
                listOf(
                    "setprop net.dns1 $dnsIp",
                    "setprop net.dns2 $FALLBACK_DNS_IP",
                    "iptables -t nat -D OUTPUT -p udp --dport 53 -j DNAT --to-destination $dnsIp:53 2>/dev/null",
                    "iptables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination $dnsIp:53"
                )
            } else {
                listOf(
                    "setprop net.dns1 1.1.1.1",
                    "setprop net.dns2 8.8.8.8",
                    "iptables -t nat -D OUTPUT -p udp --dport 53 -j DNAT --to-destination $DEFAULT_DNS_IP:53 2>/dev/null",
                    "iptables -t nat -F OUTPUT 2>/dev/null"
                )
            }

            var anyExecuted = false
            for (cmd in commands) {
                try {
                    val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                    p.waitFor()
                    anyExecuted = true
                } catch (_: Exception) {}
            }
            Log.i(TAG, "Smart DNS set to enabled=$enabled, ip=$dnsIp (applied=$anyExecuted)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error applying DNS settings", e)
            false
        }
    }

    /**
     * Abre de forma directa la pantalla de ajustes de red Wi-Fi o Ethernet de la TV.
     */
    fun openNetworkSettings(ctx: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent("android.settings.NETWORK_PROVIDER_SETTINGS"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}
