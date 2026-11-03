package cl.elboomlicanray.tvlauncher.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.painterResource
import cl.elboomlicanray.tvlauncher.R
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Salvapantallas Ambiental Cinemático "Lican Ray Paraíso".
 * Activa modo de descanso tras inactividad para proteger pantallas OLED/LED.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AmbientScreensaver(
    unitName: String,
    currentTime: String,
    currentDate: String,
    temperatureText: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Vistas emblemáticas de Lican Ray y Lago Calafquén
    val scenicWallpapers = remember {
        listOf(
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1920&q=80", // Lago y playa
            "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1920&q=80", // Volcán y cordillera
            "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1920&q=80", // Bosque nativo sureño
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=1920&q=80"  // Cielo nocturno estrellado
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(12_000L) // Transición cada 12 segundos
            currentIndex = (currentIndex + 1) % scenicWallpapers.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                onDismiss()
                true
            }
            .clickable { onDismiss() }
            .background(Color.Black)
    ) {
        Crossfade(
            targetState = currentIndex,
            animationSpec = tween(durationMillis = 2000),
            label = "screensaver_crossfade"
        ) { index ->
            AsyncImage(
                model = scenicWallpapers[index],
                contentDescription = "Lican Ray Paisaje",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradiente ambiental sutil para legibilidad cinematográfica
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0x22000000),
                            Color(0xCC050811)
                        )
                    )
                )
        )

        // Indicador superior de la Cabaña
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 64.dp, top = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x880F172A))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_horizontal),
                        contentDescription = "El Boom",
                        modifier = Modifier.height(20.dp),
                        contentScale = ContentScale.FillHeight
                    )
                    Text(
                        text = "· $unitName",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // Reloj y fecha inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 64.dp, bottom = 48.dp)
        ) {
            Text(
                text = currentTime,
                fontSize = 84.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Text(
                text = "$currentDate · $temperatureText",
                fontSize = 24.sp,
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "Presiona cualquier tecla del control remoto para continuar",
                fontSize = 16.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
