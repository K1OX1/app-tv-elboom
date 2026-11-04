# DOCUMENTACIÓN OFICIAL: INTEGRACIÓN Y API CMS ELBOOM.CL

> **Alojamientos El Boom · Lican Ray (Chile)**  
> **Sistema Smart TV Launcher para Android TV (v1.0.51 / Build 51+)**  
> Documento técnico exhaustivo para Desarrolladores Backend, Ingenieros de Sistemas y Modelos de Inteligencia Artificial.

---

## 1. Visión General de la Arquitectura de Red

La aplicación **El Boom TV Launcher** (`cl.elboomlicanray.tvlauncher`) opera como una interfaz inteligente tipo quiosco de 10 pies (10-Foot UI) instalada en los televisores de cada cabaña del complejo turístico **Alojamientos El Boom** en Licán Ray, Lago Calafquén, Chile.

El launcher se comunica directamente con el CMS y backend web oficial alojado en **`https://elboom.cl`** a través de HTTPS para:
1. Identificar en tiempo real el estado de ocupación de la cabaña, nombre del huésped, fechas de estadía, camas y saldo financiero.
2. Descargar dinámicamente las fotografías HD, recorridos virtuales en YouTube, detalles de tinajas y amenidades de las 6 cabañas oficiales.
3. Cargar la Guía Turística interactiva (termas, parques, senderos, playas y actividades) con distancias, tiempos de viaje en vehículo y reproductor de video in-app.
4. Enviar fichas turísticas directamente al WhatsApp del huésped que se encuentra actualmente en la cabaña.
5. Descargar las normas del recinto, horarios oficiales de silencio, políticas de mascotas, tinajas y preguntas frecuentes (FAQs).
6. Ejecutar actualizaciones automáticas y desatendidas del sistema vía **OTA (Over The Air)** con verificación criptográfica SHA-256.

---

## 2. Convenciones de Red y Autenticación

### 2.1 Dominio Base
```
PRODUCCIÓN: https://elboom.cl
```

### 2.2 User-Agent Oficial
Todas las solicitudes HTTP enviadas desde el televisor incluyen un encabezado `User-Agent` que identifica la versión de la app:
```http
User-Agent: ElBoom-SmartTV/1.0.51
```
*(Para descargas OTA de binarios APK o verificación de versión se utiliza el User-Agent estándar de Android TV para compatibilidad con Cloudflare/CDNs).*

### 2.3 Seguridad y Tokens de Acceso (Cero Tokens Quemados)
- **Principio de seguridad:** La aplicación no almacena tokens "hardcoded" en el código fuente.
- **Flujo de vinculación:** En la primera instalación (`InitialSetupDialog.kt`), el administrador selecciona el número de cabaña (1 al 6) e ingresa el PIN/Token secreto emitido por el panel administrativo (`elboom.cl/admin`).
- **Almacenamiento seguro:** El token se guarda en las `SharedPreferences` locales del televisor (`hotel_tv_unit_token`) y el PIN de administrador se almacena cifrado en SHA-256 con salt único por hardware derivado de `Settings.Secure.ANDROID_ID`.
- **Cabeceras de autenticación enviadas en cada consulta:**
```http
Authorization: Bearer <TOKEN_DE_CABAÑA>
x-tv-token: <TOKEN_DE_CABAÑA>
```
*(Además, en `/api/hotel-tv` se envía en los query parameters `?unit={id}&token={token}` para compatibilidad dual).*

### 2.4 Política de Caché HTTP 304 (Not Modified) y Ahorro de Ancho de Banda
Para evitar saturar la conexión satelital Starlink y el procesador de televisores con 1 GB o 2 GB de RAM:
- El launcher envía los headers:
  ```http
  If-None-Match: <ETAG_PREVIO>
  If-Modified-Since: <FECHA_HTTP_PREVIA>
  ```
- Si el backend responde **`HTTP 304 Not Modified`**, el launcher **no descarga datos** y reutiliza la caché guardada en el disco privado del dispositivo (`cacheDir/cms_*.json`), reduciendo el consumo de red a 0 bytes de payload.
- Si no hay conexión a internet (modo offline), la app recurre transparentemente a la caché de disco sin interrumpir la experiencia del huésped.

---

## 3. Catálogo Exhaustivo de Endpoints

---

### Endpoint 1: Estado de Estadía, Huésped y Aseo (`GET /api/hotel-tv`)

