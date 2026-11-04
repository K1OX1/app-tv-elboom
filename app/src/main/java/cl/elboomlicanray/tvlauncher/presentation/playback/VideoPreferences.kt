package cl.elboomlicanray.tvlauncher.presentation.playback

import android.content.Context

/** Local to this TV; embedded playback remains the default after upgrades. */
object VideoPreferences {
    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences("video_playback", Context.MODE_PRIVATE)
    fun useYouTubeApp(context: Context): Boolean = prefs(context).getBoolean("external_youtube", false)
    fun setUseYouTubeApp(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("external_youtube", enabled).apply()
    }
}
