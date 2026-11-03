package cl.elboomlicanray.tvlauncher.config

import androidx.compose.ui.graphics.Color
import cl.elboomlicanray.tvlauncher.domain.model.AppCategory
import cl.elboomlicanray.tvlauncher.domain.model.AppItem

/**
 * Catálogo centralizado de Aplicaciones de Streaming y Entretenimiento.
 *
 * Responsabilidad:
 * Definir paquetes, nombres legibles, colores de identidad visual y gradientes
 * para cada plataforma (Zapping, Netflix, YouTube, Prime, Spotify, etc.).
 *
 * Modifique este archivo para añadir nuevas plataformas o alterar sus colores/paquetes.
 */
object StreamingAppsConfig {
    val defaultStreamingApps = listOf(
        AppItem(
            id = "zapping",
            name = "Zapping",
            subtitle = "Canales Nacionales & Cable",
            packageName = "com.eltelon.zapping",
            brandColorHex = 0xFFFFD200,
            gradientColors = listOf(Color(0xFFFFD200), Color(0xFFFF9E00))
        ),
        AppItem(
            id = "netflix",
            name = "Netflix",
            subtitle = "Películas & Series",
            packageName = "com.netflix.ninja",
            brandColorHex = 0xFF000000,
            gradientColors = listOf(Color(0xFF141414), Color(0xFF000000))
        ),
        AppItem(
            id = "youtube",
            name = "YouTube",
            subtitle = "Videos & Música 4K",
            packageName = "com.google.android.youtube.tv",
            brandColorHex = 0xFFFF0000,
            gradientColors = listOf(Color(0xFFFF0000), Color(0xFFCC0000))
        ),
        AppItem(
            id = "prime",
            name = "Prime Video",
            subtitle = "Amazon Originals",
            packageName = "com.amazon.amazonvideo.livingroom",
            brandColorHex = 0xFF00A8E1,
            gradientColors = listOf(Color(0xFF0F172A), Color(0xFF021024))
        ),
        AppItem(
            id = "disney",
            name = "Disney+",
            subtitle = "Disney, Pixar & Marvel",
            packageName = "com.disney.disneyplus",
            brandColorHex = 0xFF113CCF,
            gradientColors = listOf(Color(0xFF0A1931), Color(0xFF150050))
        ),
        AppItem(
            id = "max",
            name = "Max",
            subtitle = "HBO & Warner",
            packageName = "com.wbd.stream",
            brandColorHex = 0xFF002BE7,
            gradientColors = listOf(Color(0xFF002BE7), Color(0xFF00155E))
        ),
        AppItem(
            id = "spotify",
            name = "Spotify",
            subtitle = "Música & Podcasts",
            packageName = "com.spotify.tv.android",
            brandColorHex = 0xFF1ED760,
            gradientColors = listOf(Color(0xFF1ED760), Color(0xFF169C46))
        ),
        AppItem(
            id = "dgo",
            name = "DGO",
            subtitle = "Directv Fútbol & TV",
            packageName = "com.directv.dgotv",
            brandColorHex = 0xFF0072C6,
            gradientColors = listOf(Color(0xFF0072C6), Color(0xFF003865))
        ),
        AppItem(
            id = "pluto",
            name = "Pluto TV",
            subtitle = "Canales Gratis 24/7",
            packageName = "tv.pluto.android",
            brandColorHex = 0xFF000000,
            gradientColors = listOf(Color(0xFF18181B), Color(0xFF09090B))
        ),
        AppItem(
            id = "appletv",
            name = "Apple TV+",
            subtitle = "Originales & Cine",
            packageName = "com.apple.atve.androidtv.appletv",
            brandColorHex = 0xFF000000,
            gradientColors = listOf(Color(0xFF27272A), Color(0xFF09090B))
        )
    )
}