- **Método:** `GET`
- **URL Completa:** `https://elboom.cl/api/hotel-tv`
- **Frecuencia:** Se consulta al iniciar el launcher y se refresca periódicamente cada 5 a 15 minutos en segundo plano (`HomeViewModel.kt`).
- **Propósito:** Determinar qué pantalla debe ver el televisor:
  - Bienvenida personalizada al huésped con su nombre.
  - Alerta de día de check-out (11:00 hrs) y saldo pendiente.
  - Pantalla de Aseo / Preparación para camareras cuando la cabaña está desocupada.
  - Pantalla de espera / standby cuando está libre.

#### Parámetros de Consulta (Query Params)
| Parámetro | Tipo | Requerido | Descripción | Ejemplo |
| :--- | :--- | :--- | :--- | :--- |
| `unit` | Integer | Sí | ID numérico de la cabaña (1 al 6). | `1` |
| `token` | String | Sí | Token o PIN secreto asignado a la cabaña en administración. | `boom_cab1_secure` |

#### Encabezados (Request Headers)
```http
GET /api/hotel-tv?unit=1&token=boom_cab1_secure HTTP/1.1
Host: elboom.cl
Accept: application/json
Cache-Control: no-cache
Authorization: Bearer boom_cab1_secure
User-Agent: ElBoom-SmartTV/1.0.51
```

#### Respuestas HTTP

##### Caso A: Cabaña Ocupada con Huésped Activo (`HTTP 200 OK`)
```json
{
  "success": true,
  "screen": "IN_STAY",
  "unit": {
    "id": 1,
    "name": "Cabaña N°1",
    "official_name": "Cabaña N°1 - Familiar Superior",
    "capacity": 8
  },
  "guest": {
    "title": "¡Bienvenido, Juan!",
    "firstName": "Juan",
    "fullName": "Juan Carlos Pérez Soto",
    "orderId": "NB-84920",
    "phone": "+56912345678",
    "email": "juan.perez@gmail.com",
    "originCity": "Santiago, Chile",
    "guestsCount": {
      "adults": 4,
      "children": 2,
      "total": 6
    },
    "bookingSource": "Sitio Web Oficial El Boom",
    "vehiclePlate": "Estacionamiento techado Cabaña N°1 (Patente JP-9021)",
    "selectedBedsSummary": "1 Cama King · 2 Camas 1.5 plazas · 2 Literas",
    "specialRequests": "Cuna para bebé solicitada, check-in anticipado",
    "checkin": "04/11/2026",
    "checkout": "08/11/2026",
    "nights": 4,
    "stayProgress": "Noche 2 de 4",
    "total": 380000,
    "prepay": 190000,
    "balance": 190000,
    "isPaid": false,
    "balanceMessage": "Saldo pendiente: $190.000 CLP"
  },
  "checkoutInfo": {
    "isCheckoutDay": false,
    "checkinOfficialTime": "15:00 hrs",
    "checkoutOfficialTime": "11:00 hrs",
    "rotationTime": "12:00 hrs",
    "checkoutAlert": null
  },
  "wifi": {
    "network": "El Boom Starlink",
    "password": "licanray_paraiso",
    "speedText": "Starlink Satelital Alta Velocidad"
  },
  "services": {
    "hostWhatsapp": "+56972694597",
    "hostWhatsappFormatted": "+56 9 7269 4597"
  },
  "cleaning": {
    "isDirty": false,
    "status": "LIMPIA",
    "sheetsCount": 6,
    "notes": "Revisado por supervisión a las 14:30"
  },
  "nextGuest": null
}
```

##### Caso B: Día de Salida (Check-out Day) (`HTTP 200 OK`)
```json
{
  "success": true,
  "screen": "IN_STAY",
  "checkoutInfo": {
    "isCheckoutDay": true,
    "checkinOfficialTime": "15:00 hrs",
    "checkoutOfficialTime": "11:00 hrs",
    "rotationTime": "12:00 hrs",
    "checkoutAlert": "Hoy finaliza tu estadía. El check-out oficial es a las 11:00 hrs."
  },
  "guest": {
    "firstName": "Juan",
    "fullName": "Juan Carlos Pérez Soto",
    "balance": 0,
    "isPaid": true,
    "balanceMessage": "Cuenta al día"
  }
}
```
*Efecto en la TV:* Muestra el banner rojo/dorado de alerta en la parte superior del launcher indicando la hora límite de salida (11:00 hrs) y un recordatorio para dejar las llaves.

