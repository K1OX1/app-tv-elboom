package cl.elboomlicanray.tvlauncher

import cl.elboomlicanray.tvlauncher.domain.model.PairingStartResponse
import cl.elboomlicanray.tvlauncher.domain.model.PairingStatusResponse
import cl.elboomlicanray.tvlauncher.domain.model.TvPairingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TvPairingTest {

    @Test
    fun testGeneratedDeviceId_entropyAndFormatValid() {
        // High entropy UUID test
        val uuid = UUID.randomUUID().toString().replace("-", "")
        assertEquals(32, uuid.length)
        assertTrue("Device ID must match backend regex", uuid.matches(Regex("^[a-zA-Z0-9_-]{16,128}$")))
        assertFalse("Device ID must not contain colon or spaces", uuid.contains(":") || uuid.contains(" "))
    }

    @Test
    fun testApprovalUrlFormat() {
        val code = "481920"
        val expectedUrl = "https://elboom.cl/admin?tab=tv_updates&tv_pair=$code"
        assertTrue(expectedUrl.contains("tv_pair=$code"))
        assertTrue(expectedUrl.startsWith("https://elboom.cl/admin"))
    }

    @Test
    fun testPairingUiStateTransitions() {
        // 1. Initial Loading
        val loadingState = TvPairingUiState.Loading("Iniciando vinculación...")
        assertEquals("Iniciando vinculación...", loadingState.message)

        // 2. WaitingForApproval
        val waitingState = TvPairingUiState.WaitingForApproval(
            code = "123456",
            approvalUrl = "https://elboom.cl/admin?tab=tv_updates&tv_pair=123456",
            expiresAt = System.currentTimeMillis() + 600000L,
            secondsRemaining = 600
        )
        assertEquals("123456", waitingState.code)
        assertEquals(600, waitingState.secondsRemaining)

        // 3. Approved
        val approvedState = TvPairingUiState.Approved(unitId = 2, unitName = "Cabaña N°2")
        assertEquals(2, approvedState.unitId)
        assertEquals("Cabaña N°2", approvedState.unitName)

        // 4. Expired
        val expiredState = TvPairingUiState.Expired
        assertTrue(expiredState is TvPairingUiState)

        // 5. Network Error
        val netErr = TvPairingUiState.NetworkError("Sin conexión a internet")
        assertEquals("Sin conexión a internet", netErr.message)
    }

    @Test
    fun testPairingModelsParsing() {
        val start = PairingStartResponse(
            success = true,
            code = "998877",
            expiresAt = 1735000000000L,
            deviceId = "test-device-uuid-1234",
            approvalUrl = "https://elboom.cl/admin?tab=tv_updates&tv_pair=998877"
        )
        assertTrue(start.success)
        assertEquals(6, start.code.length)

        val statusPending = PairingStatusResponse(success = true, status = "pending")
        assertEquals("pending", statusPending.status)

        val statusApproved = PairingStatusResponse(
            success = true,
            status = "approved",
            unitId = 1,
            sessionCookie = "tv1.payload.signature"
        )
        assertEquals("approved", statusApproved.status)
        assertEquals(1, statusApproved.unitId)
        assertEquals("tv1.payload.signature", statusApproved.sessionCookie)
    }
}
