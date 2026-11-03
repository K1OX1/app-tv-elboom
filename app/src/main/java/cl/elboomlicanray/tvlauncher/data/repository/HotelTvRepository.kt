package cl.elboomlicanray.tvlauncher.data.repository

import android.util.Log
import cl.elboomlicanray.tvlauncher.data.helper.HotelPreferencesHelper
import cl.elboomlicanray.tvlauncher.domain.model.GuestStayInfo
import cl.elboomlicanray.tvlauncher.domain.model.ScreenStayState
import cl.elboomlicanray.tvlauncher.domain.model.WeatherInfo
import cl.elboomlicanray.tvlauncher.domain.model.DynamicCabinDetails
import cl.elboomlicanray.tvlauncher.domain.model.DynamicTouristAttraction
import cl.elboomlicanray.tvlauncher.domain.model.DynamicPropertyInfo
import cl.elboomlicanray.tvlauncher.domain.model.CleaningInfo
import cl.elboomlicanray.tvlauncher.domain.model.NextGuestInfo
import cl.elboomlicanray.tvlauncher.domain.model.DynamicBedroom
import cl.elboomlicanray.tvlauncher.domain.model.DynamicTinajaOption
import cl.elboomlicanray.tvlauncher.domain.model.DynamicFaq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotelTvRepository @Inject constructor(
    private val preferencesHelper: HotelPreferencesHelper
) {
    companion object {
        private const val TAG = "HotelTvRepository"
        private const val OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=-39.49&longitude=-72.15&current=temperature_2m,is_day,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,wind_speed_10m_max&timezone=America%2FSantiago"
        private const val ACCOMMODATIONS_URL = "https://elboom.cl/api/accommodations"
        private const val TOURIST_GUIDE_URL = "https://elboom.cl/api/tourist-guide"
        private const val PROPERTY_URL = "https://elboom.cl/api/property"
    }

    suspend fun fetchHotelStayData(): Result<GuestStayInfo> = withContext(Dispatchers.IO) {
        val unit = preferencesHelper.getConfiguredUnit()
        val token = preferencesHelper.getUnitToken()
        val tokenParam = if (token.isNotBlank()) "&token=$token" else ""
        val endpoint = "${preferencesHelper.getApiBaseUrl()}?unit=${unit.id}$tokenParam"

        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                setRequestProperty("User-Agent", "ElBoom-SmartTV/${cl.elboomlicanray.tvlauncher.BuildConfig.VERSION_NAME}")
                useCaches = false
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                if (json.optBoolean("success", false)) {
                    val screenStr = json.optString("screen", "")
                    val isOccupied = if (screenStr.isNotBlank()) {
                        screenStr.equals("IN_STAY", ignoreCase = true)
                    } else {
                        json.optBoolean("isOccupied", false)
                    }

                    val wifiObj = json.optJSONObject("wifi")
                    val rawWifiNet = wifiObj?.optString("wifiNetwork", "") ?: ""
                    val wifiNetwork = if (rawWifiNet.isNotBlank()) rawWifiNet else (wifiObj?.optString("network", "El Boom Starlink") ?: "El Boom Starlink")
                    val rawWifiPass = wifiObj?.optString("wifiPassword", "") ?: ""
                    val wifiPass = if (rawWifiPass.isNotBlank()) rawWifiPass else (wifiObj?.optString("password", "licanray_paraiso") ?: "licanray_paraiso")
                    val wifiSpeedText = wifiObj?.optString("speedText", "Starlink Satelital Alta Velocidad") ?: "Starlink Satelital Alta Velocidad"

                    val servicesObj = json.optJSONObject("services")
                    val hostWhatsapp = servicesObj?.optString("hostWhatsapp", "+56972694597") ?: "+56972694597"
                    val hostWhatsappFormatted = servicesObj?.optString("hostWhatsappFormatted", "+56 9 7269 4597") ?: "+56 9 7269 4597"

                    val cleaningObj = json.optJSONObject("cleaning")
                    val isDirty = cleaningObj?.optBoolean("isDirty", false) ?: false
                    val cleaningStatus = cleaningObj?.optString("status", if (isDirty) "SUCIA" else "LIMPIA") ?: if (isDirty) "SUCIA" else "LIMPIA"
                    val cleaningSheets = cleaningObj?.optInt("sheetsCount", 0) ?: 0
                    val cleaningNotes = cleaningObj?.optString("notes", "") ?: ""
                    val cleaningInfo = CleaningInfo(
                        isDirty = isDirty,
                        status = cleaningStatus,
                        sheetsCount = cleaningSheets,
                        notes = cleaningNotes
                    )

                    val nextGuestObj = json.optJSONObject("nextGuest")
                    val nextGuestInfo = if (nextGuestObj != null) {
                        val ngGuests = nextGuestObj.optJSONObject("guestsCount")
                        val ngAdults = ngGuests?.optInt("adults", 2) ?: 2
                        val ngChildren = ngGuests?.optInt("children", 0) ?: 0
                        val ngTotal = ngGuests?.optInt("total", ngAdults + ngChildren) ?: (ngAdults + ngChildren)
                        NextGuestInfo(
                            fullName = nextGuestObj.optString("fullName", ""),
                            checkinTime = nextGuestObj.optString("checkinTime", "15:00 hrs"),
                            adultsCount = ngAdults,
                            childrenCount = ngChildren,
                            totalGuests = ngTotal,
                            sheetsCount = nextGuestObj.optInt("sheetsCount", cleaningSheets),
                            specialRequests = nextGuestObj.optString("specialRequests", "")
                        )
                    } else null

                    val isLocallyCheckedIn = if (nextGuestInfo != null && nextGuestInfo.fullName.isNotBlank()) {
                        preferencesHelper.isUnitGuestCheckedIn(unit.id, nextGuestInfo.fullName)
                    } else false
                    val isLocallyClean = preferencesHelper.isUnitForceClean(unit.id)

                    // El alojamiento se considera listo/limpio si el backend reporta LIMPIA, !isDirty, PREPARED, o si localmente se confirmó
                    val isAlojamientoLimpio = !isDirty ||
                            cleaningStatus.equals("LIMPIA", ignoreCase = true) ||
                            screenStr.equals("PREPARED", ignoreCase = true) ||
                            isLocallyClean ||
                            isLocallyCheckedIn

                    // Si hay un próximo huésped asignado para hoy y el alojamiento está limpio o el huésped ya está en cabaña:
                    // ¡Mostrar directamente la pantalla de Bienvenida del Huésped, jamás la ficha de aseo!
                    if (nextGuestInfo != null && nextGuestInfo.fullName.isNotBlank() && isAlojamientoLimpio && !isOccupied) {
                        val firstName = nextGuestInfo.fullName.trim().split("\\s+".toRegex()).firstOrNull() ?: "Estimado Huésped"
                        return@withContext Result.success(
                            GuestStayInfo(
                                isOccupied = true,
                                screenState = ScreenStayState.IN_STAY,
                                title = "¡Bienvenido, $firstName!",
                                subtitle = "Alojamientos El Boom · ${nextGuestInfo.fullName}",
                                orderId = "",
                                guestFullName = nextGuestInfo.fullName,
                                guestFirstName = firstName,
                                guestPhone = "",
                                guestEmail = "",
                                guestOriginCity = "Chile",
                                adultsCount = nextGuestInfo.adultsCount,
                                childrenCount = nextGuestInfo.childrenCount,
                                totalGuests = nextGuestInfo.totalGuests,
                                bookingSource = "Directo Web El Boom",
                                vehiclePlate = "Estacionamiento techado Cabaña N°${unit.id}",
                                selectedBedsSummary = unit.bedsDescription,
                                specialRequests = nextGuestInfo.specialRequests,
                                checkinDate = "Hoy",
                                checkoutDate = "",
                                nights = 1,
                                stayProgress = "Llegada hoy",
                                totalAmount = 0L,
                                prepaidAmount = 0L,
                                balance = 0L,
                                isPaid = true,
                                balanceMessage = "Cuenta al día",
                                isCheckoutDay = false,
                                checkinOfficialTime = nextGuestInfo.checkinTime,
                                checkoutOfficialTime = "11:00 hrs",
                                rotationTime = "12:00 hrs",
                                checkoutAlert = null,
                                wifiNetwork = wifiNetwork,
                                wifiPassword = wifiPass,
                                wifiSpeedText = wifiSpeedText,
                                cleaningInfo = cleaningInfo.copy(isDirty = false, status = "LIMPIA"),
                                nextGuestInfo = nextGuestInfo,
                                hostWhatsapp = hostWhatsapp,
                                hostWhatsappFormatted = hostWhatsappFormatted
                            )
                        )
                    }

                    val isCleaning = (screenStr.equals("CLEANING", ignoreCase = true) || (isDirty && !isOccupied)) && !isAlojamientoLimpio
                    val isPrepared = screenStr.equals("PREPARED", ignoreCase = true) && !isAlojamientoLimpio

                    if (isCleaning) {
                        return@withContext Result.success(
                            GuestStayInfo(
                                isOccupied = false,
                                screenState = ScreenStayState.CLEANING,
                                title = "Preparación de alojamiento",
                                subtitle = "Preparación y Aseo · ${unit.name}",
                                selectedBedsSummary = unit.bedsDescription,
                                wifiNetwork = wifiNetwork,
                                wifiPassword = wifiPass,
                                wifiSpeedText = wifiSpeedText,
                                cleaningInfo = cleaningInfo,
                                nextGuestInfo = nextGuestInfo,
                                hostWhatsapp = hostWhatsapp,
                                hostWhatsappFormatted = hostWhatsappFormatted
                            )
                        )
                    }

                    if (isPrepared) {
                        return@withContext Result.success(
                            GuestStayInfo(
                                isOccupied = false,
                                screenState = ScreenStayState.PREPARED,
                                title = "Alojamiento preparado",
                                subtitle = "Listo para recibir a ${nextGuestInfo?.fullName ?: "nuevo huésped"}",
                                wifiNetwork = wifiNetwork,
                                wifiPassword = wifiPass,
                                wifiSpeedText = wifiSpeedText,
                                cleaningInfo = cleaningInfo,
                                nextGuestInfo = nextGuestInfo,
                                hostWhatsapp = hostWhatsapp,
                                hostWhatsappFormatted = hostWhatsappFormatted
                            )
                        )
                    }

                    if (!isOccupied) {
                        return@withContext Result.success(
                            GuestStayInfo(
                                isOccupied = false,
                                screenState = ScreenStayState.STANDBY,
                                title = json.optString("title", "Alojamiento disponible"),
                                subtitle = json.optString("subtitle", "Alojamientos El Boom · Lican Ray"),
                                wifiNetwork = wifiNetwork,
                                wifiPassword = wifiPass,
                                wifiSpeedText = wifiSpeedText,
                                cleaningInfo = cleaningInfo,
                                nextGuestInfo = nextGuestInfo,
                                hostWhatsapp = hostWhatsapp,
                                hostWhatsappFormatted = hostWhatsappFormatted
                            )
                        )
                    }

                    val guestObj = json.optJSONObject("guest")
                    val checkoutObj = json.optJSONObject("checkoutInfo")
                    val isCheckoutDay = checkoutObj?.optBoolean("isCheckoutDay", false) ?: false

                    val fullName = guestObj?.optString("fullName", "") ?: ""
                    val firstName = guestObj?.optString("firstName", "Estimado Huésped") ?: "Estimado Huésped"
                    val guestTitle = guestObj?.optString("title", "¡Bienvenido, $firstName!") ?: "¡Bienvenido, $firstName!"
                    val orderId = guestObj?.optString("orderId", "") ?: ""
                    val phone = guestObj?.optString("phone", "") ?: ""
                    val email = guestObj?.optString("email", "") ?: ""
                    val originCity = guestObj?.optString("originCity", "Chile") ?: "Chile"

                    val guestsCountObj = guestObj?.optJSONObject("guestsCount")
                    val adults = guestsCountObj?.optInt("adults", unit.capacity) ?: unit.capacity
                    val children = guestsCountObj?.optInt("children", 0) ?: 0
                    val totalGuests = guestsCountObj?.optInt("total", adults + children) ?: (adults + children)

                    val bookingSource = guestObj?.optString("bookingSource", "Directo Web El Boom") ?: "Directo Web El Boom"
                    val vehiclePlate = guestObj?.optString("vehiclePlate", "Estacionamiento techado Cabaña N°${unit.id}") ?: "Estacionamiento techado Cabaña N°${unit.id}"
                    val selectedBeds = guestObj?.optString("selectedBedsSummary", unit.bedsDescription) ?: unit.bedsDescription
                    val specialRequests = guestObj?.optString("specialRequests", "") ?: ""

                    val checkin = guestObj?.optString("checkin", "") ?: ""
                    val checkout = guestObj?.optString("checkout", "") ?: ""
                    val nights = guestObj?.optInt("nights", 1) ?: 1
                    val stayProgress = guestObj?.optString("stayProgress", "Estadía en curso") ?: "Estadía en curso"
                    val total = guestObj?.optLong("total", 0L) ?: 0L
                    val prepay = guestObj?.optLong("prepay", 0L) ?: 0L
                    val balance = guestObj?.optLong("balance", 0L) ?: 0L
                    val isPaid = guestObj?.optBoolean("isPaid", true) ?: true
                    val balanceMessage = guestObj?.optString("balanceMessage", "Cuenta al día") ?: "Cuenta al día"
                    val checkoutAlert = if (checkoutObj?.has("checkoutAlert") == true && !checkoutObj.isNull("checkoutAlert")) checkoutObj.optString("checkoutAlert") else null

                    val screenState = if (isCheckoutDay) ScreenStayState.CHECKOUT_DAY else ScreenStayState.IN_STAY

                    return@withContext Result.success(
                        GuestStayInfo(
                            isOccupied = true,
                            screenState = screenState,
                            title = guestTitle,
                            subtitle = "Alojamientos El Boom · $fullName",
                            orderId = orderId,
                            guestFullName = fullName,
                            guestFirstName = firstName,
                            guestPhone = phone,
                            guestEmail = email,
                            guestOriginCity = originCity,
                            adultsCount = adults,
                            childrenCount = children,
                            totalGuests = totalGuests,
                            bookingSource = bookingSource,
                            vehiclePlate = vehiclePlate,
                            selectedBedsSummary = selectedBeds,
                            specialRequests = specialRequests,
                            checkinDate = checkin,
                            checkoutDate = checkout,
                            nights = nights,
                            stayProgress = stayProgress,
                            totalAmount = total,
                            prepaidAmount = prepay,
                            balance = balance,
                            isPaid = isPaid,
                            balanceMessage = balanceMessage,
                            isCheckoutDay = isCheckoutDay,
                            checkinOfficialTime = checkoutObj?.optString("checkinOfficialTime", "15:00 hrs") ?: "15:00 hrs",
                            checkoutOfficialTime = checkoutObj?.optString("checkoutOfficialTime", "11:00 hrs") ?: "11:00 hrs",
                            rotationTime = checkoutObj?.optString("rotationTime", "12:00 hrs") ?: "12:00 hrs",
                            checkoutAlert = checkoutAlert,
                            wifiNetwork = wifiNetwork,
                            wifiPassword = wifiPass,
                            wifiSpeedText = wifiSpeedText,
                            cleaningInfo = cleaningInfo,
                            nextGuestInfo = nextGuestInfo,
                            hostWhatsapp = hostWhatsapp,
                            hostWhatsappFormatted = hostWhatsappFormatted
                        )
                    )
                }
            }
            Result.failure(Exception("HTTP ${connection.responseCode} al consultar Hotel TV"))
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo sincronizar Hotel TV en vivo: ${e.message}")
            Result.success(
                GuestStayInfo(
                    isOccupied = false,
                    screenState = ScreenStayState.STANDBY,
                    title = "Alojamiento disponible",
                    subtitle = "Alojamientos El Boom · Lican Ray",
                    wifiNetwork = "El Boom Starlink",
                    wifiPassword = "licanray_paraiso",
                    hostWhatsapp = "+56972694597",
                    hostWhatsappFormatted = "+56 9 7269 4597"
                )
            )
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchLicanRayWeather(): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(OPEN_METEO_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                useCaches = false
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val current = root.optJSONObject("current")

                if (current != null) {
                    val temp = current.optDouble("temperature_2m", 18.0)
                    val code = current.optInt("weather_code", 0)
                    val isDay = current.optInt("is_day", 1) == 1
                    val (icon, desc) = mapWmoWeatherCode(code, isDay)

                    // Procesar Pronóstico de 7 Días
                    val daily = root.optJSONObject("daily")
                    val weeklyList = mutableListOf<cl.elboomlicanray.tvlauncher.domain.model.DayForecast>()

                    if (daily != null) {
                        val times = daily.optJSONArray("time")
                        val weatherCodes = daily.optJSONArray("weather_code")
                        val maxTemps = daily.optJSONArray("temperature_2m_max")
                        val minTemps = daily.optJSONArray("temperature_2m_min")
                        val windSpeeds = daily.optJSONArray("wind_speed_10m_max")

                        val count = times?.length() ?: 0
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale("es", "CL"))
                        val dayNumFormat = java.text.SimpleDateFormat("d", java.util.Locale("es", "CL"))

                        for (i in 0 until minOf(count, 7)) {
                            val dateStr = times?.optString(i) ?: ""
                            var dayOfWeek = "DÍA"
                            var dayNumber = "${i + 1}"
                            try {
                                val parsedDate = sdf.parse(dateStr)
                                if (parsedDate != null) {
                                    dayOfWeek = dayFormat.format(parsedDate).uppercase().replace(".", "")
                                    dayNumber = dayNumFormat.format(parsedDate)
                                }
                            } catch (_: Exception) {}

                            val wCode = weatherCodes?.optInt(i, 0) ?: 0
                            val maxT = maxTemps?.optDouble(i, 18.0)?.toInt() ?: 18
                            val minT = minTemps?.optDouble(i, 8.0)?.toInt() ?: 8
                            val windKmh = windSpeeds?.optDouble(i, 8.0)?.toInt() ?: 8
                            val (wIcon, wDesc) = mapWmoWeatherCode(wCode, isDay = true)

                            weeklyList.add(
                                cl.elboomlicanray.tvlauncher.domain.model.DayForecast(
                                    dateString = dateStr,
                                    dayOfWeek = dayOfWeek,
                                    dayNumber = dayNumber,
                                    weatherCode = wCode,
                                    conditionDescription = wDesc,
                                    conditionIcon = wIcon,
                                    maxTempC = maxT,
                                    minTempC = minT,
                                    windKmH = windKmh
                                )
                            )
                        }
                    }

                    val finalWeekly = if (weeklyList.isNotEmpty()) weeklyList else getDefaultWeeklyForecast()

                    return@withContext Result.success(
                        WeatherInfo(
                            temperatureC = temp,
                            conditionDescription = desc,
                            conditionIcon = icon,
                            isDay = isDay,
                            weeklyForecast = finalWeekly
                        )
                    )
                }
            }
            Result.failure(Exception("HTTP ${connection.responseCode}"))
        } catch (e: Exception) {
            Result.success(
                WeatherInfo(
                    temperatureC = 19.0,
                    conditionDescription = "Lican Ray",
                    conditionIcon = "🌤️",
                    isDay = true,
                    weeklyForecast = getDefaultWeeklyForecast()
                )
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun getDefaultWeeklyForecast(): List<cl.elboomlicanray.tvlauncher.domain.model.DayForecast> {
        val cal = java.util.Calendar.getInstance()
        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale("es", "CL"))
        val dayNumFormat = java.text.SimpleDateFormat("d", java.util.Locale("es", "CL"))
        val list = mutableListOf<cl.elboomlicanray.tvlauncher.domain.model.DayForecast>()
        val defaultCodes = listOf(1, 80, 2, 0, 3, 61, 80)
        val defaultMax = listOf(19, 17, 19, 21, 18, 16, 17)
        val defaultMin = listOf(7, 7, 8, 9, 9, 8, 8)
        val defaultWind = listOf(8, 10, 10, 6, 11, 12, 8)

        for (i in 0 until 7) {
            val dName = dayFormat.format(cal.time).uppercase().replace(".", "")
            val dNum = dayNumFormat.format(cal.time)
            val code = defaultCodes[i % defaultCodes.size]
            val (icon, desc) = mapWmoWeatherCode(code, isDay = true)
            list.add(
                cl.elboomlicanray.tvlauncher.domain.model.DayForecast(
                    dateString = "",
                    dayOfWeek = dName,
                    dayNumber = dNum,
                    weatherCode = code,
                    conditionDescription = desc,
                    conditionIcon = icon,
                    maxTempC = defaultMax[i],
                    minTempC = defaultMin[i],
                    windKmH = defaultWind[i]
                )
            )
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    private fun mapWmoWeatherCode(code: Int, isDay: Boolean): Pair<String, String> {
        return when (code) {
            0 -> if (isDay) Pair("☀️", "Despejado") else Pair("🌙", "Noche despejada")
            1, 2 -> if (isDay) Pair("🌤️", "Parcialmente nublado") else Pair("☁️", "Nubosidad parcial")
            3 -> Pair("☁️", "Nublado")
            45, 48 -> Pair("🌫️", "Niebla matinal")
            51, 53, 55 -> Pair("🌦️", "Llovizna")
            61, 63 -> Pair("🌧️", "Lluvia suave")
            65 -> Pair("🌧️", "Lluvia intensa")
            71, 73, 75 -> Pair("❄️", "Nieve")
            80, 81, 82 -> Pair("🌧️", "Chubascos")
            95, 96, 99 -> Pair("⛈️", "Tormenta eléctrica")
            else -> Pair("⛅", "Lican Ray")
        }
    }

    // =============================================================================================
    // CMS DINÁMICO 1: CABAÑAS (FOTOS HD, VIDEO TOUR Y CAMAS)
    // =============================================================================================
    suspend fun fetchDynamicAccommodations(): Result<List<DynamicCabinDetails>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        val cachedJson = preferencesHelper.getCachedAccommodationsJson()
        val cachedEtag = preferencesHelper.getHttpHeader("etag_accommodations")
        val cachedLastMod = preferencesHelper.getHttpHeader("lastmod_accommodations")

        try {
            val url = URL(ACCOMMODATIONS_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                if (!cachedEtag.isNullOrBlank()) setRequestProperty("If-None-Match", cachedEtag)
                if (!cachedLastMod.isNullOrBlank()) setRequestProperty("If-Modified-Since", cachedLastMod)
                useCaches = false
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "Accommodations HTTP 304 (Not Modified): Reutilizando caché de disco sin transferencia de red.")
                if (cachedJson != null) {
                    val cabins = parseAccommodationsJson(cachedJson)
                    if (cabins.isNotEmpty()) return@withContext Result.success(cabins)
                }
            } else if (responseCode == 200) {
                val etag = connection.getHeaderField("ETag")
                val lastMod = connection.getHeaderField("Last-Modified")
                if (etag != null) preferencesHelper.setHttpHeader("etag_accommodations", etag)
                if (lastMod != null) preferencesHelper.setHttpHeader("lastmod_accommodations", lastMod)

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                preferencesHelper.setCachedAccommodationsJson(responseText)
                val cabins = parseAccommodationsJson(responseText)
                if (cabins.isNotEmpty()) {
                    return@withContext Result.success(cabins)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al consultar accommodations en vivo, revisando cache: ${e.message}")
        } finally {
            connection?.disconnect()
        }

        // Fallback a Cache Offline
        cachedJson?.let { json ->
            try {
                val cabins = parseAccommodationsJson(json)
                if (cabins.isNotEmpty()) {
                    return@withContext Result.success(cabins)
                }
            } catch (_: Exception) {}
        }

        Result.failure(Exception("No se pudieron cargar cabañas dinámicas"))
    }

    private fun parseAccommodationsJson(jsonString: String): List<DynamicCabinDetails> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("accommodations") ?: return emptyList()
        val list = mutableListOf<DynamicCabinDetails>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optInt("id", 0)
            if (id <= 0) continue

            val imagesList = mutableListOf<String>()
            val imgArr = obj.optJSONArray("images")
            if (imgArr != null) {
                for (j in 0 until imgArr.length()) {
                    val img = imgArr.optString(j, "")
                    if (img.isNotBlank()) imagesList.add(img)
                }
            }

            val amenitiesList = mutableListOf<String>()
            val amenArr = obj.optJSONArray("amenities")
            if (amenArr != null) {
                for (j in 0 until amenArr.length()) {
                    val amen = amenArr.optString(j, "")
                    if (amen.isNotBlank()) amenitiesList.add(amen)
                }
            }

            val bedroomsList = mutableListOf<DynamicBedroom>()
            val bedArr = obj.optJSONArray("bedrooms")
            if (bedArr != null) {
                for (j in 0 until bedArr.length()) {
                    val bObj = bedArr.optJSONObject(j) ?: continue
                    val bId = bObj.optString("id", "")
                    val bName = bObj.optString("name", "Dormitorio")
                    val bedsSummary = StringBuilder()
                    val subBeds = bObj.optJSONArray("beds")
                    if (subBeds != null) {
                        for (k in 0 until subBeds.length()) {
                            val bedItem = subBeds.optJSONObject(k)
                            val bType = bedItem?.optString("type", "") ?: ""
                            val bCount = bedItem?.optInt("count", 1) ?: 1
                            val bedFormatted = formatBedType(bType)
                            val label = if (bCount > 1) "$bCount $bedFormatted" else "1 $bedFormatted"
                            if (bedsSummary.isNotEmpty()) bedsSummary.append(" · ")
                            bedsSummary.append(label)
                        }
                    }
                    bedroomsList.add(DynamicBedroom(bId, bName, bedsSummary.toString()))
                }
            }

            list.add(
                DynamicCabinDetails(
                    id = id,
                    nobedsId = obj.optString("nobedsId", ""),
                    name = obj.optString("name", "Cabaña N°$id"),
                    type = obj.optString("type", "Cabaña"),
                    area = obj.optInt("area", 80),
                    capacity = obj.optInt("capacity", 8),
                    bathrooms = obj.optInt("bathrooms", 2),
                    bedsSummary = obj.optString("beds", ""),
                    view = obj.optString("view", "Bosque Nativo y Jardines"),
                    description = obj.optString("description", ""),
                    youtubeVideoId = obj.optString("youtubeVideoId", ""),
                    images = imagesList,
                    amenities = amenitiesList,
                    tinajaDetails = obj.optString("tinajaDetails", ""),
                    bedrooms = bedroomsList
                )
            )
        }
        return list
    }

    private fun formatBedType(type: String): String {
        return when (type.trim().lowercase()) {
            "2p" -> "Cama 2 plazas"
            "1.5p" -> "Cama 1.5 plazas"
            "1p" -> "Cama 1 plaza"
            "litera_1.5p" -> "Litera de 1.5 plazas"
            "litera_1p" -> "Litera de 1 plaza"
            "sofa_cama" -> "Sofá cama"
            "king" -> "Cama King"
            "queen" -> "Cama Queen"
            else -> type.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
    }

    // =============================================================================================
    // CMS DINÁMICO 2: GUÍA TURÍSTICA & ATRACTIVOS (TERMAS, PARQUES, PLAYAS)
    // =============================================================================================
    suspend fun fetchDynamicTouristGuide(): Result<List<DynamicTouristAttraction>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        val cachedJson = preferencesHelper.getCachedTouristGuideJson()
        val cachedEtag = preferencesHelper.getHttpHeader("etag_tourist_guide")
        val cachedLastMod = preferencesHelper.getHttpHeader("lastmod_tourist_guide")

        try {
            val url = URL(TOURIST_GUIDE_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                if (!cachedEtag.isNullOrBlank()) setRequestProperty("If-None-Match", cachedEtag)
                if (!cachedLastMod.isNullOrBlank()) setRequestProperty("If-Modified-Since", cachedLastMod)
                useCaches = false
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "TouristGuide HTTP 304 (Not Modified): Reutilizando atractivos desde caché de disco.")
                if (cachedJson != null) {
                    val items = parseTouristGuideJson(cachedJson)
                    if (items.isNotEmpty()) return@withContext Result.success(items)
                }
            } else if (responseCode == 200) {
                val etag = connection.getHeaderField("ETag")
                val lastMod = connection.getHeaderField("Last-Modified")
                if (etag != null) preferencesHelper.setHttpHeader("etag_tourist_guide", etag)
                if (lastMod != null) preferencesHelper.setHttpHeader("lastmod_tourist_guide", lastMod)

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                preferencesHelper.setCachedTouristGuideJson(responseText)
                val items = parseTouristGuideJson(responseText)
                if (items.isNotEmpty()) {
                    return@withContext Result.success(items)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al consultar tourist guide en vivo, revisando cache: ${e.message}")
        } finally {
            connection?.disconnect()
        }

        cachedJson?.let { json ->
            try {
                val items = parseTouristGuideJson(json)
                if (items.isNotEmpty()) {
                    return@withContext Result.success(items)
                }
            } catch (_: Exception) {}
        }

        Result.success(cl.elboomlicanray.tvlauncher.domain.model.DEFAULT_OFFICIAL_ATTRACTIONS)
    }

    private fun parseTouristGuideJson(jsonString: String): List<DynamicTouristAttraction> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<DynamicTouristAttraction>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val tagsList = mutableListOf<String>()
            val tagArr = obj.optJSONArray("tags")
            if (tagArr != null) {
                for (j in 0 until tagArr.length()) {
                    val t = tagArr.optString(j, "")
                    if (t.isNotBlank()) tagsList.add(t)
                }
            }

            val galleryList = mutableListOf<String>()
            val galArr = obj.optJSONArray("gallery")
            if (galArr != null) {
                for (j in 0 until galArr.length()) {
                    val g = galArr.optString(j, "")
                    if (g.isNotBlank()) galleryList.add(g)
                }
            }

            val rec = obj.optBoolean("recommended_by_el_boom", false) || obj.optInt("recommended_by_el_boom", 0) > 0
            val video = obj.optString("youtube_url").ifBlank { obj.optString("video_url", "") }
            val ratingStr = when {
                obj.has("rating") -> obj.optString("rating")
                obj.has("score") -> obj.optString("score")
                obj.optString("name").contains("Licán Ray", ignoreCase = true) -> "4.7"
                rec -> "4.9"
                else -> "4.8"
            }

            list.add(
                DynamicTouristAttraction(
                    id = obj.optString("id", "${i + 1}"),
                    name = obj.optString("name", "Atractivo"),
                    category = obj.optString("category", "ACTIVIDADES").uppercase(),
                    subcategory = obj.optString("subcategory", ""),
                    distanceKm = obj.optString("distance_km", ""),
                    drivingTime = obj.optString("driving_time", ""),
                    descriptionShort = obj.optString("description_short", ""),
                    descriptionLong = obj.optString("description_long", ""),
                    googleMapsUrl = obj.optString("google_maps_url", ""),
                    phone = obj.optString("phone", ""),
                    price = obj.optString("price", ""),
                    hours = obj.optString("hours", ""),
                    season = obj.optString("season", ""),
                    difficulty = obj.optString("difficulty", ""),
                    recommendedByElBoom = rec,
                    tags = tagsList,
                    mainImageUrl = obj.optString("main_image_url", ""),
                    gallery = galleryList,
                    youtubeUrl = video,
                    videoUrl = video,
                    rating = ratingStr
                )
            )
        }
        return list
    }

    // =============================================================================================
    // CMS DINÁMICO 3: NORMAS, REGLAS, HORARIOS Y RECREACIÓN (PROPERTY)
    // =============================================================================================
    suspend fun fetchDynamicProperty(): Result<DynamicPropertyInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        val cachedJson = preferencesHelper.getCachedPropertyJson()
        val cachedEtag = preferencesHelper.getHttpHeader("etag_property")
        val cachedLastMod = preferencesHelper.getHttpHeader("lastmod_property")

        try {
            val url = URL(PROPERTY_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                if (!cachedEtag.isNullOrBlank()) setRequestProperty("If-None-Match", cachedEtag)
                if (!cachedLastMod.isNullOrBlank()) setRequestProperty("If-Modified-Since", cachedLastMod)
                useCaches = false
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "Property HTTP 304 (Not Modified): Reutilizando normas y horarios desde caché de disco.")
                if (cachedJson != null) {
                    return@withContext Result.success(parsePropertyJson(cachedJson))
                }
            } else if (responseCode == 200) {
                val etag = connection.getHeaderField("ETag")
                val lastMod = connection.getHeaderField("Last-Modified")
                if (etag != null) preferencesHelper.setHttpHeader("etag_property", etag)
                if (lastMod != null) preferencesHelper.setHttpHeader("lastmod_property", lastMod)

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                preferencesHelper.setCachedPropertyJson(responseText)
                return@withContext Result.success(parsePropertyJson(responseText))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al consultar property en vivo, revisando cache: ${e.message}")
        } finally {
            connection?.disconnect()
        }

        cachedJson?.let { json ->
            try {
                return@withContext Result.success(parsePropertyJson(json))
            } catch (_: Exception) {}
        }

        Result.success(DynamicPropertyInfo())
    }

    private fun parsePropertyJson(jsonString: String): DynamicPropertyInfo {
        val root = JSONObject(jsonString)
        val prop = root.optJSONObject("property") ?: return DynamicPropertyInfo()

        val schedules = prop.optJSONObject("schedules")
        val checkIn = schedules?.optString("checkIn", "15:00 a 22:00 hrs") ?: "15:00 a 22:00 hrs"
        val checkOut = schedules?.optString("checkOut", "09:00 a 11:00 hrs") ?: "09:00 a 11:00 hrs"
        val quietHours = schedules?.optString("quietHours", "23:00 a 11:00 hrs (estrictamente prohibida música alta)")
            ?: "23:00 a 11:00 hrs (estrictamente prohibida música alta)"

        val tinajasSec = prop.optJSONObject("tinajasSection")
        val tinajasTitle = tinajasSec?.optString("title", "Tinajas de Agua Caliente a Leña") ?: "Tinajas de Agua Caliente a Leña"
        val tinajasNotice = tinajasSec?.optString("noticeText", "Llenar y calentar una tinaja toma 7 horas de fuego continuo para asegurar la temperatura perfecta.")
            ?: "Llenar y calentar una tinaja toma 7 horas de fuego continuo para asegurar la temperatura perfecta."

        val tinajaOptsList = mutableListOf<DynamicTinajaOption>()
        val tinajaOptsArr = prop.optJSONArray("tinajaOptions")
        if (tinajaOptsArr != null) {
            for (i in 0 until tinajaOptsArr.length()) {
                val to = tinajaOptsArr.optJSONObject(i) ?: continue
                tinajaOptsList.add(
                    DynamicTinajaOption(
                        id = to.optString("id", ""),
                        name = to.optString("name", "Tinaja"),
                        capacity = to.optInt("capacity", 4),
                        photoUrl = to.optString("photoUrl", ""),
                        priceClp = to.optLong("priceCLP", 35000L),
                        description = to.optString("description", ""),
                        durationHours = to.optInt("durationHours", 3)
                    )
                )
            }
        }

        val recreation = prop.optJSONObject("recreation")
        val indoor = recreation?.optString("indoorGames", "Mesa de Pool reglamentaria, Máquina Arcade multijuegos retro ($100), Mesa de Ping Pong profesional y Taca-Taca.")
            ?: "Mesa de Pool reglamentaria, Máquina Arcade multijuegos retro ($100), Mesa de Ping Pong profesional y Taca-Taca."
        val kayaks = recreation?.optString("kayaks", "Disponibles en los meses de Enero y Febrero en Playa Ensenada.")
            ?: "Disponibles en los meses de Enero y Febrero en Playa Ensenada."
        val outdoor = recreation?.optString("outdoorPark", "Columpios, Sube y baja, Cama elástica protegida perimetralmente y Casa de juegos de madera.")
            ?: "Columpios, Sube y baja, Cama elástica protegida perimetralmente y Casa de juegos de madera."

        val pet = prop.optJSONObject("petPolicy")
        val petFee = pet?.optLong("feePerNightCLP", 4000L) ?: 4000L
        val petMax = pet?.optInt("maxPets", 1) ?: 1
        val petAllowed = pet?.optString("allowedTypes", "Razas dóciles pequeñas a medianas") ?: "Razas dóciles pequeñas a medianas"
        val petReq = pet?.optString("requirement", "Enviar fotografía de la mascota por WhatsApp antes de la llegada para autorización previa.")
            ?: "Enviar fotografía de la mascota por WhatsApp antes de la llegada para autorización previa."

        val connectivity = prop.optJSONObject("connectivity")
        val wifiNet = connectivity?.optString("wifiNetwork", "El Boom Starlink") ?: "El Boom Starlink"
        val wifiPass = connectivity?.optString("wifiPassword", "licanray_paraiso") ?: "licanray_paraiso"

        val faqsList = mutableListOf<DynamicFaq>()
        val faqsArr = prop.optJSONArray("faqs")
        if (faqsArr != null) {
            for (i in 0 until faqsArr.length()) {
                val f = faqsArr.optJSONObject(i) ?: continue
                faqsList.add(
                    DynamicFaq(
                        question = f.optString("question", ""),
                        answer = f.optString("answer", "")
                    )
                )
            }
        }

        val heroObj = prop.optJSONObject("hero")
        val heroBg = heroObj?.optString("bgImage", "https://i.postimg.cc/KvkXjb86/1.jpg") ?: "https://i.postimg.cc/KvkXjb86/1.jpg"
        val waUrl = prop.optString("whatsappUrl", "https://wa.me/56972694597")

        return DynamicPropertyInfo(
            checkIn = checkIn,
            checkOut = checkOut,
            quietHours = quietHours,
            tinajasTitle = tinajasTitle,
            tinajasNotice = tinajasNotice,
            tinajaOptions = tinajaOptsList,
            indoorGames = indoor,
            kayaks = kayaks,
            outdoorPark = outdoor,
            petFeePerNight = petFee,
            petMaxAllowed = petMax,
            petAllowedTypes = petAllowed,
            petRequirement = petReq,
            wifiNetwork = wifiNet,
            wifiPassword = wifiPass,
            hostWhatsapp = prop.optString("phoneRaw", "+56972694597"),
            whatsappUrl = waUrl,
            heroBgImage = heroBg,
            faqs = faqsList
        )
    }

    /**
     * Envía la ficha resumida del atractivo turístico al WhatsApp del huésped que se encuentra
     * actualmente alojado en esta unidad física (considerando las 11:00 hrs de check-out).
     */
    suspend fun sendAttractionToGuestWhatsApp(
        attractionName: String,
        category: String,
        hours: String,
        price: String,
        drivingTime: String,
        googleMapsUrl: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val unit = preferencesHelper.getConfiguredUnit()
        val token = preferencesHelper.getUnitToken()
        val baseUrl = preferencesHelper.getApiBaseUrl()
        // El endpoint base suele ser https://elboom.cl/api/hotel-tv
        val endpoint = if (baseUrl.endsWith("/hotel-tv")) {
            baseUrl.replace("/hotel-tv", "/hotel-tv/send-attraction")
        } else {
            "https://elboom.cl/api/hotel-tv/send-attraction"
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("x-tv-token", token)
                }
                setRequestProperty("User-Agent", "ElBoom-SmartTV/${cl.elboomlicanray.tvlauncher.BuildConfig.VERSION_NAME}")
                useCaches = false
            }

            val jsonBody = JSONObject().apply {
                put("unitId", unit.id)
                put("token", token)
                put("attractionName", attractionName)
                put("category", category)
                put("hours", hours)
                put("price", price)
                put("drivingTime", drivingTime)
                put("googleMapsUrl", googleMapsUrl)
            }

            connection.outputStream.use { os ->
                val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val statusCode = connection.responseCode
            val responseStream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseText.isNotBlank()) {
                val json = JSONObject(responseText)
                if (json.optBoolean("success", false)) {
                    val guestName = json.optString("guestFirstName", "")
                    val msg = json.optString("message", "Información enviada exitosamente a tu WhatsApp")
                    val resultMsg = if (guestName.isNotBlank()) "¡Listo $guestName! Te enviamos los detalles a tu WhatsApp" else msg
                    Result.success(resultMsg)
                } else {
                    val err = json.optString("error", "No se pudo enviar el mensaje")
                    Result.failure(Exception(err))
                }
            } else {
                Result.failure(Exception("Error en servidor ($statusCode)"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando atractivo a WhatsApp: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error de conexión con el servidor"))
        } finally {
            connection?.disconnect()
        }
    }
}
