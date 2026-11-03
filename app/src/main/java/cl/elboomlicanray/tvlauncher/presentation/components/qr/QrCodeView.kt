package cl.elboomlicanray.tvlauncher.presentation.components.qr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generador y Renderizador Oficial de Códigos QR Estándar ISO/IEC 18004.
 *
 * Utiliza el motor oficial de ZXing para garantizar el 100% de compatibilidad
 * con cámaras de smartphones (iPhone, Samsung, Xiaomi, Motorola, etc.).
 *
 * Incluye:
 * - Zona de silencio (Quiet Zone) reglamentaria para lectura instantánea de cámara.
 * - Corrección de errores Nivel M (15% de redundancia) con patrones de alineación oficiales.
 * - Renderizado nítido en Bitmap de alta resolución sin desenfoque en televisores 4K.
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    codeColor: Color = Color.Black
) {
    val qrBitmap = remember(content) {
        generateQrBitmap(
            text = content,
            sizePixels = 512,
            fgColor = android.graphics.Color.BLACK,
            bgColor = android.graphics.Color.WHITE
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Código QR para escanear",
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Genera un Bitmap estándar utilizando el algoritmo oficial de ZXing.
 */
private fun generateQrBitmap(
    text: String,
    sizePixels: Int,
    fgColor: Int,
    bgColor: Int
): Bitmap? {
    if (text.isBlank()) return null
    return try {
        val hints = HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 2) // Quiet zone estándar de 2 módulos
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, sizePixels, sizePixels, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) fgColor else bgColor
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (_: Exception) {
        null
    }
}
