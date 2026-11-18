package cl.elboomlicanray.tvlauncher.domain.model

data class PairingStartResponse(
    val success: Boolean,
    val code: String,
    val expiresAt: Long,
    val deviceId: String,
    val approvalUrl: String,
    val qrDataUrl: String? = null
)

data class PairingStatusResponse(
    val success: Boolean,
    val status: String, // "pending" | "approved" | "expired"
    val unitId: Int? = null,
    val sessionCookie: String? = null
)

sealed interface TvPairingUiState {
    data class Loading(val message: String = "Iniciando vinculación...") : TvPairingUiState

    data class WaitingForApproval(
        val code: String,
        val approvalUrl: String,
        val expiresAt: Long,
        val secondsRemaining: Int
    ) : TvPairingUiState

    data class Approved(
        val unitId: Int,
        val unitName: String
    ) : TvPairingUiState

    object Expired : TvPairingUiState

    data class NetworkError(val message: String) : TvPairingUiState

    data class ServerError(val message: String) : TvPairingUiState

    object Revoked : TvPairingUiState
}
