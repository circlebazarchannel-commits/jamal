package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object DuroodHelper {
    private const val PREFS_NAME = "durood_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL = "interval"
    private const val KEY_VOICE = "voice_enabled"
    private const val KEY_TEXT = "selected_text"
    
    private const val KEY_BUSY_ENABLED = "busy_enabled"
    private const val KEY_BUSY_START_MINS = "busy_start_mins"
    private const val KEY_BUSY_END_MINS = "busy_end_mins"
    private const val KEY_CUSTOM_VOICE_URI = "custom_voice_uri"
    
    private const val REQUEST_CODE = 8888

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun getIntervalMins(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL, 60)
    }

    fun isVoiceEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VOICE, true)
    }

    fun getSelectedText(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TEXT, "ﷺ") ?: "ﷺ"
    }

    fun isBusyEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BUSY_ENABLED, false)
    }

    fun getBusyStartMins(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BUSY_START_MINS, 1380) // default 11 PM
    }

    fun getBusyEndMins(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BUSY_END_MINS, 360) // default 6 AM
    }

    fun getCustomVoiceUri(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_VOICE_URI, null)
    }

    fun saveConfig(
        context: Context,
        enabled: Boolean,
        interval: Int,
        voiceEnabled: Boolean,
        text: String,
        busyEnabled: Boolean,
        busyStartMins: Int,
        busyEndMins: Int,
        customVoiceUri: String?
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_INTERVAL, interval)
            .putBoolean(KEY_VOICE, voiceEnabled)
            .putString(KEY_TEXT, text)
            .putBoolean(KEY_BUSY_ENABLED, busyEnabled)
            .putInt(KEY_BUSY_START_MINS, busyStartMins)
            .putInt(KEY_BUSY_END_MINS, busyEndMins)
            .putString(KEY_CUSTOM_VOICE_URI, customVoiceUri)
            .apply()

        if (enabled) {
            scheduleNextDurood(context)
        } else {
            cancelDurood(context)
        }
    }

    fun isCurrentlyBusy(context: Context): Boolean {
        if (!isBusyEnabled(context)) return false
        
        val startMins = getBusyStartMins(context)
        val endMins = getBusyEndMins(context)
        
        val cal = java.util.Calendar.getInstance()
        val currentMins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        
        return if (startMins < endMins) {
            currentMins in startMins..endMins
        } else {
            currentMins >= startMins || currentMins <= endMins
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        if (enabled) {
            scheduleNextDurood(context)
        } else {
            cancelDurood(context)
        }
    }

    fun setIntervalMins(context: Context, intervalMins: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL, intervalMins)
            .apply()

        // Reschedule if enabled
        if (isEnabled(context)) {
            scheduleNextDurood(context)
        }
    }

    fun setVoiceEnabled(context: Context, voiceEnabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VOICE, voiceEnabled)
            .apply()
    }

    fun setSelectedText(context: Context, text: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEXT, text)
            .apply()
    }

    fun scheduleNextDurood(context: Context) {
        if (!isEnabled(context)) {
            cancelDurood(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DuroodReceiver::class.java).apply {
            action = "com.example.ACTION_DUROOD_REMINDER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMs = getIntervalMins(context) * 60 * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMs

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d("DuroodHelper", "Durood reminder alarm scheduled in ${getIntervalMins(context)} minutes.")
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelDurood(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DuroodReceiver::class.java).apply {
            action = "com.example.ACTION_DUROOD_REMINDER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("DuroodHelper", "Durood reminder alarm cancelled.")
    }
}
