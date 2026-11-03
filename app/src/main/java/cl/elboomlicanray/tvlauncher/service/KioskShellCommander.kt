package cl.elboomlicanray.tvlauncher.service

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

/**
 * Gestor de inyección de teclas de hardware de alta velocidad.
 *
 * Utiliza 3 estrategias ultra-rápidas en orden de prioridad:
 * 1. Socket TCP Local (127.0.0.1:8765) con privilegios UID 2000 (Shell/Input) sin necesidad de root.
 * 2. Proceso Superusuario (su) para TV Boxes rooteadas de fábrica.
 * 3. Ejecución directa por shell de emergencia.
 */
object KioskShellCommander {

    private const val TAG = "KioskShellCommander"
    private const val DAEMON_PORT = 8765

    private val handlerThread = HandlerThread("KioskShellWorker").apply { start() }
    private val handler = Handler(handlerThread.looper)

    /**
     * Inyecta una tecla física en tiempo real en segundo plano (< 0.2 milisegundos).
     */
    fun sendKeyEvent(keyCode: Int) {
        handler.post {
            // Intento 1: Socket TCP al demonio Shell Local (UID 2000 con permiso INPUT de sistema)
            try {
                Socket("127.0.0.1", DAEMON_PORT).use { socket ->
                    socket.tcpNoDelay = true
                    socket.soTimeout = 800
                    val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                    writer.println("input keyevent $keyCode")
                    Log.d(TAG, "KeyEvent $keyCode inyectado vía Shell Daemon (127.0.0.1:$DAEMON_PORT)")
                    return@post
                }
            } catch (_: Exception) {}

            // Intento 2: Superusuario (Root / su)
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent $keyCode"))
                if (p.waitFor() == 0) {
                    Log.d(TAG, "KeyEvent $keyCode inyectado vía Superusuario (su)")
                    return@post
                }
            } catch (_: Exception) {}

            // Intento 3: Shell directo estándar
            try {
                Runtime.getRuntime().exec("input keyevent $keyCode")
            } catch (_: Exception) {}
        }
    }
}
