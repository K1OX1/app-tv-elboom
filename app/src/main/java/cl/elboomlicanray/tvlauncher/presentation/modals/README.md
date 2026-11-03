# Módulo Modals (`cl.elboomlicanray.tvlauncher.presentation.modals`)

## Propósito
Alojar todos los diálogos y ventanas emergentes del launcher, organizados por dominio funcional, heredando del contenedor base estandarizado `BaseHotelDialog`.

## Subcarpetas y Archivos
- `base/`
  - `BaseHotelDialog.kt`: Contenedor modal con fondo glassmorphism TV, título dorado, descripción, botón de cierre ("Entendido / Volver") con foco inicial garantizado en D-Pad.
  - `DialogItemRow.kt`: Fila reutilizable de item o servicio para listas dentro de diálogos.
- `stay/`
  - `MyStayDialog.kt`: Ficha de reserva en tiempo real desde Nobeds (titular, fechas de check-in / check-out, estado de pago, patente del vehículo, estacionamiento y camas).
- `apps/`
  - `StreamingAppsDialog.kt`: Catálogo de 2 filas de apps de streaming para lanzar o instalar.
- `kiosk/`
  - `ZappingKioskSetupDialog.kt`: Asistente interactivo paso a paso para guiar al usuario a habilitar el Servicio de Accesibilidad para el modo kiosko de Zapping TV.
- `services/`
  - `HotelServiceDialogs.kt`: Diálogos de servicios de cabañas (`RoomServiceDialog`, `RestaurantDialog`, `SpaDialog`, `ActivitiesDialog`).
- `info/`
  - `HotelInfoDialogs.kt`: Diálogos informativos del complejo (`WifiInfoDialog` con código QR vectorial, `HotelMapDialog`, `NotificationsDialog`, `AgendaDialog`, `TransfersDialog`, `BeachDialog`, `ExcursionsDialog`).

## Regla de Oro
NUNCA crear un diálogo con `Dialog(...)` directo sin usar `BaseHotelDialog`. Esto garantiza accesibilidad uniforme, captura de tecla BACK del control remoto y foco inicial sin trampas de foco.
