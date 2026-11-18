# Alojamientos El Boom · Android TV Launcher

Aplicación oficial de Android TV (Smart TV Launcher) para los televisores de las cabañas de **Alojamientos El Boom** en Licán Ray, Lago Calafquén, Chile.

## 🚀 Versión Actual
- **Versión**: `v1.0.52`
- **Build**: `52`
- **Package**: `cl.elboomlicanray.tvlauncher`
- **Descarga Directa**: [app-release.apk](https://github.com/K1OX1/app-tv-elboom/releases/download/v1.0.52/app-release.apk)
- **SHA-256**: `1AEF4F8366EDF3B21D9EE2DE02BD9229CA2B419181AAFAA3D315B60F5B009A2C`

---

## 📖 Documentación del Sistema y APIs
Para conocer en profundidad la arquitectura técnica, flujo de emparejamiento nativo con QR, cifrado en hardware con Android Keystore y todas las consultas al backend de `https://elboom.cl`, revisa el documento oficial:

👉 **[DOCUMENTACION_SISTEMA_TV_Y_APIS.md](DOCUMENTACION_SISTEMA_TV_Y_APIS.md)**

---

## 🛠️ Características Principales
- **Emparejamiento Nativo con QR**: Escaneo desde teléfono móvil del administrador con aprobación en un clic.
- **Seguridad Android Keystore**: Sesión cifrada con `AES-256-GCM` en hardware y UUIDv4 anónimo.
- **TV en Vivo y Canales Nacionales**: Reproductor con soporte IPTV y liberación agresiva de decodificadores de hardware.
- **Guía Turística Interactiva**: Atractivos turísticos, termas, volcanes y senderos con envío de GPS a WhatsApp.
- **Información de Estadía Personalizada**: Nombre del huésped, fechas de estadía, estado de limpieza y normas.
- **Actualizaciones OTA Automáticas**: Sincronización continua de flota mediante `OtaUpdateManager` contra `https://elboom.cl/api/tv/update`.
