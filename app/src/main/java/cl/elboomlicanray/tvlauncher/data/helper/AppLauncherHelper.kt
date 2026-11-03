package cl.elboomlicanray.tvlauncher.data.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import cl.elboomlicanray.tvlauncher.R
import cl.elboomlicanray.tvlauncher.domain.model.AppCategory
import cl.elboomlicanray.tvlauncher.domain.model.AppItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor utilitario de alto rendimiento para ejecutar aplicaciones en Android TV de gama baja.
 *
 * Incluye caché de Intents para evitar consultas repetitivas al PackageManager (que en TVs lentos
 * causan micro-congelamientos de hasta 400ms) y flags de sistema optimizados.
 */
@Singleton
class AppLauncherHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val packageManager: PackageManager = context.packageManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val intentCache = ConcurrentHashMap<String, Intent>()

    // Callback opcional ejecutado justo antes de abrir otra app (ej. para detener decodificadores)
    var onBeforeAppLaunch: (() -> Unit)? = null

    companion object {
        val KIOSK_BLOCKED_PACKAGES = setOf(
            "flar2.homebutton",
            "com.sdmc.tms.daemon",
            "com.nathnetwork.x96minitv",
            "com.lite.fczx"
        )
    }

    /**
     * Comprueba si un paquete de sistema está restringido por la política de Kiosco hotelero.
     */
    fun isPackageBlockedInKiosk(packageName: String): Boolean {
        return KIOSK_BLOCKED_PACKAGES.contains(packageName)
    }

    /**
     * Intenta abrir una aplicación de terceros con máxima velocidad.
     * Prioriza Intent Leanback (TV nativo) y almacena en caché el resultado.
     * Aplica restricciones Kiosco automáticas para paquetes bloqueados del sistema.
     */
    fun launchApp(packageName: String, friendlyName: String? = null, allowAdminBypass: Boolean = false): Boolean {
        // Bloqueo quirúrgico de Kiosco: previene que huéspedes accedan a ajustes o tiendas no autorizadas
        if (!allowAdminBypass && isPackageBlockedInKiosk(packageName)) {
            showToast("Acceso restringido por la administración de las cabañas.")
            return false
        }

        try {
            // Notificar al launcher para liberar el hardware MediaCodec antes del cambio de contexto
            onBeforeAppLaunch?.invoke()

            // 1. Verificar si ya tenemos el intent en caché
            var launchIntent = intentCache[packageName]

            if (launchIntent == null) {
                val candidatePackages = if (packageName.contains("zapping", ignoreCase = true)) {
                    listOf("com.eltelon.zapping", packageName, "com.zappingtv.tv", "com.zapping.tv", "com.zapping.android")
                } else {
                    listOf(packageName)
                }

                for (pkg in candidatePackages) {
                    // Intento 1: Leanback (TV nativo con D-Pad)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        launchIntent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                    }
                    // Intento 2: Intent estándar de lanzamiento
                    if (launchIntent == null) {
                        launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    }
                    // Intento 3: Intent explícito de actividad principal para APKs sideloaded (Xuper TV, etc.)
                    if (launchIntent == null) {
                        val mainQuery = Intent(Intent.ACTION_MAIN).apply {
                            setPackage(pkg)
                            addCategory(Intent.CATEGORY_LAUNCHER)
                        }
                        val resolve = packageManager.queryIntentActivities(mainQuery, 0).firstOrNull()
                            ?: packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply {
                                setPackage(pkg)
                                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                            }, 0).firstOrNull()
                            ?: packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply {
                                setPackage(pkg)
                            }, 0).firstOrNull()

                        if (resolve != null) {
                            launchIntent = Intent(Intent.ACTION_MAIN).apply {
                                setClassName(pkg, resolve.activityInfo.name)
                            }
                        }
                    }
                    if (launchIntent != null) {
                        intentCache[packageName] = launchIntent
                        break
                    }
                }
            }

            if (launchIntent != null) {
                // Copia defensiva con flags de alto rendimiento para Android TV
                val optimizedIntent = Intent(launchIntent).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
                if (packageName.contains("zapping", ignoreCase = true)) {
                    cl.elboomlicanray.tvlauncher.service.KioskState.isZappingForeground = true
                }
                context.startActivity(optimizedIntent)
                return true
            } else {
                if (packageName.contains("zapping", ignoreCase = true)) {
                    try {
                        val playIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.eltelon.zapping")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(playIntent)
                        showToast("Abriendo Google Play Store para instalar Zapping...")
                        return true
                    } catch (e: Exception) {
                        showToast("Zapping no está instalada aún. Descárgala desde Google Play Store.")
                    }
                } else {
                    showAppNotInstalledToast(friendlyName ?: packageName)
                }
                return false
            }
        } catch (e: ActivityNotFoundException) {
            showAppNotInstalledToast(friendlyName ?: packageName)
            return false
        } catch (e: SecurityException) {
            showErrorToast("Permiso denegado para abrir ${friendlyName ?: packageName}")
            return false
        } catch (e: Exception) {
            showErrorToast("Error inesperado al abrir la aplicación")
            return false
        }
    }

    /**
     * Comprueba si una aplicación específica está instalada en el dispositivo.
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Obtiene la lista de aplicaciones reales instaladas en el sistema (TV y estándar),
     * excluyendo herramientas internas del sistema y el propio Kiosco Launcher.
     * Únicamente incluye aplicaciones instaladas que el usuario puede ejecutar.
     */
    fun getInstalledTvApps(): List<AppItem> {
        // 1. Apps con Leanback Launcher (TV nativo con D-Pad)
        val tvIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        // 2. Apps con Launcher Estándar (móviles / sideloaded APKs como Magis TV, Xuper TV, etc.)
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        // 3. Apps con Info (algunas apps de TV se registran bajo CATEGORY_INFO)
        val infoIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_INFO)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.ResolveInfoFlags.of(0L)
        } else {
            0
        }

        val leanbackApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(tvIntent, flags as PackageManager.ResolveInfoFlags)
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(tvIntent, 0)
        }

        val regularApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(launcherIntent, flags as PackageManager.ResolveInfoFlags)
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val infoApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(infoIntent, flags as PackageManager.ResolveInfoFlags)
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(infoIntent, 0)
        }

        val myPackageName = context.packageName
        val hiddenPackages = setOf(
            myPackageName,
            "com.android.cts.priv.ctsshim",
            "com.android.cts.ctsshim"
        )

        val scannedApps = mutableMapOf<String, AppItem>()

        val initialResolves = (leanbackApps + regularApps + infoApps)
            .distinctBy { it.activityInfo.packageName }

        for (resolveInfo in initialResolves) {
            val pkg = resolveInfo.activityInfo.packageName
            if (hiddenPackages.contains(pkg)) continue
            scannedApps[pkg] = createAppItemFromResolveInfo(resolveInfo, pkg)
        }

        // 4. Búsqueda exhaustiva sobre todos los paquetes instalados
        // Cubre Google Play Store (com.android.vending), Google Apps, APKs sideloaded y utilidades
        try {
            val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }
            for (p in installedPackages) {
                val pkg = p.packageName
                if (scannedApps.containsKey(pkg) || hiddenPackages.contains(pkg)) continue

                val launchIntent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                    ?: packageManager.getLaunchIntentForPackage(pkg)

                if (launchIntent != null) {
                    val resolve = try { packageManager.resolveActivity(launchIntent, 0) } catch (_: Exception) { null }
                    if (resolve != null) {
                        scannedApps[pkg] = createAppItemFromResolveInfo(resolve, pkg)
                    } else {
                        val fallbackItem = createAppItemFromPackage(pkg)
                        if (fallbackItem != null) {
                            scannedApps[pkg] = fallbackItem
                        }
                    }
                } else {
                    try {
                        val mainQuery = Intent(Intent.ACTION_MAIN).apply { setPackage(pkg) }
                        val resolves = packageManager.queryIntentActivities(mainQuery, 0)
                        val candidate = resolves.firstOrNull { it.activityInfo.exported } ?: resolves.firstOrNull()
                        if (candidate != null) {
                            scannedApps[pkg] = createAppItemFromResolveInfo(candidate, pkg)
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        // Orden de prioridad oficial: Zapping y entretenimiento primero, luego orden alfabético
        val priorityMap = mapOf(
            "com.eltelon.zapping" to 1,
            "com.netflix.ninja" to 2,
            "com.netflix.mediaclient" to 2,
            "com.google.android.youtube.tv" to 3,
            "com.google.android.youtube" to 4,
            "com.amazon.amazonvideo.livingroom" to 5,
            "com.disney.disneyplus" to 6,
            "com.hbo.hbonow" to 7,
            "com.wbd.stream" to 7,
            "com.spotify.tv.android" to 8,
            "com.android.vending" to 9, // Google Play Store
            "com.esaba.downloader" to 10,
            "com.android.chrome" to 11,
            "com.tiktok.tv" to 12,
            "com.android.mgstv" to 13
        )

        return scannedApps.values.sortedWith(
            compareBy<AppItem> { priorityMap[it.packageName] ?: 50 }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun createAppItemFromResolveInfo(resolveInfo: android.content.pm.ResolveInfo, pkg: String): AppItem {
        val label = try {
            resolveInfo.loadLabel(packageManager).toString().trim().ifBlank { pkg }
        } catch (_: Exception) {
            pkg
        }

        val rawBanner = resolveInfo.activityInfo.loadBanner(packageManager)
            ?: resolveInfo.activityInfo.applicationInfo.loadBanner(packageManager)
            ?: try { packageManager.getApplicationBanner(pkg) } catch (_: Exception) { null }

        val rawIcon = resolveInfo.loadIcon(packageManager)
            ?: resolveInfo.activityInfo.loadIcon(packageManager)
            ?: try { packageManager.getApplicationIcon(pkg) } catch (_: Exception) { null }
            ?: resolveInfo.activityInfo.loadLogo(packageManager)

        val isIconWide = (rawIcon != null && rawIcon.intrinsicWidth > 0 && rawIcon.intrinsicHeight > 0) &&
                (rawIcon.intrinsicWidth.toFloat() / rawIcon.intrinsicHeight.toFloat() > 1.3f)
        val effectiveBanner = rawBanner ?: if (isIconWide) rawIcon else null

        return AppItem(
            id = pkg,
            name = label,
            subtitle = "App Instalada",
            packageName = pkg,
            isSystemOrPreinstalled = true,
            category = AppCategory.STREAMING,
            iconDrawable = rawIcon,
            bannerDrawable = effectiveBanner
        )
    }

    private fun createAppItemFromPackage(pkg: String): AppItem? {
        return try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            val label = packageManager.getApplicationLabel(appInfo).toString().trim().ifBlank { pkg }
            val rawBanner = try { packageManager.getApplicationBanner(appInfo) } catch (_: Exception) { null }
            val rawIcon = try { packageManager.getApplicationIcon(appInfo) } catch (_: Exception) { null }

            val isIconWide = (rawIcon != null && rawIcon.intrinsicWidth > 0 && rawIcon.intrinsicHeight > 0) &&
                    (rawIcon.intrinsicWidth.toFloat() / rawIcon.intrinsicHeight.toFloat() > 1.3f)
            val effectiveBanner = rawBanner ?: if (isIconWide) rawIcon else null

            AppItem(
                id = pkg,
                name = label,
                subtitle = "App Instalada",
                packageName = pkg,
                isSystemOrPreinstalled = true,
                category = AppCategory.STREAMING,
                iconDrawable = rawIcon,
                bannerDrawable = effectiveBanner
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun showAppNotInstalledToast(appName: String) {
        val message = context.getString(R.string.app_not_installed_format, appName)
        showToast(message)
    }

    private fun showErrorToast(message: String) {
        showToast(message)
    }

    private fun showToast(message: String) {
        // Garantizar ejecución en el Hilo Principal (UI Thread)
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
