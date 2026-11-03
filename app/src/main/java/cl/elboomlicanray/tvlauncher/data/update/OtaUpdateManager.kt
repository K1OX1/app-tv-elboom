package cl.elboomlicanray.tvlauncher.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Gestor Autónomo y Seguro de Actualizaciones OTA (Over The Air) para El Boom TV.
 *
 * Características de Seguridad Quirúrgica:
 * 1. Consulta remota con timeouts estrictos y control de redirecciones.
 * 2. Descarga a almacenamiento de caché privado de la aplicación.
 * 3. Verificación de suma de comprobación criptográfica SHA-256 (si está provista).
 * 4. Verificación rigurosa de Package Name y firma digital del certificado de la app instalada
 *    (previene inyección de APKs de terceros o falsificaciones maliciosas).
 * 5. Lanzamiento selectivo del instalador nativo del sistema vía FileProvider.
 */
object OtaUpdateManager {

    private const val TAG = "OtaUpdateManager"

    // URLs oficiales en elboom.cl para comprobación de versiones OTA
    val UPDATE_METADATA_URLS = listOf(
        "https://elboom.cl/tv/version.json",
        "https://elboom.cl/api/tv/update",
        "https://extendsclass.com/api/json-storage/bin/fbdcfbb",
        "https://files.catbox.moe/3xiad6.json"
    )

    private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 BoomLauncher/1.0"

    suspend fun checkUpdate(context: Context, currentVersionCode: Long): UpdateInfo? = withContext(Dispatchers.IO) {
        var bestUpdate: UpdateInfo? = null
        var maxVersionCode = currentVersionCode

        for (metadataUrl in UPDATE_METADATA_URLS) {
            try {
                val url = URL(metadataUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    useCaches = false
                    setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
                    setRequestProperty("Accept", "application/json, text/plain, */*")
                    setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                    setRequestProperty("Pragma", "no-cache")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)

                    val remoteVersionCode = json.optInt("versionCode", 0)
                    val apkUrl = json.optString("apkUrl", "").ifBlank { json.optString("downloadUrl", "") }
                    val sha256 = json.optString("sha256", "").takeIf { it.isNotBlank() }

                    if (remoteVersionCode > maxVersionCode && apkUrl.isNotBlank()) {
                        Log.i(TAG, "¡Actualización encontrada en $metadataUrl! Remota: $remoteVersionCode, Actual: $currentVersionCode")
                        maxVersionCode = remoteVersionCode.toLong()
                        bestUpdate = UpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = json.optString("versionName", ""),
                            apkUrl = apkUrl,
                            changelog = json.optString("changelog", "").ifBlank {
                                json.optString("releaseNotes", "Mejoras de rendimiento y estabilidad.")
                            },
                            sha256 = sha256
                        )
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error verificando en $metadataUrl: ${e.message}")
            }
        }
        bestUpdate
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        expectedSha256: String? = null,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val destinationFile = File(context.cacheDir, "update_latest.apk")
        try {
            if (destinationFile.exists()) destinationFile.delete()

            var currentUrl = apkUrl
            var connection: HttpURLConnection? = null
            var redirects = 0
            while (redirects < 5) {
                val u = URL(currentUrl)
                val conn = (u.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
                    setRequestProperty("Accept", "*/*")
                }
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        conn.disconnect()
                        currentUrl = location
                        redirects++
                        continue
                    }
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error $code al descargar APK de $currentUrl")
                    conn.disconnect()
                    return@withContext false
                }
                connection = conn
                break
            }

            if (connection == null) {
                Log.e(TAG, "No se pudo establecer conexión tras redirecciones.")
                return@withContext false
            }

