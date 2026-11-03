package cl.elboomlicanray.tvlauncher.data.helper

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import cl.elboomlicanray.tvlauncher.domain.model.AccommodationUnit
import cl.elboomlicanray.tvlauncher.domain.model.OFFICIAL_ACCOMMODATION_UNITS
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotelPreferencesHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("elboom_hotel_tv_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "HotelPreferencesHelper"
        private const val KEY_UNIT_ID = "hotel_tv_unit_id"
        private const val KEY_ADMIN_PIN = "admin_pin" // Clave legacy
        private const val KEY_ADMIN_PIN_HASH = "admin_pin_sha256"
        private const val KEY_FAILED_PIN_ATTEMPTS = "admin_failed_pin_attempts"
        private const val KEY_PIN_LOCKOUT_UNTIL = "admin_pin_lockout_until"
        private const val KEY_API_URL = "hotel_tv_api_url"
        private const val DEFAULT_PIN = "1234"
        private const val DEFAULT_UNIT_ID = 1
        private const val DEFAULT_API_URL = "https://elboom.cl/api/hotel-tv"
        private const val KEY_KIOSK_PROMPT_DISMISSED = "kiosk_prompt_dismissed"
        private const val KEY_SELECTED_LANGUAGE = "hotel_tv_selected_language"
        private const val DEFAULT_LANGUAGE = "ES"
        private const val KEY_UNIT_TOKEN = "hotel_tv_unit_token"
        private const val DEFAULT_UNIT_TOKEN = ""

        // Nombres de archivos de caché en disco para mantener el heap de RAM limpio
        private const val FILE_ACCOMMODATIONS = "cms_accommodations.json"
        private const val FILE_TOURIST_GUIDE = "cms_tourist_guide.json"
        private const val FILE_PROPERTY = "cms_property.json"
    }

    private val hardwareSalt: String by lazy {
        try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "elboom_tv_default_salt"
        } catch (_: Exception) {
            "elboom_tv_default_salt"
        }
    }

    init {
        migrateLegacyPinAndCaches()
    }

    private fun computePinHash(pin: String): String {
        val input = "$hardwareSalt:${pin.trim()}:elboom_secure_salt"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Migra PIN en texto plano a SHA-256 con salt de hardware,
     * y purga JSONs gigantes de SharedPreferences al almacenamiento en disco (cacheDir).
     */
    private fun migrateLegacyPinAndCaches() {
        try {
            // 1. Migración de PIN a SHA-256
            if (prefs.contains(KEY_ADMIN_PIN)) {
                val legacyPin = prefs.getString(KEY_ADMIN_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
                prefs.edit()
                    .putString(KEY_ADMIN_PIN_HASH, computePinHash(legacyPin))
                    .remove(KEY_ADMIN_PIN)
                    .apply()
                Log.i(TAG, "PIN legacy migrado con éxito a SHA-256 con salt de hardware.")
            } else if (!prefs.contains(KEY_ADMIN_PIN_HASH)) {
                prefs.edit()
                    .putString(KEY_ADMIN_PIN_HASH, computePinHash(DEFAULT_PIN))
                    .apply()
            }

            // 2. Migración y purga de cachés de SharedPreferences a disco para liberar RAM
            val legacyAccommodations = prefs.getString("cached_accommodations_json", null)
            if (legacyAccommodations != null) {
                writeCacheFile(FILE_ACCOMMODATIONS, legacyAccommodations)
                prefs.edit().remove("cached_accommodations_json").apply()
            }

            val legacyTourist = prefs.getString("cached_tourist_guide_json", null)
            if (legacyTourist != null) {
                writeCacheFile(FILE_TOURIST_GUIDE, legacyTourist)
                prefs.edit().remove("cached_tourist_guide_json").apply()
            }

            val legacyProperty = prefs.getString("cached_property_json", null)
            if (legacyProperty != null) {
                writeCacheFile(FILE_PROPERTY, legacyProperty)
                prefs.edit().remove("cached_property_json").apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la migración de seguridad y caché", e)
        }
    }

    fun isKioskPromptDismissed(): Boolean {
        return prefs.getBoolean(KEY_KIOSK_PROMPT_DISMISSED, false)
    }

    fun setKioskPromptDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_KIOSK_PROMPT_DISMISSED, dismissed).apply()
    }

    fun getConfiguredUnitId(): Int {
        return prefs.getInt(KEY_UNIT_ID, DEFAULT_UNIT_ID)
    }

    fun setConfiguredUnitId(unitId: Int) {
        prefs.edit().putInt(KEY_UNIT_ID, unitId).apply()
    }

    fun getConfiguredUnit(): AccommodationUnit {
        val currentId = getConfiguredUnitId()
        return OFFICIAL_ACCOMMODATION_UNITS.firstOrNull { it.id == currentId }
            ?: OFFICIAL_ACCOMMODATION_UNITS.first()
    }

    fun getUnitToken(): String {
        return prefs.getString(KEY_UNIT_TOKEN, DEFAULT_UNIT_TOKEN) ?: DEFAULT_UNIT_TOKEN
    }

    fun setUnitToken(token: String) {
        prefs.edit().putString(KEY_UNIT_TOKEN, token.trim()).apply()
    }

    // =========================================================================
    // SEGURIDAD DE PIN QUIRÚRGICA: SHA-256 + RATE LIMITING ANTI-FUERZA BRUTA
    // =========================================================================

    fun isPinLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getLockoutRemainingSeconds(): Int {
        val lockoutUntil = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        val diffMs = lockoutUntil - System.currentTimeMillis()
        return if (diffMs > 0) (diffMs / 1000).toInt() + 1 else 0
    }

    fun verifyPin(pin: String): Boolean {
        if (isPinLockedOut()) {
            Log.w(TAG, "Intento de PIN rechazado por bloqueo de seguridad temporal activo.")
            return false
        }

        // Retardo artificial constante para inmunidad contra ataques de temporización (timing attacks)
        try {
            Thread.sleep(250)
        } catch (_: InterruptedException) {}

        val storedHash = prefs.getString(KEY_ADMIN_PIN_HASH, null) ?: computePinHash(DEFAULT_PIN)
        val inputHash = computePinHash(pin.trim())

        val isValid = MessageDigest.isEqual(
            storedHash.toByteArray(Charsets.UTF_8),
            inputHash.toByteArray(Charsets.UTF_8)
        )

        val editor = prefs.edit()
        if (isValid) {
            editor.putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
            editor.remove(KEY_PIN_LOCKOUT_UNTIL)
            editor.apply()
            return true
        } else {
            val failed = prefs.getInt(KEY_FAILED_PIN_ATTEMPTS, 0) + 1
            editor.putInt(KEY_FAILED_PIN_ATTEMPTS, failed)
            if (failed >= 4) {
                // 4 intentos fallidos activan un bloqueo de 60 segundos
                val lockoutTime = System.currentTimeMillis() + 60_000L
                editor.putLong(KEY_PIN_LOCKOUT_UNTIL, lockoutTime)
                editor.putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
                Log.w(TAG, "Umbral de fallos alcanzado. Panel de administrador bloqueado por 60s.")
            }
            editor.apply()
            return false
        }
    }

    fun setAdminPin(newPin: String) {
        prefs.edit()
            .putString(KEY_ADMIN_PIN_HASH, computePinHash(newPin))
            .remove(KEY_ADMIN_PIN)
            .apply()
    }

    fun getApiBaseUrl(): String {
        return prefs.getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
    }

    fun getSelectedAppPackages(): Set<String> {
        val raw = prefs.getString("selected_home_apps", null)
        return if (raw != null) {
            raw.split(",").filter { it.isNotBlank() }.toSet()
        } else {
            emptySet()
        }
    }

    fun setSelectedAppPackages(packages: Set<String>) {
        prefs.edit().putString("selected_home_apps", packages.joinToString(",")).apply()
    }

    // =========================================================================
    // OPTIMIZACIÓN DE MEMORIA RAM: ARCHIVOS PLANOS EN DISCO (CACHE DIR)
    // =========================================================================

    private fun writeCacheFile(filename: String, content: String) {
        try {
            val targetFile = File(context.cacheDir, filename)
            val tempFile = File(context.cacheDir, "$filename.tmp")
            tempFile.writeText(content, Charsets.UTF_8)
            if (tempFile.renameTo(targetFile).not()) {
                // Fallback si renameTo falla en ciertos FS de Android TV
                targetFile.writeText(content, Charsets.UTF_8)
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error escribiendo cache en disco para $filename", e)
        }
    }

    private fun readCacheFile(filename: String): String? {
        return try {
            val file = File(context.cacheDir, filename)
            if (file.exists() && file.length() > 0) {
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo cache en disco para $filename", e)
            null
        }
    }

    fun getCachedAccommodationsJson(): String? = readCacheFile(FILE_ACCOMMODATIONS)
    fun setCachedAccommodationsJson(json: String) = writeCacheFile(FILE_ACCOMMODATIONS, json)

    fun getCachedTouristGuideJson(): String? = readCacheFile(FILE_TOURIST_GUIDE)
    fun setCachedTouristGuideJson(json: String) = writeCacheFile(FILE_TOURIST_GUIDE, json)

    fun getCachedPropertyJson(): String? = readCacheFile(FILE_PROPERTY)
    fun setCachedPropertyJson(json: String) = writeCacheFile(FILE_PROPERTY, json)

    // =========================================================================
    // OPTIMIZACIÓN HTTP: ETAG & LAST-MODIFIED METADATA
    // =========================================================================

    fun getHttpHeader(key: String): String? = prefs.getString("http_hdr_$key", null)
    fun setHttpHeader(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            prefs.edit().remove("http_hdr_$key").apply()
        } else {
            prefs.edit().putString("http_hdr_$key", value).apply()
        }
    }

    fun getSelectedLanguage(): String {
        return prefs.getString(KEY_SELECTED_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setSelectedLanguage(languageCode: String) {
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, languageCode.uppercase()).apply()
    }

    // =========================================================================
    // SMART DNS / DESBLOQUEO NETFLIX USA (GOOGLE CLOUD US-CENTRAL1)
    // =========================================================================

    fun isSmartDnsEnabled(): Boolean {
        return prefs.getBoolean("smart_dns_enabled", false)
    }

    fun setSmartDnsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("smart_dns_enabled", enabled).apply()
    }

    fun getSmartDnsIp(): String {
        return prefs.getString("smart_dns_ip", "104.198.180.125") ?: "104.198.180.125"
    }

    fun setSmartDnsIp(ip: String) {
        prefs.edit().putString("smart_dns_ip", ip.trim()).apply()
    }
}
