package cl.elboomlicanray.tvlauncher.presentation.playback

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

fun isLowMemoryTv(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memory = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(memory)
    return manager.isLowRamDevice || memory.totalMem <= 2L * 1024 * 1024 * 1024
}

/** Budgets cover compressed samples, not codec surfaces or the entire process. */
@OptIn(UnstableApi::class)
fun createTvPlayer(context: Context, preview: Boolean = false): ExoPlayer {
    val app = context.applicationContext
    val lowMemory = isLowMemoryTv(app)
    val selector = DefaultTrackSelector(app).apply {
        parameters = buildUponParameters()
            .setMaxVideoSize(if (preview || lowMemory) 854 else 1280, if (preview || lowMemory) 480 else 720)
            .setMaxVideoFrameRate(30)
            .setMaxVideoBitrate(if (preview || lowMemory) 1_200_000 else 2_500_000)
            .setPreferredAudioLanguage("spa")
            .build()
    }
    return ExoPlayer.Builder(app, DefaultRenderersFactory(app).setEnableDecoderFallback(true))
        .setTrackSelector(selector)
        .setLoadControl(DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 25_000, 2_000, 4_000)
            .setTargetBufferBytes((if (preview) 4 else if (lowMemory) 8 else 16) * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(0, false)
            .build())
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        .build()
}
