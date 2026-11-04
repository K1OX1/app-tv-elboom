package cl.elboomlicanray.tvlauncher

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

/**
 * Aplicación principal con Hilt y configuración optimizada de Coil para TV.
 *
 * Optimizaciones quirúrgicas para Android TV de 1GB-2GB RAM:
 * 1. Caché en memoria limitado a 12/24 MiB para prevenir OOM ante decodificación de galerías HD.
 * 2. RGB_565 forzado para reducir consumo de RAM por píxel en un 50% frente a ARGB_8888.
 * 3. Purga agresiva en onTrimMemory ante TRIM_MEMORY_UI_HIDDEN (ej. cuando se abre Zapping/Netflix),
 *    permitiendo que las apps de streaming externas dispongan de toda la memoria física del TV.
 */
@HiltAndroidApp
class TvLauncherApp : Application(), ImageLoaderFactory {

    private var imageLoaderInstance: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_cabanas")
        } catch (_: Exception) {}
    }

    override fun newImageLoader(): ImageLoader {
        val loader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(if (cl.elboomlicanray.tvlauncher.presentation.playback.isLowMemoryTv(this)) 12 * 1024 * 1024 else 24 * 1024 * 1024) // Presupuesto fijo para dejar espacio al reproductor
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(40L * 1024L * 1024L) // 40 MB en almacenamiento local
                    .build()
            }
            .crossfade(false)
            .allowRgb565(true) // Reduce el consumo de RAM de imágenes a la mitad (2 bytes por pixel)
            .respectCacheHeaders(false) // Cacheo agresivo para evitar llamadas de red repetitivas
            .build()
        imageLoaderInstance = loader
        return loader
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= TRIM_MEMORY_MODERATE || level == TRIM_MEMORY_UI_HIDDEN -> {
                // Liberar toda la memoria de imágenes cuando el launcher pasa a segundo plano
                // (ej. el huésped está viendo TV en Zapping o una película en Netflix)
                imageLoaderInstance?.memoryCache?.clear()
            }
            level >= TRIM_MEMORY_RUNNING_LOW -> {
                imageLoaderInstance?.memoryCache?.clear()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        imageLoaderInstance?.memoryCache?.clear()
    }
}
