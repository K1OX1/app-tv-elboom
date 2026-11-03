package cl.elboomlicanray.tvlauncher.presentation.sections.header

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import cl.elboomlicanray.tvlauncher.presentation.components.TvIcon
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Píldora de Navegación del Header (HeaderNavPill).
 *
 * Reproduce el pill activo de la referencia con fondo azul degradado,
 * borde cian brillante y escala suave al tener foco D-Pad.
 */
@Composable
fun HeaderNavPill(
    iconName: String,
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(TvTokens.MotionDurationMs, easing = FastOutSlowInEasing),
        label = "pill_scale"
    )

    val pillShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isSelected || isFocused) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = pillShape,
                        spotColor = Color(0xFF00D4FF),
                        ambientColor = Color(0xFF00D4FF)
                    )
                } else Modifier
            )
            .clip(pillShape)
            .background(
                when {
                    isFocused -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0284C7), Color(0xFF00D4FF).copy(alpha = 0.8f))
                    )
                    isSelected -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0369A1), Color(0xFF0284C7))
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .border(
                width = if (isSelected || isFocused) 1.5.dp else 0.dp,
                color = if (isSelected || isFocused) Color(0xFF00D4FF) else Color.Transparent,
                shape = pillShape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TvIcon(
                name = iconName,
                size = 14.dp,
                color = if (isSelected || isFocused) Color.White else TvTokens.TextMuted
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected || isFocused) Color.White else TvTokens.TextMuted
            )
        }
    }
}
