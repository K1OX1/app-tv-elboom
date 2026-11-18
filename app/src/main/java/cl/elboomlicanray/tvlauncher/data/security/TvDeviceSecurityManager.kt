package cl.elboomlicanray.tvlauncher.data.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import cl.elboomlicanray.tvlauncher.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de Seguridad y Cifrado para Identidad del Televisor y Sesión Segura.
 *
 * Principios:
 * - Genera un identificador de alta entropía (UUIDv4) no derivado de datos de hardware sensibles.
 * - Almacena la sesión de televisor (cookie `elboom_tv_device`) cifrada mediante AES-256-GCM
 *   con llaves custodiadas exclusivamente por el Android Keystore del dispositivo.
 * - Nunca expone cookies ni secretos en logs ni en parámetros de URL.
 */
@Singleton
class TvDeviceSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TvDeviceSecurityManager"
        private const val PREFS_NAME = "elboom_tv_security_keystore"
        private const val KEY_DEVICE_ID = "secure_device_id"
        private const val KEY_ENCRYPTED_COOKIE = "encrypted_session_cookie"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "ElBoomTvSessionKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    /**
     * Retorna o genera un identificador aleatorio de alta entropía (UUID) estable entre reinicios.
     * Si la aplicación se desinstala y reinstala, se genera un nuevo UUID.
     */
    @Synchronized
    fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank() || id.length < 16) {
            id = UUID.randomUUID().toString().replace("-", "")
            prefs.edit().putString(KEY_DEVICE_ID, id).commit()
            Log.i(TAG, "Nuevo identificador aleatorio de TV generado exitosamente.")
        }
        return id
    }

    /**
     * Retorna la etiqueta legible del dispositivo para el panel de administración.
     * Ejemplo: "El Boom TV · Google Chromecast TV · v1.0.52"
     */
    fun getDeviceLabel(): String {
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "SmartTV"
        val model = Build.MODEL ?: "AndroidTV"
        return "El Boom TV · $manufacturer $model · v${BuildConfig.VERSION_NAME}"
    }

    /**
     * Obtiene o crea la llave criptográfica AES-256 respaldada por hardware en Android Keystore.
     */
    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            Log.i(TAG, "Llave AES-256 generada en Android Keystore para protección de sesión TV.")
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Guarda la cookie de sesión firmada de forma cifrada mediante Android Keystore.
     * El valor en texto plano nunca se guarda en disco ni se envía a logs.
     */
    @Synchronized
    fun saveSessionCookie(cookieValue: String) {
        if (cookieValue.isBlank()) {
            clearSession()
            return
        }
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(cookieValue.toByteArray(Charsets.UTF_8))

            // Prepend IV al ciphertext: [12 bytes IV][ciphertext + tag]
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString(KEY_ENCRYPTED_COOKIE, encoded).commit()
            Log.i(TAG, "Sesión de televisor cifrada y persistida de forma segura.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cifrar la sesión de TV", e)
        }
    }

    /**
     * Recupera y descifra la cookie de sesión desde Android Keystore.
     * Retorna null si no existe o si no se puede descifrar.
     */
    @Synchronized
    fun getSessionCookie(): String? {
        val encoded = prefs.getString(KEY_ENCRYPTED_COOKIE, null) ?: return null
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= IV_SIZE) return null

            val iv = ByteArray(IV_SIZE)
            System.arraycopy(combined, 0, iv, 0, IV_SIZE)
            val cipherText = ByteArray(combined.size - IV_SIZE)
            System.arraycopy(combined, IV_SIZE, cipherText, 0, cipherText.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo descifrar la sesión de TV (clave revocada o modificada)", e)
            clearSession()
            null
        }
    }

    /**
     * Borra la sesión local cifrada (utilizado en revocación o cambio de cabaña).
     */
    @Synchronized
    fun clearSession() {
        prefs.edit().remove(KEY_ENCRYPTED_COOKIE).commit()
        Log.i(TAG, "Sesión local de TV eliminada.")
    }

    /**
     * Retorna si este televisor cuenta con una sesión válida descifrable.
     */
    fun isPaired(): Boolean {
        return !getSessionCookie().isNullOrBlank()
    }
}
