# ARCHIVOS CRÍTICOS DEL PROYECTO (CRITICAL FILES)

> **ATENCIÓN:** Los siguientes archivos contienen lógica de bajo nivel, integración de hardware o configuraciones críticas del sistema operativo Android TV.  
> **NO MODIFICAR** salvo por motivos técnicos justificados y con respaldo previo.

---

### 1. `MainActivity.kt`
- **Riesgo:** Alto.
- **Motivo:**
  - Controla el comportamiento de Kiosco (`onBackPressedDispatcher` deshabilitado para evitar cierres).
  - Gestiona el atajo secreto del control remoto `0000` para entrar al menú administrador.
  - Implementa el filtro anti-rebote (Throttle D-Pad de 45ms) vital para que el mando de TV no salte elementos.
  - Limpia la memoria en `onTrimMemory` para evitar cierres por falta de RAM tras horas de uso.
- **Cuándo modificar:** Únicamente si se añade un nuevo atajo global del control remoto o se ajusta el ciclo de vida de la actividad.

---

### 2. `data/helper/AppLauncherHelper.kt`
- **Riesgo:** Alto.
- **Motivo:**
  - Implementa el caché de `Intent` para evitar pausas de 400ms al consultar el `PackageManager` en procesadores lentos de TV.
  - Aplica flags de alto rendimiento (`FLAG_ACTIVITY_NEW_TASK`, `FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`, `FLAG_ACTIVITY_CLEAR_TOP`).
  - Notifica al reproductor de TV para liberar el hardware `MediaCodec` antes de abrir apps pesadas como Netflix o YouTube.
- **Cuándo modificar:** Si se añaden intents o compatibilidad con una aplicación con un esquema de paquete personalizado.

---

### 3. `service/ZappingKioskAccessibilityService.kt`
- **Riesgo:** Crítico.
- **Motivo:**
  - Controla la protección de control remoto de Zapping TV.
  - Intercepta teclas de volumen, remapea D-Pad arriba/abajo a canales y anula izquierda/derecha para que los huéspedes no cambien de cuenta en Zapping.
- **Cuándo modificar:** Únicamente ante cambios en el mapeo de teclas del servicio de accesibilidad.

---

### 4. `presentation/sections/hero/PersistentLiveTvFrame.kt`
- **Riesgo:** Medio - Alto.
- **Motivo:**
  - Gestiona el ciclo de vida de ExoPlayer. Un mal manejo de `DisposableEffect` mantendría el audio sonando de fondo o colgaría el decodificador de video del televisor.
- **Cuándo modificar:** Para añadir o actualizar la lista de canales HLS (`tvChannels`).

---

### 5. `data/repository/HotelTvRepository.kt`
- **Riesgo:** Medio.
- **Motivo:**
  - Realiza las llamadas HTTP nativas al Channel Manager Nobeds y a Open-Meteo con timeouts estrictos para no congelar la UI.
- **Cuándo modificar:** Si cambia el formato del JSON de Nobeds o la URL base del endpoint.
