package cl.elboomlicanray.tvlauncher.data.helper

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkType {
    WIFI,
    ETHERNET,
    DISCONNECTED
}

data class NetworkStatus(
    val type: NetworkType = NetworkType.WIFI,
    val isConnected: Boolean = true
)

object NetworkMonitor {

    fun observeNetwork(context: Context): Flow<NetworkStatus> = callbackFlow {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(NetworkStatus(NetworkType.DISCONNECTED, false))
            close()
            return@callbackFlow
        }

        fun currentStatus(): NetworkStatus {
            val activeNetwork = cm.activeNetwork ?: return NetworkStatus(NetworkType.DISCONNECTED, false)
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus(NetworkType.DISCONNECTED, false)
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

            return when {
                !hasInternet -> NetworkStatus(NetworkType.DISCONNECTED, false)
                isEthernet -> NetworkStatus(NetworkType.ETHERNET, true)
                isWifi -> NetworkStatus(NetworkType.WIFI, true)
                else -> NetworkStatus(NetworkType.WIFI, true)
            }
        }

        // Emisión inmediata del estado actual
        trySend(currentStatus())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentStatus())
            }

            override fun onLost(network: Network) {
                trySend(currentStatus())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(currentStatus())
            }
        }

        try {
            val request = NetworkRequest.Builder().build()
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            try {
                cm.registerDefaultNetworkCallback(callback)
            } catch (_: Exception) {}
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()
}
