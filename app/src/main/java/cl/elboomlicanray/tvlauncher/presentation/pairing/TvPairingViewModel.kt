package cl.elboomlicanray.tvlauncher.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.elboomlicanray.tvlauncher.data.helper.HotelPreferencesHelper
import cl.elboomlicanray.tvlauncher.data.repository.TvPairingRepository
import cl.elboomlicanray.tvlauncher.data.security.TvDeviceSecurityManager
import cl.elboomlicanray.tvlauncher.domain.model.NetworkOfflineException
import cl.elboomlicanray.tvlauncher.domain.model.OFFICIAL_ACCOMMODATION_UNITS
import cl.elboomlicanray.tvlauncher.domain.model.TvPairingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvPairingViewModel @Inject constructor(
    private val pairingRepository: TvPairingRepository,
    private val preferencesHelper: HotelPreferencesHelper,
    private val securityManager: TvDeviceSecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvPairingUiState>(TvPairingUiState.Loading())
    val uiState: StateFlow<TvPairingUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var countdownJob: Job? = null
    private var currentCode: String? = null

    init {
        startPairing()
    }

    fun startPairing() {
        stopJobs()
        _uiState.value = TvPairingUiState.Loading("Generando código de vinculación...")

        viewModelScope.launch {
            val result = pairingRepository.startPairing()
            result.onSuccess { response ->
                currentCode = response.code
                val totalSeconds = ((response.expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)

                _uiState.value = TvPairingUiState.WaitingForApproval(
                    code = response.code,
                    approvalUrl = response.approvalUrl,
                    expiresAt = response.expiresAt,
                    secondsRemaining = totalSeconds
                )

                startCountdown(response.expiresAt)
                startPolling(response.code)
            }.onFailure { error ->
                if (error is NetworkOfflineException) {
                    _uiState.value = TvPairingUiState.NetworkError(error.message ?: "Sin conexión a internet")
                } else {
                    _uiState.value = TvPairingUiState.ServerError(error.message ?: "Error al contactar con el servidor")
                }
            }
        }
    }

    private fun startCountdown(expiresAt: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val remainingMs = expiresAt - System.currentTimeMillis()
                val remainingSec = (remainingMs / 1000).toInt()

                if (remainingSec <= 0) {
                    stopJobs()
                    _uiState.value = TvPairingUiState.Expired
                    break
                }

                val current = _uiState.value
                if (current is TvPairingUiState.WaitingForApproval) {
                    _uiState.value = current.copy(secondsRemaining = remainingSec)
                }

                delay(1000L)
            }
        }
    }

    private fun startPolling(code: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            // Espera 2.5s antes del primer sondeo
            delay(2500L)
            while (isActive) {
                val result = pairingRepository.pollPairingStatus(code)
                result.onSuccess { response ->
                    if (response.status == "approved" && response.unitId != null) {
                        stopJobs()
                        val unit = OFFICIAL_ACCOMMODATION_UNITS.firstOrNull { it.id == response.unitId }
                        val unitName = unit?.name ?: "Cabaña N°${response.unitId}"
                        _uiState.value = TvPairingUiState.Approved(response.unitId, unitName)
                        return@launch
                    }
                }.onFailure {
                    // Silently continue polling on transient network hiccup
                }

                delay(3000L) // Polling cada 3 segundos
            }
        }
    }

    fun generateNewCode() {
        startPairing()
    }

    fun retry() {
        startPairing()
    }

    private fun stopJobs() {
        pollingJob?.cancel()
        pollingJob = null
        countdownJob?.cancel()
        countdownJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopJobs()
    }
}