##### Caso C: Cabaña Desocupada en Proceso de Limpieza (`HTTP 200 OK`)
```json
{
  "success": true,
  "screen": "CLEANING",
  "cleaning": {
    "isDirty": true,
    "status": "SUCIA",
    "sheetsCount": 6,
    "notes": "Cambio completo de sábanas blancas y toallas de baño"
  },
  "nextGuest": {
    "fullName": "Carolina Morales",
    "checkinTime": "15:00 hrs",
    "sheetsCount": 5,
    "specialRequests": "Preparar tinaja para las 19:00 hrs",
    "guestsCount": {
      "adults": 3,
      "children": 2,
      "total": 5
    }
  }
}
```
*Efecto en la TV:* Activa el modo de preparación para las camareras, mostrando cuántas sábanas montar y a qué hora llega el próximo huésped.

##### Caso D: Preparada / Limpia esperando Huésped de Hoy (`HTTP 200 OK`)
```json
{
  "success": true,
  "screen": "PREPARED",
  "cleaning": {
    "isDirty": false,
    "status": "LIMPIA"
  },
  "nextGuest": {
    "fullName": "Carolina Morales",
    "checkinTime": "15:00 hrs",
    "specialRequests": "Tinaja reservada"
  }
}
```
*Regla de Negocio Crítica:* Si `nextGuest` está asignado y la cabaña está limpia (`LIMPIA`), la TV **muestra directamente la bienvenida con el nombre del nuevo huésped ("¡Bienvenido, Carolina!")**, garantizando una primera impresión impecable al momento de abrir la puerta.

##### Caso E: Token Incorrecto o Cabaña No Autorizada (`HTTP 401 / 403`)
```json
{
  "success": false,
  "error": "Token de cabaña inválido o revocado."
}
```
*Efecto en la TV:* Se bloquea el launcher en `InitialSetupDialog.kt` solicitando al administrador reingresar el PIN de vinculación.

---

### Endpoint 2: Fichas Dinámicas de Cabañas (`GET /api/accommodations`)

- **Método:** `GET`
- **URL Completa:** `https://elboom.cl/api/accommodations`
- **Encabezados:** `Accept: application/json`, `If-None-Match`, `If-Modified-Since`.
- **Propósito:** Proporcionar los detalles interactivos de las cabañas 1 a 6 para el diálogo modal de cabañas (`MyStayDialog.kt`).

#### Estructura del JSON de Respuesta
```json
{
  "accommodations": [
    {
      "id": 1,
      "nobedsId": "CAB-01",
      "name": "Cabaña N°1 · Familiar",
      "type": "Cabaña de Madera Nativa",
      "area": 85,
      "capacity": 8,
      "bathrooms": 2,
      "beds": "1 Cama King · 1 Cama 2P · 2 Camas 1.5P · 1 Litera",
      "view": "Bosque Nativo y Jardines",
      "description": "Hermosa cabaña de 2 pisos construida en maderas nobles del sur de Chile...",
      "youtubeVideoId": "dQw4w9WgXcQ",
      "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
      "images": [
        "https://elboom.cl/images/cabanas/cab1_1.webp",
        "https://elboom.cl/images/cabanas/cab1_2.webp",
        "https://elboom.cl/images/cabanas/cab1_3.webp"
      ],
      "amenities": [
        "Estufa a combustión lenta (Bosca)",
        "Cocina full equipada con vajilla completa",
        "TV Smart 50 pulgadas con Starlink Satelital",
        "Parrilla privada para asados",
        "Estacionamiento techado a la puerta"
      ],
      "tinajaDetails": "Tinaja privada exclusiva a pasos de la terraza, capacidad 6 personas.",
      "bedrooms": [
        {
          "id": "DORM-1",
          "name": "Dormitorio Principal (Planta Baja)",
          "beds": [
            { "type": "king", "count": 1 }
          ]
        },
        {
          "id": "DORM-2",
          "name": "Dormitorio Segundo Piso",
          "beds": [
            { "type": "2p", "count": 1 },
            { "type": "1.5p", "count": 2 }
          ]
        }
      ]
    }
  ]
}
```

---

### Endpoint 3: Guía Turística y Atractivos (`GET /api/tourist-guide`)

