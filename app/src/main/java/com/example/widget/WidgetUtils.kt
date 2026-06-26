package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.calculator.PrayerCalculator
import com.example.calculator.PrayerTimes
import com.example.HijriCalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object WidgetUtils {

    fun isEnglishMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getString("language", "bn") == "en"
    }

    fun toBengaliNumber(number: Int): String {
        return HijriCalendarHelper.toBengaliNumber(number)
    }

    fun pinWidget(context: Context, providerClassName: String) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val providerClass = Class.forName(providerClassName)
            val myProvider = ComponentName(context, providerClass)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val successCallback = android.app.PendingIntent.getBroadcast(
                        context,
                        199,
                        Intent(context, providerClass).apply {
                            action = "com.example.widget.PIN_SUCCESS"
                        },
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                    
                    val msg = if (isEnglishMode(context)) {
                        "Requesting to add widget to Home Screen..."
                    } else {
                        "হোম স্ক্রিনে উইজেট যোগ করার অনুরোধ করা হচ্ছে..."
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val msg = if (isEnglishMode(context)) {
                        "Pinning widget is not supported on your launcher."
                    } else {
                        "আপনার লঞ্চারে সরাসরি উইজেট পিন করা সমর্থিত নয়।"
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                val msg = if (isEnglishMode(context)) {
                    "Pinning widgets requires Android 8.0 or higher."
                } else {
                    "উইজেট সরাসরি পিন করতে অ্যান্ড্রয়েড ৮.০ বা তার বেশি প্রয়োজন।"
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun String.toBengali(): String {
        val eng = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "AM", "PM", "am", "pm")
        val ben = listOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "এএম", "পিএম", "এএম", "পিএম")
        var res = this
        eng.forEachIndexed { index, s ->
            res = res.replace(s, ben[index])
        }
        return res
    }

    data class WidgetData(
        val isEnglish: Boolean,
        val locationName: String,
        val gregorianDate: String,
        val hijriDate: String,
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String,
        val nextPrayerName: String,
        val nextPrayerCountdown: String,
        val sunriseTimeOnly: String,
        val sunsetTimeOnly: String
    )

    fun getWidgetData(context: Context): WidgetData {
        val isEng = isEnglishMode(context)

        // Load location preferences
        val alarmPrefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        val isAuto = alarmPrefs.getBoolean("is_auto_location", true)
        val savedDist = alarmPrefs.getString("saved_district", "ঢাকা") ?: "ঢাকা"
        val lat = alarmPrefs.getFloat("lat", 23.8103f).toDouble()
        val lng = alarmPrefs.getFloat("lng", 90.4125f).toDouble()
        val offset = alarmPrefs.getFloat("offset", 6.0f).toDouble()

        // Load madhab
        val prefs = context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)
        val madhab = prefs.getInt("madhab", 2)

        val cal = Calendar.getInstance()
        val times = PrayerCalculator.calculatePrayerTimes(lat, lng, offset, madhab, cal)

        // Format Gregorian Date
        val gregFormat = if (isEng) {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US)
        } else {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US)
        }
        var gregStr = gregFormat.format(Date())
        if (!isEng) {
            // Translate weekdays and months to Bengali manually or via mapping to avoid locale mismatches
            gregStr = translateGregorianToBengali(gregStr)
        }

        // Format Hijri Date
        val hDate = HijriCalendarHelper.gregorianToHijri(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val hijriMonth = if (isEng) {
            HijriCalendarHelper.enHijriMonths[hDate.month - 1]
        } else {
            HijriCalendarHelper.bnHijriMonths[hDate.month - 1]
        }
        val hijriDay = if (isEng) hDate.day.toString() else toBengaliNumber(hDate.day)
        val hijriYear = if (isEng) hDate.year.toString() else toBengaliNumber(hDate.year)
        val hijriStr = if (isEng) {
            "$hijriDay $hijriMonth, $hijriYear AH"
        } else {
            "$hijriDay $hijriMonth, $hijriYear হিজরি"
        }

        // Selected location display name
        val locName = if (isAuto) {
            if (isEng) "My Location" else "আমার অবস্থান"
        } else {
            if (isEng) translateDistrictToEnglish(savedDist) else savedDist
        }

        // Format daily prayers
        val formatTime = { h: Double ->
            val totalMin = (h * 60).toInt()
            val hour = (totalMin / 60) % 24
            val min = totalMin % 60
            val p = if (hour >= 12) (if (isEng) "PM" else "পিএম") else (if (isEng) "AM" else "এএম")
            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            val timeFormatted = String.format("%02d:%02d %s", displayHour, min, p)
            if (isEng) timeFormatted else timeFormatted.toBengali()
        }

        val fajrStr = formatTime(times.fajrHours)
        val sunriseStr = formatTime(times.sunriseHours)
        val dhuhrStr = formatTime(times.dhuhrHours)
        val asrStr = formatTime(times.asrHours)
        val maghribStr = formatTime(times.maghribHours)
        val ishaStr = formatTime(times.ishaHours)

        // Sunrise/sunset raw time only (for smaller widget)
        val formatTimeOnly = { h: Double ->
            val totalMin = (h * 60).toInt()
            val hour = (totalMin / 60) % 24
            val min = totalMin % 60
            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            val p = if (hour >= 12) (if (isEng) "PM" else "PM") else (if (isEng) "AM" else "AM")
            val timeFormatted = String.format("%02d:%02d %s", displayHour, min, p)
            if (isEng) timeFormatted else timeFormatted.toBengali()
        }

        val sunriseTimeOnly = formatTimeOnly(times.sunriseHours)
        val sunsetTimeOnly = formatTimeOnly(times.maghribHours)

        // Calculate next prayer and countdown
        val currentHourDec = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0 + cal.get(Calendar.SECOND) / 3600.0
        val isNaflPeriod = currentHourDec >= times.sunriseHours && currentHourDec < times.dhuhrHours

        var nextName = ""
        var nextHour = 0.0

        if (isNaflPeriod) {
            nextName = if (isEng) "Dhuhr" else "যোহর"
            nextHour = times.dhuhrHours
        } else {
            if (currentHourDec < times.fajrHours) {
                nextName = if (isEng) "Fajr" else "ফজর"
                nextHour = times.fajrHours
            } else if (currentHourDec < times.sunriseHours) {
                // Technically Sunrise is next, but standard daily prayer is Dhuhr
                nextName = if (isEng) "Dhuhr" else "যোহর"
                nextHour = times.dhuhrHours
            } else if (currentHourDec < times.dhuhrHours) {
                nextName = if (isEng) "Dhuhr" else "যোহর"
                nextHour = times.dhuhrHours
            } else if (currentHourDec < times.asrHours) {
                nextName = if (isEng) "Asr" else "আসর"
                nextHour = times.asrHours
            } else if (currentHourDec < times.maghribHours) {
                nextName = if (isEng) "Maghrib" else "মাগরিব"
                nextHour = times.maghribHours
            } else if (currentHourDec < times.ishaHours) {
                nextName = if (isEng) "Isha" else "এশা"
                nextHour = times.ishaHours
            } else {
                // Next is tomorrow's Fajr
                nextName = if (isEng) "Fajr" else "ফজর"
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowTimes = PrayerCalculator.calculatePrayerTimes(lat, lng, offset, madhab, tomorrow)
                nextHour = tomorrowTimes.fajrHours + 24.0
            }
        }

        var diff = nextHour - currentHourDec
        if (diff < 0) diff += 24.0

        val h = Math.floor(diff).toInt()
        val m = Math.floor((diff - h) * 60).toInt()
        val s = Math.floor(((diff - h) * 60 - m) * 60).toInt()

        val countdownStr = String.format("%02d:%02d:%02d", h, m, s)
        val countdownDisplay = if (isEng) countdownStr else countdownStr.toBengali()

        return WidgetData(
            isEnglish = isEng,
            locationName = locName,
            gregorianDate = gregStr,
            hijriDate = hijriStr,
            fajr = fajrStr,
            sunrise = sunriseStr,
            dhuhr = dhuhrStr,
            asr = asrStr,
            maghrib = maghribStr,
            isha = ishaStr,
            nextPrayerName = nextName,
            nextPrayerCountdown = countdownDisplay,
            sunriseTimeOnly = sunriseTimeOnly,
            sunsetTimeOnly = sunsetTimeOnly
        )
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val prayerProvider = ComponentName(context, "com.example.widget.PrayerTimesWidgetProvider")
        try {
            val prayerIds = appWidgetManager.getAppWidgetIds(prayerProvider)
            if (prayerIds.isNotEmpty()) {
                val intent = Intent(context, Class.forName("com.example.widget.PrayerTimesWidgetProvider")).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, prayerIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val nextProvider = ComponentName(context, "com.example.widget.NextPrayerWidgetProvider")
        try {
            val nextIds = appWidgetManager.getAppWidgetIds(nextProvider)
            if (nextIds.isNotEmpty()) {
                val intent = Intent(context, Class.forName("com.example.widget.NextPrayerWidgetProvider")).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, nextIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sunriseProvider = ComponentName(context, "com.example.widget.SunriseSunsetWidgetProvider")
        try {
            val sunriseIds = appWidgetManager.getAppWidgetIds(sunriseProvider)
            if (sunriseIds.isNotEmpty()) {
                val intent = Intent(context, Class.forName("com.example.widget.SunriseSunsetWidgetProvider")).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, sunriseIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sunriseOnlyProvider = ComponentName(context, "com.example.widget.SunriseWidgetProvider")
        try {
            val sunriseOnlyIds = appWidgetManager.getAppWidgetIds(sunriseOnlyProvider)
            if (sunriseOnlyIds.isNotEmpty()) {
                val intent = Intent(context, Class.forName("com.example.widget.SunriseWidgetProvider")).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, sunriseOnlyIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sunsetOnlyProvider = ComponentName(context, "com.example.widget.SunsetWidgetProvider")
        try {
            val sunsetOnlyIds = appWidgetManager.getAppWidgetIds(sunsetOnlyProvider)
            if (sunsetOnlyIds.isNotEmpty()) {
                val intent = Intent(context, Class.forName("com.example.widget.SunsetWidgetProvider")).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, sunsetOnlyIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dateProvider = ComponentName(context, "com.example.widget.DateWidgetProvider")
        try {
            val dateIds = appWidgetManager.getAppWidgetIds(dateProvider)
            if (dateIds.isNotEmpty()) {
                val intent = Intent(context, Class.forName("com.example.widget.DateWidgetProvider")).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, dateIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun translateGregorianToBengali(engDateStr: String): String {
        var res = engDateStr
        val weekdayMap = mapOf(
            "Sunday" to "রবিবার", "Monday" to "সোমবার", "Tuesday" to "মঙ্গলবার",
            "Wednesday" to "বুধবার", "Thursday" to "বৃহস্পতিবার", "Friday" to "শুক্রবার", "Saturday" to "শনিবার"
        )
        val monthMap = mapOf(
            "January" to "জানুয়ারি", "February" to "ফেব্রুয়ারি", "March" to "মার্চ", "April" to "এপ্রিল",
            "May" to "মে", "June" to "জুন", "July" to "জুলাই", "August" to "আগস্ট",
            "September" to "সেপ্টেম্বর", "October" to "অক্টোবর", "November" to "নভেম্বর", "December" to "ডিসেম্বর"
        )

        weekdayMap.forEach { (eng, ben) -> res = res.replace(eng, ben) }
        monthMap.forEach { (eng, ben) -> res = res.replace(eng, ben) }
        return res.toBengali()
    }

    private fun translateDistrictToEnglish(bnDistrict: String): String {
        val mapping = mapOf(
            "ঢাকা" to "Dhaka", "চট্টগ্রাম" to "Chittagong", "সিলেট" to "Sylhet", "রাজশাহী" to "Rajshahi",
            "খুলনা" to "Khulna", "বরিশাল" to "Barisal", "রংপুর" to "Rangpur", "ময়মনসিংহ" to "Mymensingh",
            "কুমিল্লা" to "Comilla", "গাজীপুর" to "Gazipur", "নারায়ণগঞ্জ" to "Narayanganj"
        )
        return mapping[bnDistrict] ?: bnDistrict
    }
}
