package com.example.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.PrayerCalculator
import com.example.calculator.PrayerTimes
import com.example.receiver.AlarmHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

object GlobalLanguage {
    var isEnglish: Boolean = false
}

fun String.toBengali(): String {
    if (GlobalLanguage.isEnglish) {
        val ben = listOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "এএম", "পিএম")
        val eng = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "AM", "PM")
        var res = this
        ben.forEachIndexed { index, s ->
            res = res.replace(s, eng[index])
        }
        return res
    }
    val eng = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "AM", "PM", "am", "pm")
    val ben = listOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "এএম", "পিএম", "এএম", "পিএম")
    var res = this
    eng.forEachIndexed { index, s ->
        res = res.replace(s, ben[index])
    }
    return res
}

data class ViewState(
    val isLoading: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isAutoLocation: Boolean = true,
    val locationName: String = "ঢাকা",
    val latitude: Double = 23.8103,
    val longitude: Double = 90.4125,
    val selectedCountry: String = "বাংলাদেশ",
    val selectedDistrict: String = "ঢাকা",
    val currentDate: String = "",
    val prayerTimes: PrayerTimes? = null,
    val nextPrayerName: String = "Loading...",
    val nextPrayerNameBen: String = "...",
    val nextPrayerRemaining: String = "০০:০০:০০",
    val timerProgress: Float = 0f,
    val specialCountdownLabel: String = "সাহরির বাকি",
    val specialCountdownTime: String = "০০:০০:০০",
    val specialCountdownProgress: Float = 0f,
    val alarms: Map<String, Boolean> = mapOf(
        "Fajr" to true,
        "Sunrise" to false,
        "Dhuhr" to true,
        "Asr" to true,
        "Maghrib" to true,
        "Isha" to true
    ),
    val forbiddenSunrise: String = "০০:০০",
    val forbiddenSunriseEnd: String = "০০:০০",
    val forbiddenNoon: String = "০০:০০",
    val forbiddenNoonEnd: String = "০০:০০",
    val forbiddenSunset: String = "০০:০০",
    val forbiddenSunsetEnd: String = "০০:০০",
    val currentPrayerName: String = "",
    val currentPrayerNameBen: String = "",
    val rotatingNames: List<String> = emptyList(),
    val madhab: Int = 2, // 1 for Shafi, 2 for Hanafi
    val error: String? = null
)

class PrayerViewModel : ViewModel() {
    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var timerJob: Job? = null

    private var lastLat = 23.8103
    private var lastLng = 90.4125
    private var lastOffset = 6.0
    private var lastMadhab = 2
    private var hasLocationData = true

    init {
        // We will receive context later to load from prefs if needed, 
        // initially use defaults
        refreshState()
    }
    