- **Método:** `GET`
- **URL Completa:** `https://elboom.cl/api/tourist-guide`
- **Encabezados:** `Accept: application/json`, `If-None-Match`, `If-Modified-Since`.
- **Propósito:** Alimenta la guía turística interactiva con carrusel por categorías (`TouristGuideDialog.kt`).

#### Estructura del JSON de Respuesta
```json
{
  "data": [
    {
      "id": "TER-001",
      "name": "Termas Geométricas",
      "category": "TERMAS",
      "subcategory": "Termas de Diseño Arquitectónico",
      "distance_km": "37.9",
      "driving_time": "52 min",
      "description_short": "Las termas más fotogénicas de Chile. 20 piscinas de piedra unidas por pasarelas rojas...",
      "description_long": "Diseñadas por el reconocido arquitecto chileno Germán del Sol, las Termas Geométricas son una obra maestra construida en el fondo de una profunda quebrada natural dentro del Parque Nacional Villarrica...",
      "google_maps_url": "https://www.google.com/maps/search/?api=1&query=-39.501943,-71.874943",
      "phone": "+56997424108",
      "price": "$38.000 a $46.000 CLP p/p",
      "hours": "10:00 a 19:00 hrs",
      "season": "Todo el año (Reserva previa online)",
      "difficulty": "Baja (Caminatas en pasarelas planas)",
      "recommended_by_el_boom": true,
      "tags": ["⭐ IMPERDIBLE", "❤️ FAVORITO DE EL BOOM", "📸 FOTOGÉNICO", "♨️ RELAX"],
      "main_image_url": "https://elboom.cl/images/turismo/geometricas.webp",
      "gallery": [
        "https://elboom.cl/images/turismo/geometricas_1.webp",
        "https://elboom.cl/images/turismo/geometricas_2.webp"
      ],
      "youtube_url": "https://www.youtube.com/watch?v=0kG3n4f5zY8",
      "video_url": "https://www.youtube.com/watch?v=0kG3n4f5zY8",
      "rating": "4.9"
    }
  ]
}
```

#### Reglas de Renderizado en Android TV
- **Categorías soportadas:** `TODOS`, `CIUDADES`, `ATRACTIVOS`, `TERMAS`, `PARQUES`, `ACTIVIDADES`.
- **Marquesina auto-scrolling:** Si `description_long` excede el ancho de la tarjeta o pantalla, el texto avanza automáticamente a velocidad lenta y confortable para lectura desde el sofá.
- **Reproductor de Video Integrado:** Si `youtube_url` o `video_url` está presente, se activa el botón "Ver video" que reproduce el contenido en YouTube IFrame Player API optimizado a pantalla completa con aceleración por hardware GPU.

---

### Endpoint 4: Envío de Atractivo al WhatsApp del Huésped (`POST /api/hotel-tv/send-attraction`)

- **Método:** `POST`
- **URL Completa:** `https://elboom.cl/api/hotel-tv/send-attraction`
- **Propósito:** Permite al huésped enviar la ficha del atractivo a su teléfono celular con un solo toque del botón "Enviar a mi WhatsApp" en el control remoto de la TV.

#### Encabezados Requeridos
```http
POST /api/hotel-tv/send-attraction HTTP/1.1
Host: elboom.cl
Content-Type: application/json; charset=UTF-8
Accept: application/json
Authorization: Bearer <TOKEN_DE_CABAÑA>
x-tv-token: <TOKEN_DE_CABAÑA>
User-Agent: ElBoom-SmartTV/1.0.51
```

#### Cuerpo de la Solicitud (Request JSON Body)
```json
{
  "unitId": 1,
  "token": "boom_cab1_secure",
  "attractionName": "Termas Geométricas",
  "category": "TERMAS",
  "hours": "10:00 a 19:00 hrs",
  "price": "$38.000 a $46.000 CLP p/p",
  "drivingTime": "52 min",
  "googleMapsUrl": "https://www.google.com/maps/search/?api=1&query=-39.501943,-71.874943"
}
```

#### Respuesta Exitosa (`HTTP 200 OK`)
```json
{
  "success": true,
  "guestFirstName": "Juan",
  "message": "Información enviada exitosamente a tu WhatsApp"
}
```
*Comportamiento UI en TV:* Muestra un Toast animado: *"¡Listo Juan! Te enviamos los detalles a tu WhatsApp"*.

