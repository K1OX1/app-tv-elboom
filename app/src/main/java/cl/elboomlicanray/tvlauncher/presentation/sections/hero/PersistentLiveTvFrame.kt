package cl.elboomlicanray.tvlauncher.presentation.sections.hero

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import cl.elboomlicanray.tvlauncher.presentation.home.MainActivityEvents
import cl.elboomlicanray.tvlauncher.presentation.theme.TvTokens

/**
 * Cuadro Persistente de TV en Vivo (PersistentLiveTvFrame).
 *
 * Responsabilidad:
 * Reproducir stream de televisión chilena en vivo en relación de aspecto 16:9
 * utilizando ExoPlayer con bajo consumo de memoria y buffer adaptativo para Smart TVs.
 */
@OptIn(UnstableApi::class)
@Composable
fun PersistentLiveTvFrame(
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaybackActive: Boolean = true,
    selectedLanguage: String = "ES"
) {
    val context = LocalContext.current
    val strings = remember(selectedLanguage) {
        cl.elboomlicanray.tvlauncher.presentation.i18n.AppStringsCatalog.get(selectedLanguage)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.045f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = 360f
        ),
        label = "tv_scale"
    )

    val livePulseTransition = rememberInfiniteTransition(label = "live_pulse")
    val liveDotAlpha by livePulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    val tvChannels = remember {
        listOf(
            Triple("13", "Canal 13 HD", "http://15.204.246.24:8080/Canal13HD/index.m3u8"),
            Triple("11", "Chilevisión HD", "http://15.204.246.24:8080/CHVHD/index.m3u8"),
            Triple("09", "Mega HD", "http://15.204.246.24:8080/MEGAHD/index.m3u8"),
            Triple("07", "TVN HD", "http://15.204.246.24:8080/TVNHD/index.m3u8"),
            Triple("14", "24 Horas Noticias", "http://cdn1tlinkgo.tlink.cl/24horashd/mono.m3u8")
        )
    }
    var currentChannelIdx by remember { mutableIntStateOf(0) }
    val (activeChNumber, activeChName, activeChStream) = tvChannels[currentChannelIdx % tvChannels.size]

    val fallbackUri = "https://origin.dpsgo.com/ssai/event/GI-9cp_bT8KcerLpZwkuhw/master.m3u8"
    var isVideoReady by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1000, 3000, 500, 1000)
            .setBackBuffer(0, false)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(854, 480)
                .setMaxVideoFrameRate(30)
                .setMaxVideoBitrate(1_200_000)
                .setPreferredAudioLanguage("spa")
                .setForceLowestBitrate(true)
                .build()
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, false)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                volume = 1.0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
    }

    LaunchedEffect(activeChStream) {
        isVideoReady = false
        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(activeChStream))
            exoPlayer.prepare()
            if (isPlaybackActive) exoPlayer.play()
        } catch (_: Exception) {}
    }

    LaunchedEffect(isPlaybackActive) {
        try {
            if (isPlaybackActive) {
                if (exoPlayer.playbackState == Player.STATE_IDLE) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(activeChStream))
                    exoPlayer.prepare()
                }
                exoPlayer.play()
            } else {
                // Liberar de inmediato el decodificador MediaCodec de hardware para evitar colisiones
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        MainActivityEvents.releaseDecodersEvents.collect {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            } catch (_: Exception) {}
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    try {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                    } catch (_: Exception) {}
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    try {
                        exoPlayer.setMediaItem(MediaItem.fromUri(activeChStream))
                        exoPlayer.prepare()
                        exoPlayer.play()
                    } catch (_: Exception) {}
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) isVideoReady = true
            }
            override fun onPlayerError(error: PlaybackException) {
                try {
                    exoPlayer.setMediaItem(MediaItem.fromUri(fallbackUri))
                    exoPlayer.prepare()
                    exoPlayer.play()
                } catch (_: Exception) {}
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val frameShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = false
            }
            .then(
                if (isFocused) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    TvTokens.CyanNeon.copy(alpha = 0.40f),
                                    TvTokens.CyanNeon.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.maxDimension * 0.85f
                            ),
                            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .background(Color.Black, shape = frameShape)
            .border(
                width = if (isFocused) 2.5.dp else 1.2.dp,
                brush = if (isFocused) {
                    Brush.verticalGradient(
                        listOf(TvTokens.CyanNeon, TvTokens.CyanNeon.copy(alpha = 0.70f))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f))
                    )
                },
                shape = frameShape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    try {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                    } catch (_: Exception) {}
                    onOpenFullscreen()
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(frameShape)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isVideoReady) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=1280&auto=format&fit=crop",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xD9000000)
                            ),
                            startY = 200f
                        )
                    )
            )

            // OVERLAY SUPERIOR: ● LIVE (izq) y HD (der)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFDC2626))
                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = liveDotAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = strings.liveBadge,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x99000000))
                        .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "HD",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // OVERLAY INFERIOR: CANAL Y PROGRAMA
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                        .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable { currentChannelIdx = (currentChannelIdx + 1) % tvChannels.size },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeChNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = activeChName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Noticias centrales",
                        fontSize = 11.sp,
                        color = TvTokens.TextMuted
                    )
                }
            }
        }
    }
}
