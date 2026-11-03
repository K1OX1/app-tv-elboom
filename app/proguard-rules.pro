# Proguard / R8 Optimization Rules for Android TV Launcher
# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Hilt & Dagger
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class androidx.hilt.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**
-dontwarn androidx.tv.**