#### Respuestas de Error
- **`HTTP 401 / 403`:** Token inválido.
- **`HTTP 400`:** Cabaña sin reserva activa o sin número de teléfono registrado en el CMS.
```json
{
  "success": false,
  "error": "No hay un número de WhatsApp registrado para la reserva actual."
}
```

---

### Endpoint 5: Normas, Horarios, Tinajas y Recreación (`GET /api/property`)

- **Método:** `GET`
- **URL Completa:** `https://elboom.cl/api/property`
- **Encabezados:** `Accept: application/json`, `If-None-Match`, `If-Modified-Since`.
- **Propósito:** Mantiene sincronizadas las normas de convivencia, horarios de silencio y tarifas de tinajas en los diálogos informativos (`HotelInfoDialogs.kt`).

#### Estructura del JSON de Respuesta
```json
{
  "property": {
    "schedules": {
      "checkIn": "15:00 a 22:00 hrs",
      "checkOut": "09:00 a 11:00 hrs",
      "quietHours": "23:00 a 11:00 hrs (estrictamente prohibida música alta)"
    },
    "tinajasSection": {
      "title": "Tinajas de Agua Caliente a Leña",
      "noticeText": "Llenar y calentar una tinaja toma 7 horas de fuego continuo para asegurar la temperatura perfecta de 38°C a 40°C."
    },
    "tinajaOptions": [
      {
        "id": "TIN-01",
        "name": "Tinaja Familiar Ciprés",
        "capacity": 6,
        "photoUrl": "https://elboom.cl/images/tinajas/cipres.webp",
        "priceCLP": 40000,
        "durationHours": 3,
        "description": "Agua caliente pura de vertiente aromatizada con hojas de eucalipto natural."
      }
    ],
    "recreation": {
      "indoorGames": "Mesa de Pool reglamentaria, Máquina Arcade multijuegos retro ($100), Mesa de Ping Pong y Taca-Taca.",
      "kayaks": "Disponibles en los meses de Enero y Febrero en Playa Ensenada.",
      "outdoorPark": "Columpios, Sube y baja, Cama elástica protegida perimetralmente y Casa de juegos de madera."
    },
    "petPolicy": {
      "feePerNightCLP": 4000,
      "maxPets": 1,
      "allowedTypes": "Razas dóciles pequeñas a medianas",
      "requirement": "Enviar fotografía de la mascota por WhatsApp antes de la llegada para autorización previa."
    },
    "connectivity": {
      "wifiNetwork": "El Boom Starlink",
      "wifiPassword": "licanray_paraiso"
    },
    "faqs": [
      {
        "question": "¿A qué hora es el check-out oficial?",
        "answer": "El check-out es estrictamente a las 11:00 hrs para permitir la desinfección y preparación de la cabaña."
      },
      {
        "question": "¿Hay leña incluida para la combustión lenta?",
        "answer": "Sí, cada cabaña cuenta con una canasta de leña seca incluida para su llegada. Cargas adicionales disponibles con administración."
      }
    ],
    "phoneRaw": "+56972694597",
    "whatsappUrl": "https://wa.me/56972694597",
    "hero": {
      "bgImage": "https://elboom.cl/images/hero/fondo_principal.webp"
    }
  }
}
```

---

### Endpoint 6: Actualizaciones Automáticas del Sistema OTA (`GET /tv/version.json`)

- **Método:** `GET`
- **URLs de Consulta:**
  1. `https://elboom.cl/tv/version.json` (URL principal)
  2. `https://elboom.cl/api/tv/update` (URL secundaria de contingencia)
- **Frecuencia:** Verificación en segundo plano al iniciar el TV y cada 6 horas (`OtaUpdateManager.kt`).
- **Propósito:** Notificar o descargar automáticamente nuevas versiones del APK con mejoras de rendimiento o parches de seguridad.

#### Estructura del JSON de Versión (`version.json`)
```json
{
  "versionCode": 51,
  "versionName": "1.0.51",
  "apkUrl": "https://github.com/K1OX1/app-tv-elboom/releases/download/v1.0.51/app-release.apk",
  "downloadUrl": "https://github.com/K1OX1/app-tv-elboom/releases/download/v1.0.51/app-release.apk",
  "sha256": "4bae8599c9987a3a3830d907898b7fc29e9bd84a77c2268a09952aaf60355394",
  "releaseNotes": "Optimizaciones de reproduccion y recursos: liberacion de decodificadores en TV en vivo, selector de reproductor YouTube en configuracion, proteccion contra cuelgues de WebView y consumo de key up en salvapantallas.",
  "changelog": "1. Version y build sincronizadas en v1.0.51 (Build 51).\n2. Selector de reproductor YouTube integrado vs app oficial en ajustes.\n3. Deteccion de bloqueo de reproduccion (stall) y manejo de onRenderProcessGone.\n4. Optimizacion de decodificadores y buffer de video nativo."
}
```

