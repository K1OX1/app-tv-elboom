package cl.elboomlicanray.tvlauncher.presentation.home

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MainActivityEvents {
    private val _adminDialogRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val adminDialogRequests = _adminDialogRequests.asSharedFlow()

    private val _userInteractionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val userInteractionEvents = _userInteractionEvents.asSharedFlow()

    private val _dismissScreensaverEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dismissScreensaverEvents = _dismissScreensaverEvents.asSharedFlow()

    private val _releaseDecodersEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val releaseDecodersEvents = _releaseDecodersEvents.asSharedFlow()

    @Volatile
    var isScreensaverShowing: Boolean = false

    fun triggerAdminDialog() {
        _adminDialogRequests.tryEmit(Unit)
    }

    fun triggerReleaseDecoders() {
        _releaseDecodersEvents.tryEmit(Unit)
    }

    fun notifyUserInteraction() {
        _userInteractionEvents.tryEmit(Unit)
    }

    fun dismissScreensaver() {
        isScreensaverShowing = false
        _dismissScreensaverEvents.tryEmit(Unit)
        _userInteractionEvents.tryEmit(Unit)
    }
}
