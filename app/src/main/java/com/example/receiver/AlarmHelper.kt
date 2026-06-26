package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.calculator.PrayerCalculator
import com.example.calculator.PrayerTimes
import java.util.Calendar

object AlarmHelper {

    fun scheduleNextPrayer(context: Context, lat: Double, lng: Double, timezoneOffsetHor: Double, alarms: Map<String, Boolean>? = null, locationName: String? = null, isAuto: Boolean? = null) {
        saveState(context, lat, lng, timezoneOffsetHor, alarms, locationName, isAuto)
        val madhab = context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE).getInt("madhab", 2)
        
        val calendarToday = Calendar.getInstance()
        val timesToday = PrayerCalculator.calculatePrayerTimes(lat, lng, timezoneOffsetHor, madhab, calendarToday)
        
        val calendarTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val timesTomorrow = PrayerCalculator.calculatePrayerTimes(lat, lng, timezoneOffsetHor, madhab, calendarTomorrow)
        
        // Candidates definition
        data class PrayerCandidate(val name: String, val timeInMillis: Long, val isTomorrow: Boolean, val hourDecimal: Double)
        
        fun getMillisForPrayer(hourDecimal: Double, isTom: Boolean): Long {
            val cal = Calendar.getInstance()
            if (isTom) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val hour = Math.floor(hourDecimal).toInt()
            val minute = Math.floor((hourDecimal - hour) * 60).toInt()
            val second = Math.round(((hourDecimal - hour) * 60 - minute) * 60).toInt()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, second)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        
        val todayCandidates = listOf(
            PrayerCandidate("Fajr", getMillisForPrayer(timesToday.fajrHours, false), false, timesToday.fajrHours),
            PrayerCandidate("Sunrise", getMillisForPrayer(timesToday.sunriseHours, false), false, timesToday.sunriseHours),
            PrayerCandidate("Dhuhr", getMillisForPrayer(timesToday.dhuhrHours, false), false, timesToday.dhuhrHours),
            PrayerCandidate("Asr", getMillisForPrayer(timesToday.asrHours, false), false, timesToday.asrHours),
            PrayerCandidate("Maghrib", getMillisForPrayer(timesToday.maghribHours, false), false, timesToday.maghribHours),
            PrayerCandidate("Isha", getMillisForPrayer(timesToday.ishaHours, false), false, timesToday.ishaHours)
        )
        
        val tomorrowCandidates = listOf(
            PrayerCandidate("Fajr", getMillisForPrayer(timesTomorrow.fajrHours, true), true, timesTomorrow.fajrHours),
            PrayerCandidate("Sunrise", getMillisForPrayer(timesTomorrow.sunriseHours, true), true, timesTomorrow.sunriseHours),
            PrayerCandidate("Dhuhr", getMillisForPrayer(timesTomorrow.dhuhrHours, true), true, timesTomorrow.dhuhrHours),
            PrayerCandidate("Asr", getMillisForPrayer(timesTomorrow.asrHours, true), true, timesTomorrow.asrHours),
            PrayerCandidate("Maghrib", getMillisForPrayer(timesTomorrow.maghribHours, true), true, timesTomorrow.maghribHours),
            PrayerCandidate("Isha", getMillisForPrayer(timesTomorrow.ishaHours, true), true, timesTomorrow.ishaHours)
        )
        
        val allPrayers = todayCandidates + tomorrowCandidates
        
        // Filter based on user preference but ALWAYS include Sunrise and Maghrib for notifications
        val activePrayers = if (alarms != null) {
            allPrayers.filter { 
                alarms[it.name] == true || it.name == "Sunrise" || it.name == "Maghrib"
            }
        } else allPrayers
        
        val now = System.currentTimeMillis()
        
        // Find all future prayers (at least 1 second from now)
        val futurePrayers = activePrayers.filter { it.timeInMillis > now + 1000 }
            
        if (futurePrayers.isEmpty()) {
            cancelAlarm(context)
            return
        }
        
        futurePrayers.forEach { candidate ->
            schedulePrayerNotification(context, candidate.name, candidate.timeInMillis)
            
            // Schedule warning 10 minutes before
            val warningMinutes = 10
            val warningMillis = candidate.timeInMillis - (warningMinutes * 60 * 1000)
            if (warningMillis > now + 1000) {
                scheduleWarningAlarm(context, candidate.name, candidate.hourDecimal, candidate.isTomorrow)
            }
        }
        
        // Also schedule silent mode alarms
        SilentModeHelper.scheduleSilentAlarms(context, lat, lng, timezoneOffsetHor, madhab)
    }

    private fun schedulePrayerNotification(context: Context, waqtName: String, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            putExtra("WAQT_NAME", waqtName)
        }

        val requestCode = waqtName.hashCode()
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleWarningAlarm(context: Context, name: String, hourDecimal: Double, isTomorrow: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_APPROACHING"
            putExtra("PRAYER_NAME", name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            200, // Warning request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        if (isTomorrow) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val hour = Math.floor(hourDecimal).toInt()
        val minute = Math.floor((hourDecimal - hour) * 60).toInt()
        val second = Math.round(((hourDecimal - hour) * 60 - minute) * 60).toInt()
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, second)
        calendar.set(Calendar.MILLISECOND, 0)
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun saveState(context: Context, lat: Double, lng: Double, offset: Double, alarms: Map<String, Boolean>?, locationName: String? = null, isAuto: Boolean? = null) {
        val prefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("lat", lat.toFloat())
            putFloat("lng", lng.toFloat())
            putFloat("offset", offset.toFloat())
            alarms?.forEach { (name, enabled) ->
                putBoolean("alarm_$name", enabled)
            }
            if (locationName != null) {
                putString("saved_district", locationName)
            }
            if (isAuto != null) {
                putBoolean("is_auto_location", isAuto)
            }
            apply()
        }
    }

    fun reschedule(context: Context) {
        val prefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", 23.8103f).toDouble()
        val lng = prefs.getFloat("lng", 90.4125f).toDouble()
        val offset = prefs.getFloat("offset", 6.0f).toDouble()
        
        val alarms = mutableMapOf<String, Boolean>()
        listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
            alarms[name] = prefs.getBoolean("alarm_$name", name != "Sunrise")
        }
        
        scheduleNextPrayer(context, lat, lng, offset, alarms)
    }
}