#### Protocolo de Seguridad Quirúrgica en OTA
1. **Comparación numérica:** Si `remote.versionCode > current.versionCode`, se dispara el proceso.
2. **Descarga Segura:** Se descarga en `context.cacheDir/update_latest.apk`.
3. **Validación Criptográfica:** Si el campo `sha256` está presente en el JSON, se calcula el hash SHA-256 del archivo descargado y se aborta la instalación si no coincide exactamente.
4. **Validación de Firma y Paquete:** El manager verifica que el package name dentro del APK coincida con `cl.elboomlicanray.tvlauncher` y provenga del certificado de firma oficial.
5. **Instalación Nativa:** Lanza el instalador del sistema de Android mediante `FileProvider` con `Intent.ACTION_VIEW` y `FLAG_GRANT_READ_URI_PERMISSION`.

---

### Endpoint 7: Enlace Web Dinámico de Pago y Huésped (`/mi-reserva?id=...`)

- **URL:** `https://elboom.cl/mi-reserva?id={orderId}`
- **Propósito:** Cuando el huésped tiene saldo pendiente (`balance > 0`), el modal de pago (`PaymentDialog.kt`) renderiza un código QR dinámico que apunta a su enlace de pago personal en Webpay/Transbank alojado en el sitio web oficial.

---

## 4. Matriz de Resiliencia y Comportamiento Offline

| Endpoint | Timeout Conexión | Timeout Lectura | Si Falla o no hay Red | Mecanismo de Recuperación |
| :--- | :--- | :--- | :--- | :--- |
| `/api/hotel-tv` | 6.000 ms | 6.000 ms | Mantiene el último estado conocido (`HomeUiState`). Si nunca conectó, muestra pantalla de Standby. | Reintentos exponenciales en segundo plano (`HomeViewModel`). |
| `/api/accommodations` | 5.000 ms | 5.000 ms | Lee el archivo `cacheDir/cms_accommodations.json`. Si no existe, usa lista estática de 6 cabañas. | HTTP 304 ETag en la próxima reconexión. |
| `/api/tourist-guide` | 5.000 ms | 5.000 ms | Lee `cacheDir/cms_tourist_guide.json`. Si no existe, recurre a `DEFAULT_OFFICIAL_ATTRACTIONS`. | HTTP 304 ETag al recuperar internet. |
| `/api/property` | 5.000 ms | 5.000 ms | Lee `cacheDir/cms_property.json`. Si no existe, usa valores por defecto de `DynamicPropertyInfo()`. | HTTP 304 ETag al recuperar internet. |
| `/api/hotel-tv/send-attraction` | 8.000 ms | 8.000 ms | Muestra mensaje: *"Sin conexión a internet al enviar mensaje"*. | Notificación visual clara al usuario sin cerrar la app. |
| `/tv/version.json` | 10.000 ms | 10.000 ms | Ignora silenciosamente la comprobación y continúa operando normalmente. | Reintento automático en 6 horas. |

---

## 5. Mapeo UI: ¿Qué Componente de la App consume cada Consulta?

```
                                  [https://elboom.cl]
                                           │
         ┌───────────────────┬─────────────┴─────────────┬────────────────────┐
         │                   │                           │                    │
  GET /api/hotel-tv    GET /api/accommodations    GET /api/tourist-guide    GET /api/property
         │                   │                           │                    │
         ▼                   ▼                           ▼                    ▼
  ┌──────────────┐    ┌──────────────┐            ┌──────────────┐     ┌──────────────┐
  │LauncherTop   │    │MyStayDialog  │            │TouristGuide  │     │HotelRules    │
  │Header        │    │(Fichas HD,   │            │Dialog (Termas│     │Dialog,       │
  │(Nombre,      │    │Camas, Tour   │            │Parques, Video│     │Tinajas, FAQ  │
  │Wi-Fi, Reloj) │    │Virtual)      │            │Player, D-Pad)│     │y Recreación  │
  └──────────────┘    └──────────────┘            └──────────────┘     └──────────────┘
         │                                               │
         ▼                                               ▼
  ┌──────────────┐                                ┌──────────────┐
  │WelcomeHero   │                                │POST /api/    │
  │Panel         │                                │hotel-tv/send-│
  │(Bienvenida / │                                │attraction    │
  │Aseo / Standby│                                └──────────────┘
  └──────────────┘
```

