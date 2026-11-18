package cl.elboomlicanray.tvlauncher.data.repository

import android.util.Log
import cl.elboomlicanray.tvlauncher.data.helper.HotelPreferencesHelper
import cl.elboomlicanray.tvlauncher.data.security.TvDeviceSecurityManager
import cl.elboomlicanray.tvlauncher.domain.model.NetworkOfflineException
import cl.elboomlicanray.tvlauncher.domain.model.PairingStartResponse
import cl.elboomlicanray.tvlauncher.domain.model.PairingStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvPairingRepository @Inject constructor(
    private val securityManager: TvDeviceSecurityManager,
    private val preferencesHelper: HotelPreferencesHelper
) {
    companion object {
        private const val TAG = "TvPairingRepository"
        private const val BASE_PAIRING_URL = "https://elboom.cl/api/hotel-tv/pairing"
        private const val START_ENDPOINT = "$BASE_PAIRING_URL/start"
        private const val STATUS_ENDPOINT = "$BASE_PAIRING_URL/status"
        private const val COOKIE_NAME = "elboom_tv_device"
    }

    /**
     * Inicia el emparejamiento enviando el identificador aleatorio seguro del televisor.
     * Retorna el código de 6 dígitos, URL de aprobación para QR y expiración.
     */
    suspend fun startPairing(): Result<PairingStartResponse> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val deviceId = securityManager.getDeviceId()
            val deviceLabel = securityManager.getDeviceLabel()

            val bodyJson = JSONObject().apply {
                put("deviceId", deviceId)
                put("deviceLabel", deviceLabel)
            }

            val url = URL(START_ENDPOINT)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ElBoom-SmartTV/${cl.elboomlicanray.tvlauncher.BuildConfig.VERSION_NAME}")
                useCaches = false
                doOutput = true
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(bodyJson.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            val responseText = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            if (statusCode == 200) {
                val json = JSONObject(responseText)
                if (json.optBoolean("success", false)) {
                    val code = json.getString("code")
                    val expiresAt = json.getLong("expiresAt")
                    val approvalUrl = json.getString("approvalUrl")
                    val qrDataUrl = if (json.has("qrDataUrl")) json.getString("qrDataUrl") else null

                    return@withContext Result.success(
                        PairingStartResponse(
                            success = true,
                            code = code,
                            expiresAt = expiresAt,
                            deviceId = deviceId,
                            approvalUrl = approvalUrl,
                            qrDataUrl = qrDataUrl
                        )
                    )
                } else {
                    val errorMsg = json.optString("error", "Error al iniciar emparejamiento")
                    return@withContext Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = try {
                    JSONObject(responseText).optString("error", "Error de servidor (HTTP $statusCode)")
                } catch (_: Exception) {
                    "Error de servidor (HTTP $statusCode)"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: java.io.IOException) {
            return@withContext Result.failure(NetworkOfflineException("Sin conexión a internet o tiempo de espera agotado"))
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando emparejamiento", e)
            return@withContext Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Consulta el estado del emparejamiento. Si fue aprobado, extrae la cookie segura
     * `elboom_tv_device`, la cifra en el Android Keystore y guarda la cabaña asignada.
     */
    suspend fun pollPairingStatus(code: String): Result<PairingStatusResponse> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val deviceId = securityManager.getDeviceId()
            val endpoint = "$STATUS_ENDPOINT?code=${code.trim()}&deviceId=${deviceId.trim()}"

            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ElBoom-SmartTV/${cl.elboomlicanray.tvlauncher.BuildConfig.VERSION_NAME}")
                useCaches = false
            }

            val statusCode = connection.responseCode
            val responseText = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            if (statusCode == 200) {
                val json = JSONObject(responseText)
                val status = json.optString("status", "pending")
                val isApproved = status == "approved"
                val unitId = if (json.has("unitId")) json.getInt("unitId") else null

                if (isApproved && unitId != null) {
                    // Extraer y validar Set-Cookie: elboom_tv_device=...
                    val setCookieHeaders = connection.headerFields["Set-Cookie"]
                        ?: connection.headerFields["set-cookie"]
                        ?: emptyList()

                    var sessionToken: String? = null
                    for (header in setCookieHeaders) {
                        if (header.contains("$COOKIE_NAME=")) {
                            val raw = header.substringAfter("$COOKIE_NAME=").substringBefore(";")
                            if (raw.isNotBlank()) {
                                sessionToken = raw.trim()
                                break
                            }
                        }
                    }

                    if (!sessionToken.isNullOrBlank()) {
                        // Cifrar cookie en Keystore inmediatamente
                        securityManager.saveSessionCookie(sessionToken)
                        preferencesHelper.setConfiguredUnitId(unitId)
                        Log.i(TAG, "¡Televisor emparejado con éxito a Cabaña N°$unitId!")

                        return@withContext Result.success(
                            PairingStatusResponse(
                                success = true,
                                status = "approved",
                                unitId = unitId,
                                sessionCookie = sessionToken
                            )
                        )
                    } else {
                        Log.w(TAG, "Estado aprobado pero Set-Cookie no fue recibido en la respuesta.")
                    }
                }

                return@withContext Result.success(
                    PairingStatusResponse(
                        success = json.optBoolean("success", true),
                        status = status,
                        unitId = unitId
                    )
                )
            } else {
                return@withContext Result.failure(Exception("Error consultando estado (HTTP $statusCode)"))
            }
        } catch (e: java.io.IOException) {
            return@withContext Result.failure(NetworkOfflineException("Sin conexión"))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Desvincula este televisor y limpia la sesión local.
     */
    fun unpair() {
        securityManager.clearSession()
        preferencesHelper.clearCabinConfiguration()
        Log.i(TAG, "Televisor desvinculado.")
    }
}
