package cl.elboomlicanray.tvlauncher.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val packageName: String,
    val brandColorHex: Long = 0xFFE50914,
    val gradientColors: List<Color> = listOf(Color(0xFFE50914), Color(0xFF831010)),
    val isSystemOrPreinstalled: Boolean = false,
    val category: AppCategory = AppCategory.STREAMING,
    val iconDrawable: android.graphics.drawable.Drawable? = null,
    val bannerDrawable: android.graphics.drawable.Drawable? = null,
    val iconBitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    val bannerBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
)

enum class AppCategory {
    STREAMING,
    HOTEL_SERVICE,
    EXPERIENCE
}