---

## 6. Prompt Copiable para IAs y Desarrolladores del Backend Web

Copia y pega el siguiente bloque a cualquier IA o desarrollador web responsable de la plataforma `elboom.cl` para auditar o implementar estos servicios:

```markdown
Hola. Necesito que asegures que el servidor web de https://elboom.cl responda correctamente a las consultas del sistema Smart TV (paquete Android TV: cl.elboomlicanray.tvlauncher).

A continuación tienes el contrato estricto de APIs requerido por los televisores:

1. GET /api/hotel-tv?unit={1-6}&token={token}
   - Headers: Authorization: Bearer {token}, x-tv-token: {token}, Accept: application/json.
   - Debe retornar JSON con:
     * "success": true
     * "screen": "IN_STAY" | "CLEANING" | "PREPARED" | "STANDBY"
     * "unit": { "id": 1, "name": "Cabaña N°1", "capacity": 8 }
     * "guest": { "firstName": "Nombre", "fullName": "Nombre Completo", "orderId": "NB-...", "checkin": "DD/MM/AAAA", "checkout": "DD/MM/AAAA", "nights": 3, "total": 300000, "prepay": 150000, "balance": 150000, "isPaid": false, "balanceMessage": "..." }
     * "checkoutInfo": { "isCheckoutDay": true/false, "checkoutOfficialTime": "11:00 hrs", "checkoutAlert": "..." }
     * "wifi": { "network": "El Boom Starlink", "password": "..." }
     * "services": { "hostWhatsapp": "+56972694597" }
     * "cleaning": { "isDirty": true/false, "status": "LIMPIA" | "SUCIA", "sheetsCount": 6, "notes": "..." }
     * "nextGuest": { "fullName": "...", "checkinTime": "15:00 hrs", "specialRequests": "..." }
   - Si el token no coincide con el registrado en el admin para esa cabaña, debe responder HTTP 401 Unauthorized o 403 Forbidden.

2. GET /api/accommodations
   - Soporta HTTP 304 Not Modified con cabeceras ETag e If-None-Match.
   - Retorna array "accommodations" con las 6 cabañas: id, name, area, capacity, bathrooms, beds, view, description, youtubeVideoId, images (array de URLs), amenities (array de strings), tinajaDetails, bedrooms (array con id, name, beds: [{ type, count }]).

3. GET /api/tourist-guide
   - Soporta HTTP 304 Not Modified con ETag.
   - Retorna array "data" con: id, name, category ("CIUDADES", "ATRACTIVOS", "TERMAS", "PARQUES", "ACTIVIDADES"), subcategory, distance_km, driving_time, description_short, description_long, google_maps_url, phone, price, hours, season, recommended_by_el_boom, tags, main_image_url, gallery, youtube_url, rating.

4. POST /api/hotel-tv/send-attraction
   - Recibe JSON: { unitId, token, attractionName, category, hours, price, drivingTime, googleMapsUrl }.
   - Busca el teléfono del huésped que tiene reserva activa hoy en esa cabaña (unitId) y le envía los datos del atractivo por la API de WhatsApp de El Boom.
   - Retorna JSON: { "success": true, "guestFirstName": "Juan", "message": "Mensaje enviado exitosamente" }.

5. GET /api/property
   - Soporta HTTP 304 Not Modified con ETag.
   - Retorna objeto "property" con: schedules (checkIn, checkOut, quietHours), tinajasSection (title, noticeText), tinajaOptions, recreation (indoorGames, kayaks, outdoorPark), petPolicy (feePerNightCLP, maxPets, allowedTypes, requirement), connectivity, faqs (array de question y answer).

6. GET /tv/version.json
   - Retorna: { "versionCode": 51, "versionName": "1.0.51", "apkUrl": "...", "downloadUrl": "...", "sha256": "...", "changelog": "...", "releaseNotes": "..." }.
```

---
*Documentación generada y verificada quirúrgicamente contra el código fuente del proyecto Android TV.*  
*Archivo: `docs/API_ELBOOM.md`*