    fun setMadhab(context: Context, m: Int) {
        lastMadhab = m
        context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE).edit().putInt("madhab", m).apply()
        _state.update { it.copy(madhab = m) }
        refreshState()
        com.example.widget.WidgetUtils.updateAllWidgets(context)
    }
    
    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)
        lastMadhab = prefs.getInt("madhab", 2)
        
        val alarmPrefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        val loadedAlarms = mutableMapOf<String, Boolean>()
        listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
            loadedAlarms[name] = alarmPrefs.getBoolean("alarm_$name", name != "Sunrise")
        }

        val isAuto = alarmPrefs.getBoolean("is_auto_location", true)
        
        val settingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedCountryCode = settingsPrefs.getString("selected_country_code", "BD") ?: "BD"
        
        var savedDist = alarmPrefs.getString("saved_district", "ঢাকা") ?: "ঢাকা"
        lastLat = alarmPrefs.getFloat("lat", 23.8103f).toDouble()
        lastLng = alarmPrefs.getFloat("lng", 90.4125f).toDouble()
        lastOffset = alarmPrefs.getFloat("offset", 6.0f).toDouble()
        
        // Ensure savedDist is from the current country if not auto location
        if (!isAuto) {
            val validDistricts = com.example.viewmodel.getDistrictsForCountry(savedCountryCode)
            val isValid = validDistricts.any { it.name == savedDist || it.englishName == savedDist }
            if (!isValid && validDistricts.isNotEmpty()) {
                val fallback = validDistricts.first()
                savedDist = fallback.name
                lastLat = fallback.lat
                lastLng = fallback.lng
                alarmPrefs.edit()
                    .putString("saved_district", savedDist)
                    .putFloat("lat", lastLat.toFloat())
                    .putFloat("lng", lastLng.toFloat())
                    .apply()
            }
        }
        
        hasLocationData = true

        _state.update { 
            it.copy(
                madhab = lastMadhab,
                alarms = loadedAlarms,
                isAutoLocation = isAuto,
                locationName = if (isAuto) "আমার অবস্থান" else savedDist,
                selectedDistrict = savedDist,
                selectedCountry = savedCountryCode,
                latitude = lastLat,
                longitude = lastLng
            ) 
        }
        refreshState()
        if (isAuto) {
            startLocationUpdates(context)
        }
    }

    private fun refreshState() {
        val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.US)
        val defaultTimes = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, lastMadhab)
        _state.update {
            it.copy(
                isLoading = false,
                currentDate = dateFormat.format(Date()).toBengali(),
                prayerTimes = defaultTimes,
                latitude = lastLat,
                longitude = lastLng
            )
        }
        calculateForbiddenTimes(defaultTimes)
        updateNextPrayer(defaultTimes)
    }

    private fun calculateForbiddenTimes(times: com.example.calculator.PrayerTimes) {
        val format = { h: Double ->
            val totalMin = (h * 60).toInt()
            val hour = (totalMin / 60) % 24
            val min = totalMin % 60
            val p = if (hour >= 12) "পিএম" else "এএম"
            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            String.format("%02d:%02d %s", displayHour, min, p).toBengali()
        }

        _state.update {
            it.copy(
                forbiddenSunrise = format(times.sunriseHours),
                forbiddenSunriseEnd = format(times.sunriseHours + 15.0 / 60.0),
                forbiddenNoon = format(times.dhuhrHours - 15.0 / 60.0),
                forbiddenNoonEnd = format(times.dhuhrHours),
                forbiddenSunset = format(times.maghribHours - 15.0 / 60.0),
                forbiddenSunsetEnd = format(times.maghribHours)
            )
        }
    }

    fun setLocationManually(context: Context, districtName: String, lat: Double, lng: Double) {
        lastLat = lat
        lastLng = lng
        lastOffset = 6.0 // Note: We might need to get real timezone offset, maybe we can fetch it, but 6 is BST. Let's try to calculate offset from current time and lat/lng if we can, or just keep 6.0? Wait, TimeZone.getDefault() is better.
        val timeZoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000.0 * 60.0 * 60.0)
        lastOffset = timeZoneOffset
        
        hasLocationData = true
        _state.update { 
            it.copy(
                isAutoLocation = false,
                locationName = districtName,
                selectedDistrict = districtName,
                latitude = lat,
                longitude = lng
            ) 
        }
        val alarmPrefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        alarmPrefs.edit()
            .putBoolean("is_auto_location", false)
            .putString("saved_district", districtName)
            .putFloat("lat", lat.toFloat())
            .putFloat("lng", lng.toFloat())
            .putFloat("offset", lastOffset.toFloat())
            .apply()
            
        refreshState()
        AlarmHelper.scheduleNextPrayer(
            context = context, 
            lat = lastLat, 
            lng = lastLng, 
            timezoneOffsetHor = lastOffset, 
            alarms = _state.value.alarms,
            locationName = districtName,
            isAuto = false
        )
        com.example.widget.WidgetUtils.updateAllWidgets(context)
    }

    fun setAutoLocation(context: Context) {
        _state.update { it.copy(isAutoLocation = true) }
        val alarmPrefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        alarmPrefs.edit().putBoolean("is_auto_location", true).apply()
        startLocationUpdates(context)
    }

    fun toggleAlarm(context: Context, prayerName: String) {
        _state.update { current ->
            val newAlarms = current.alarms.toMutableMap()
            newAlarms[prayerName] = !(newAlarms[prayerName] ?: true)
            current.copy(alarms = newAlarms)
        }
        if (hasLocationData) {
            AlarmHelper.scheduleNextPrayer(
                context = context, 
                lat = lastLat, 
                lng = lastLng, 
                timezoneOffsetHor = lastOffset, 
                alarms = _state.value.alarms,
                locationName = if (_state.value.isAutoLocation) "আমার অবস্থান" else _state.value.selectedDistrict,
                isAuto = _state.value.isAutoLocation
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        if (!_state.value.isAutoLocation) return

        val fineLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            coarseLocationPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            _state.update { it.copy(hasLocationPermission = false, isLoading = false, error = "Permission Required") }
            return
        }

        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }

        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
            locationCallback = null
        }

        _state.update { it.copy(hasLocationPermission = true) }

        // Fetch standard LocationManager known location
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        var lastKnownLoc: android.location.Location? = null
        if (locationManager != null) {
            try {
                val providers = locationManager.getProviders(true)
                for (provider in providers) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (lastKnownLoc == null || loc.time > lastKnownLoc.time) {
                            lastKnownLoc = loc
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Process resolved location helper
        fun processResolvedLocation(location: android.location.Location) {
            if (!_state.value.isAutoLocation) return
            val timeZoneOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000.0 * 60.0 * 60.0)
            lastLat = location.latitude
            lastLng = location.longitude
            lastOffset = timeZoneOffset
            hasLocationData = true

            val alarmPrefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
            alarmPrefs.edit()
                .putFloat("lat", lastLat.toFloat())
                .putFloat("lng", lastLng.toFloat())
                .putFloat("offset", lastOffset.toFloat())
                .apply()

            val times = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, lastMadhab)
            calculateForbiddenTimes(times)
            AlarmHelper.scheduleNextPrayer(
                context = context, 
                lat = lastLat, 
                lng = lastLng, 
                timezoneOffsetHor = lastOffset, 
                alarms = _state.value.alarms,
                locationName = "আমার অবস্থান",
                isAuto = true
            )

            _state.update { it.copy(prayerTimes = times, locationName = "আমার অবস্থান", latitude = lastLat, longitude = lastLng) }
            updateNextPrayer(times)
            com.example.widget.WidgetUtils.updateAllWidgets(context)
        }

        // If we have a very fresh location from LocationManager, prioritize it
        if (lastKnownLoc != null && (System.currentTimeMillis() - lastKnownLoc.time) < 5 * 60 * 1000) {
            processResolvedLocation(lastKnownLoc)
            return
        }

        // Query FusedLocationProviderClient first as it can be accurate, falling back immediately to LocationManager
        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location != null) {
                    processResolvedLocation(location)
                } else if (lastKnownLoc != null) {
                    processResolvedLocation(lastKnownLoc)
                } else {
                    // Try to request a single update via LocationManager to avoid continuous monitoring AppOps errors
                    try {
                        locationManager?.let { mgr ->
                            val provider = if (mgr.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                                android.location.LocationManager.NETWORK_PROVIDER
                            } else {
                                android.location.LocationManager.GPS_PROVIDER
                            }
                            if (mgr.isProviderEnabled(provider)) {
                                mgr.requestSingleUpdate(provider, object : android.location.LocationListener {
                                    override fun onLocationChanged(loc: android.location.Location) {
                                        processResolvedLocation(loc)
                                    }
                                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                                    override fun onProviderEnabled(provider: String) {}
                                    override fun onProviderDisabled(provider: String) {}
                                }, Looper.getMainLooper())
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }?.addOnFailureListener {
                if (lastKnownLoc != null) {
                    processResolvedLocation(lastKnownLoc)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (lastKnownLoc != null) {
                processResolvedLocation(lastKnownLoc)
            }
        }
    }
    
    fun setPermissionDenied() {
        _state.update { it.copy(hasLocationPermission = false, isLoading = false, error = "Permission Required") }
    }

    private fun updateNextPrayer(times: PrayerTimes) {
        val calendar = Calendar.getInstance()
        val currentHourDecimal = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0 + calendar.get(Calendar.SECOND) / 3600.0
        
        val sunriseHours = times.sunriseHours
        val dhuhrHours = times.dhuhrHours
        val asrHours = times.asrHours
        val maghribHours = times.maghribHours
        val ishaHours = times.ishaHours
        val fajrHours = times.fajrHours

        // Check if we are in the Nafl/Chasht period (Sunrise to Dhuhr)
        val isNaflPeriod = currentHourDecimal >= sunriseHours && currentHourDecimal < dhuhrHours

        var currentName = ""
        var currentNameBen = ""
        var currentStartTime = 0.0
        var currentEndTime = 0.0

        var nextName = ""
        var nextNameBen = ""

        if (isNaflPeriod) {
            // It's Nafl period! Between Sunrise and Dhuhr.
            currentName = "Duha" // triggers rotating names in center of circular timer
            currentNameBen = if (GlobalLanguage.isEnglish) "Chasht" else "চাশত"
            currentStartTime = sunriseHours
            currentEndTime = dhuhrHours

            nextName = "Dhuhr"
            nextNameBen = if (GlobalLanguage.isEnglish) "Dhuhr" else "যোহর"
        } else {
            // Find current and next among the 5 daily prayers
            // Fajr period: starts at fajrHours, ends at sunriseHours
            if (currentHourDecimal >= fajrHours && currentHourDecimal < sunriseHours) {
                currentName = "Fajr"
                currentNameBen = if (GlobalLanguage.isEnglish) "Fajr" else "ফজর"
                currentStartTime = fajrHours
                currentEndTime = sunriseHours

                nextName = "Dhuhr"
                nextNameBen = if (GlobalLanguage.isEnglish) "Dhuhr" else "যোহর"
            } 
            // Dhuhr period: dhuhrHours to asrHours
            else if (currentHourDecimal >= dhuhrHours && currentHourDecimal < asrHours) {
                currentName = "Dhuhr"
                currentNameBen = if (GlobalLanguage.isEnglish) "Dhuhr" else "যোহর"
                currentStartTime = dhuhrHours
                currentEndTime = asrHours

                nextName = "Asr"
                nextNameBen = if (GlobalLanguage.isEnglish) "Asr" else "আসর"
            }
            // Asr period: asrHours to maghribHours
            else if (currentHourDecimal >= asrHours && currentHourDecimal < maghribHours) {
                currentName = "Asr"
                currentNameBen = if (GlobalLanguage.isEnglish) "Asr" else "আসর"
                currentStartTime = asrHours
                currentEndTime = maghribHours

                nextName = "Maghrib"
                nextNameBen = if (GlobalLanguage.isEnglish) "Maghrib" else "মাগরিব"
            }
            // Maghrib period: maghribHours to ishaHours
            else if (currentHourDecimal >= maghribHours && currentHourDecimal < ishaHours) {
                currentName = "Maghrib"
                currentNameBen = if (GlobalLanguage.isEnglish) "Maghrib" else "মাগরিব"
                currentStartTime = maghribHours
                currentEndTime = ishaHours

                nextName = "Isha"
                nextNameBen = if (GlobalLanguage.isEnglish) "Isha" else "এশা"
            }
            // Isha period: ishaHours to next Fajr (with midnight rollover check)
            else {
                currentName = "Isha"
                currentNameBen = if (GlobalLanguage.isEnglish) "Isha" else "এশা"
                
                if (currentHourDecimal >= ishaHours) {
                    currentStartTime = ishaHours
                    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    val tomorrowTimes = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, lastMadhab, tomorrow)
                    currentEndTime = tomorrowTimes.fajrHours + 24.0
                } else {
                    // past midnight but before Fajr
                    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    val yesterdayTimes = PrayerCalculator.calculatePrayerTimes(lastLat, lastLng, lastOffset, lastMadhab, yesterday)
                    currentStartTime = yesterdayTimes.ishaHours - 24.0
                    currentEndTime = fajrHours
                }

                nextName = "Fajr"
                nextNameBen = if (GlobalLanguage.isEnglish) "Fajr" else "ফজর"
            }
        }

        _state.update { 
            it.copy(
                currentPrayerName = currentName, 
                currentPrayerNameBen = currentNameBen,
                nextPrayerName = nextName,
                nextPrayerNameBen = nextNameBen
            ) 
        }
        
        // Handle rotating names for Nafl period (Between Sunrise and Dhuhr)
        val rotating = if (isNaflPeriod) {
            if (GlobalLanguage.isEnglish) listOf("Ishraq", "Chasht", "Duha") 
            else listOf("ইশরাক", "চাশত", "দোহা")
        } else {
            emptyList()
        }
        _state.update { it.copy(rotatingNames = rotating) }

        startCountdownTimer(currentEndTime, currentStartTime, times)
    }

    private fun startCountdownTimer(targetHour: Double, startHour: Double, todayTimes: PrayerTimes) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while(true) {
                val cal = Calendar.getInstance()
                val currentHourDec = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE)/60.0 + cal.get(Calendar.SECOND)/3600.0
                
                // Current Prayer Countdown (How much time left for current prayer to end)
                var diff = targetHour - currentHourDec
                if (diff < 0) diff += 24.0
                
                val totalDuration = targetHour - startHour
                val progress = ((currentHourDec - startHour) / totalDuration).coerceIn(0.0, 1.0).toFloat()
                
                val h = Math.floor(diff).toInt()
                val m = Math.floor((diff - h)*60).toInt()
                val s = Math.floor(((diff - h)*60 - m)*60).toInt()
                
                val timeStr = String.format("%02d:%02d:%02d", h, m, s).toBengali()
                
                // Sehri / Iftar Countdown
                // Sehri ends at Fajr. Iftar starts at Maghrib.
                val fajr = todayTimes.fajrHours
                val maghrib = todayTimes.maghribHours
                
                var specialLabel = if (GlobalLanguage.isEnglish) "Sehri Remaining" else "সাহরির বাকি"
                var targetHour = fajr
                var startHour = maghrib - 24.0 // yesterday's maghrib as fallback

                if (currentHourDec > fajr && currentHourDec < maghrib) {
                    // It's daytime, count down to Iftar
                    specialLabel = if (GlobalLanguage.isEnglish) "Iftar Remaining" else "ইফতারের বাকি"
                    targetHour = maghrib
                    startHour = fajr
                } else {
                    // It's nighttime, count down to Sehri
                    specialLabel = if (GlobalLanguage.isEnglish) "Sehri Remaining" else "সাহরির বাকি"
                    targetHour = if (currentHourDec > maghrib) fajr + 24.0 else fajr
                    startHour = if (currentHourDec > maghrib) maghrib else maghrib - 24.0
                }

                var specDiff = targetHour - currentHourDec
                val specTotal = targetHour - startHour
                val specProgress = ((currentHourDec - startHour) / specTotal).coerceIn(0.0, 1.0).toFloat()
                
                val sh = Math.floor(specDiff).toInt()
                val sm = Math.floor((specDiff - sh)*60).toInt()
                val ss = Math.floor(((specDiff - sh)*60 - sm)*60).toInt()
                val specTimeStr = String.format("%02d:%02d:%02d", sh, sm, ss).toBengali()

                _state.update { it.copy(
                    nextPrayerRemaining = timeStr,
                    timerProgress = progress,
                    specialCountdownLabel = specialLabel,
                    specialCountdownTime = specTimeStr,
                    specialCountdownProgress = specProgress
                ) }
                
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        timerJob?.cancel()
    }
}