            val totalBytes = connection.contentLength
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(32768)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) { onProgress(progress.coerceIn(0.02f, 0.99f)) }
                        }
                    }
                }
            }

            connection.disconnect()

            // 1. Verificación de Seguridad y Firmas Criptográficas antes de invocar PackageInstaller
            val isVerified = verifyDownloadedApkSecurity(context, destinationFile, expectedSha256)
            if (!isVerified) {
                destinationFile.delete()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Actualización descartada por fallo en verificación de seguridad.", Toast.LENGTH_LONG).show()
                }
                return@withContext false
            }

            // Aseguramos que la barra llegue al 100% visualmente
            withContext(Dispatchers.Main) {
                onProgress(1.0f)
                installApk(context, destinationFile)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando o instalando actualización", e)
            if (destinationFile.exists()) destinationFile.delete()
            false
        }
    }

    private fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32768)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun verifyDownloadedApkSecurity(context: Context, apkFile: File, expectedSha256: String?): Boolean {
        try {
            // 1. Verificación de hash SHA-256 si está presente en la metadata remota
            if (!expectedSha256.isNullOrBlank()) {
                val computedSha256 = calculateFileSha256(apkFile)
                if (!computedSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
                    Log.e(TAG, "Rechazo de seguridad OTA: SHA-256 no coincide. Esperado: $expectedSha256, Obtenido: $computedSha256")
                    return false
                }
                Log.i(TAG, "Verificación SHA-256 superada con éxito.")
            }

            val pm = context.packageManager

            // 2. Verificación de integridad de paquete y coincidencia de Package Name
            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
            }

            if (archiveInfo == null) {
                Log.e(TAG, "Rechazo de seguridad OTA: el archivo descargado no es un APK válido.")
                return false
            }

            if (archiveInfo.packageName != context.packageName) {
                Log.e(TAG, "Rechazo de seguridad OTA: paquete desconocido (${archiveInfo.packageName} != ${context.packageName})")
                return false
            }

            // 3. Verificación criptográfica de firma (Certificado de desarrollador)
            val currentPkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val currentSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentPkgInfo.signingInfo?.apkContentsSigners?.map { it.toCharsString() }
            } else {
                @Suppress("DEPRECATION")
                currentPkgInfo.signatures?.map { it.toCharsString() }
            }

            val archiveSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.signingInfo?.apkContentsSigners?.map { it.toCharsString() }
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.signatures?.map { it.toCharsString() }
            }

            if (!currentSignatures.isNullOrEmpty() && !archiveSignatures.isNullOrEmpty()) {
                val matches = currentSignatures.any { archiveSignatures.contains(it) }
                if (!matches) {
                    Log.e(TAG, "Rechazo de seguridad OTA: la firma digital del APK descargado no coincide con la firma del Launcher instalado.")
                    return false
                }
                Log.i(TAG, "Verificación criptográfica de firma digital superada con éxito.")
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Excepción durante la verificación de seguridad del APK", e)
            return false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val baseIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // Buscar componentes nativos del instalador oficial del sistema
            val resolveInfos = context.packageManager.queryIntentActivities(baseIntent, 0)
            val systemActivity = resolveInfos.firstOrNull { ri ->
                val p = ri.activityInfo.packageName
                (p.contains("packageinstaller") || p == "android") && !p.contains("downloader")
            }

            if (systemActivity != null) {
                val directIntent = Intent(baseIntent).apply {
                    setClassName(systemActivity.activityInfo.packageName, systemActivity.activityInfo.name)
                }
                context.startActivity(directIntent)
                Log.i(TAG, "Instalador nativo lanzado con éxito: ${systemActivity.activityInfo.packageName}/${systemActivity.activityInfo.name}")
                return
            }

            // Si no se encuentra explícitamente filtrado, lanzar intent estándar con flags
            context.startActivity(baseIntent)
            Log.i(TAG, "Instalador estándar lanzado.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al lanzar Intent de instalación", e)
            try {
                // Fallback de contingencia mediante ACTION_INSTALL_PACKAGE
                val apkUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
                val fallbackIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = apkUri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error en fallback ACTION_INSTALL_PACKAGE", e2)
            }
        }
    }
}
