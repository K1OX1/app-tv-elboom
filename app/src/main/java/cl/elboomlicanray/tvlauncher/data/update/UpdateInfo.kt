package cl.elboomlicanray.tvlauncher.data.update

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
    val sha256: String? = null
)
