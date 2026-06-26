package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.database.NotificationEntity
import com.example.database.TrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "HalalCircle:AlarmWakeLock")
        wakeLock.acquire(10000) // 10 seconds wake lock
        
        try {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
                AlarmHelper.reschedule(context)
                
                // Reschedule User Alarms
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    val viewModel = com.example.viewmodel.AlarmViewModel(context)
                    viewModel.rescheduleAllAlarms()
                }
                return
            }

            val isUserAlarm = intent.getBooleanExtra("IS_USER_ALARM", false)
            if (isUserAlarm) {
                val alarmId = intent.getIntExtra("ALARM_ID", -1)
                val label = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
                val ringtoneUri = intent.getStringExtra("RINGTONE_URI") ?: ""
                
                // Start Foreground Service for reliable alarm
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("ALARM_ID", alarmId)
                    putExtra("ALARM_LABEL", label)
                    putExtra("RINGTONE_URI", ringtoneUri)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                return
            }

            val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
            
            val prayerNameBen = when(prayerName) {
                "Fajr" -> "ফজর"
                "Sunrise" -> "সূর্যোদয়"
                "Dhuhr" -> "যোহর"
                "Asr" -> "আসর"
                "Maghrib" -> "মাগরিব"
                "Isha" -> "এশা"
                else -> prayerName
            }

            if (intent.action == "com.example.ACTION_PRAYER_APPROACHING") {
                showWarningNotification(context, prayerNameBen)
                return
            }

            val calendar = java.util.Calendar.getInstance()
            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            val year = calendar.get(java.util.Calendar.YEAR)
            val dateKey = "prayer_notified_${prayerName}_${year}_${dayOfYear}"
            
            val prefs = context.getSharedPreferences("prayer_notification_history", Context.MODE_PRIVATE)
            val alreadyNotified = prefs.getBoolean(dateKey, false)
            
            if (!alreadyNotified) {
                prefs.edit().putBoolean(dateKey, true).apply()
                showNotification(context, prayerNameBen)
                saveNotificationToDb(context, prayerNameBen)
            }
            
            // Reschedule for the next prayer
            AlarmHelper.reschedule(context)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun saveNotificationToDb(context: Context, prayerName: String) {
        val title = when(prayerName) {
            "সূর্যোদয়" -> "সূর্যোদয় হয়েছে"
            "মাগরিব" -> "সূর্যাস্ত হয়েছে (মাগরিব)"
            else -> "ওয়াক্ত পরিবর্তন হয়েছে: $prayerName"
        }
        val body = when(prayerName) {
            "সূর্যোদয়" -> "এখন সূর্যোদয় হয়েছে। ফজরের ওয়াক্ত শেষ।"
            "মাগরিব" -> "সূর্যাস্ত হয়েছে। এখন মাগরিবের ওয়াক্ত।"
            else -> "এখন $prayerName এর সময়। দয়া করে নামাজের জন্য প্রস্তুতি নিন।"
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TrackerDatabase.getDatabase(context)
                val notificationDao = db.notificationDao()
                val notification = NotificationEntity(
                    title = title,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    type = "GENERAL",
                    actorName = "সালাত রিমাইন্ডার",
                    itemType = "prayer"
                )
                notificationDao.insertNotification(notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(context: Context, prayerName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_times_channel_high"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Prayer Times Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for prayer times"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val title = when(prayerName) {
            "সূর্যোদয়" -> "সূর্যোদয় হয়েছে"
            "মাগরিব" -> "সূর্যাস্ত হয়েছে (মাগরিব)"
            else -> "ওয়াক্ত পরিবর্তন হয়েছে: $prayerName"
        }
        val body = when(prayerName) {
            "সূর্যোদয়" -> "এখন সূর্যোদয় হয়েছে। ফজরের ওয়াক্ত শেষ।"
            "মাগরিব" -> "সূর্যাস্ত হয়েছে। এখন মাগরিবের ওয়াক্ত।"
            else -> "এখন $prayerName এর সময়। দয়া করে নামাজের জন্য প্রস্তুতি নিন।"
        }

        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Play Adhan sound if it's a real prayer and not Sunrise
        if (prayerName != "সূর্যোদয়" && prayerName != "Sunrise") {
            try {
                val adhanIntent = Intent(context, AdhanService::class.java).apply {
                    putExtra("PRAYER_NAME", prayerName)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(adhanIntent)
                } else {
                    context.startService(adhanIntent)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        ScreenWakeHelper.wakeScreen(context)
        notificationManager.notify(prayerName.hashCode(), notification)
    }

    private fun showWarningNotification(context: Context, prayerName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_times_channel_high"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Prayer Times Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for prayer times"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val title = "নামাজের সময় ঘনিয়ে আসছে"
        val body = "কিছুক্ষণ পর $prayerName সালাতের সময় শুরু হবে। ওযু করে প্রস্তুতি নিন।"

        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode() + 500,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        ScreenWakeHelper.wakeScreen(context)

        notificationManager.notify(prayerName.hashCode() + 500, notification)
    }

    private fun showAlarmNotification(context: Context, alarmId: Int, label: String, ringtoneUri: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "user_alarms_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "User Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for your set alarms"
                setSound(null, null) // We play sound in Activity
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmIntent = Intent(context, com.example.AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            putExtra("RINGTONE_URI", ringtoneUri)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId + 2000,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label)
            .setContentText("Tap to stop alarm")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        ScreenWakeHelper.wakeScreen(context)
        notificationManager.notify(alarmId + 3000, notification)
        
        // Also start the activity directly as fallback for some devices
        context.startActivity(alarmIntent)
    }
}
