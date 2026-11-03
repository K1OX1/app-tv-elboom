package cl.elboomlicanray.tvlauncher.service

import android.util.Log
import cl.elboomlicanray.tvlauncher.data.update.OtaUpdateManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Servicio FCM Receptor de Comandos en Tiempo Real para El Boom TV.
 *
 * Escucha notificaciones "Data Payload" desde la consola web/Firebase.
 * Cuando envías una orden de actualización:
 * 1. Recibe 'action' == 'update' y 'apk_url'.
 * 2. Inicia de inmediato la descarga en segundo plano.
 * 3. Lanza el instalador de Android TV en 2 segundos sin intervención manual.
 */
class BoomFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "BoomFCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Nuevo Token FCM registrado para este TV: $token")
        // Suscribir automáticamente a este televisor al canal global de cabañas
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_cabanas")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Televisor suscrito exitosamente al canal: all_cabanas")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error suscribiendo al canal all_cabanas", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "Mensaje Push recibido desde Firebase! Datos: ${remoteMessage.data}")

        val action = remoteMessage.data["action"] ?: ""
        val apkUrl = remoteMessage.data["apk_url"] ?: ""

        if (action.equals("update", ignoreCase = true) && apkUrl.isNotEmpty()) {
            Log.i(TAG, "🚀 ORDEN DE ACTUALIZACIÓN INSTANTÁNEA RECIBIDA: $apkUrl")
            serviceScope.launch {
                OtaUpdateManager.downloadAndInstallApk(
                    context = applicationContext,
                    apkUrl = apkUrl,
                    onProgress = { progress ->
                        Log.d(TAG, "Descargando actualización remota: ${(progress * 100).toInt()}%")
                    }
                )
            }
        }
    }
}
