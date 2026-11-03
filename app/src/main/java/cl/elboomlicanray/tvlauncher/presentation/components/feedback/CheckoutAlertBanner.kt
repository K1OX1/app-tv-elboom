package cl.elboomlicanray.tvlauncher.presentation.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/**
 * Banner de Alerta de Salida y Cobro (Checkout Alert Banner).
 *
 * Responsabilidad:
 * Notificar al huésped en su día de check-out sobre la hora límite y estado de cuenta.
 *
 * PARA MODIFICAR:
 * - Colores de alerta -> RedAlert / AmberCheckout
 * - Formato o mensaje de advertencia
 */
@Composable
fun CheckoutAlertBanner(
    alert: String,
    balance: String,
    isPaid: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xB37C2D12),
                        Color(0x99451A03),
                        Color(0x8018181B)
                    )
                )
            )
            .border(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.85f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔔", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = alert,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = balance,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPaid) Color(0xFF4ADE80) else Color(0xFFFDE047)
            )
        }
    }
}
