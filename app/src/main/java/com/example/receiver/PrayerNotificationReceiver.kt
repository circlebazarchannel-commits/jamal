package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val waqtName = intent.getStringExtra("WAQT_NAME") ?: "পরবর্তী ওয়াক্ত"

        // ১. মোবাইল স্ক্রিন অফ থাকলে তা জ্বালিয়ে তোলার জন্য WakeLock (৩ সেকেন্ডের জন্য)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "PrayerApp:NotificationWakeLock"
        )
        wakeLock.acquire(3000) // ৩০০০ মিলিসেকেন্ড = ৩ সেকেন্ড স্ক্রিন অন থাকবে

        // ২. নোটিফিকেশন চ্যানেল তৈরি (Android 8.0+ এর জন্য IMPORTANCE_HIGH বাধ্যতামূলক)
        val channelId = "PRAYER_ALERT_CHANNEL"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Prayer Time Alerts",
                NotificationManager.IMPORTANCE_HIGH // এটি সাউন্ড এবং পপ-আপ নিশ্চিত করে
            ).apply {
                description = "নামাজের ওয়াক্ত পরিবর্তনের নোটিফিকেশন"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // ৩. নোটিফিকেশনের টেক্সট তৈরি
        val prayerNameBen = when(waqtName) {
            "Fajr" -> "ফজর"
            "Sunrise" -> "সূর্যোদয়"
            "Dhuhr" -> "যোহর"
            "Asr" -> "আসর"
            "Maghrib" -> "মাগরিব"
            "Isha" -> "এশা"
            else -> waqtName
        }

        val title = when(waqtName) {
            "Sunrise", "সূর্যোদয়" -> "সূর্যোদয় হয়েছে"
            "Maghrib", "মাগরিব" -> "সূর্যাস্ত হয়েছে (মাগরিব)"
            else -> "$prayerNameBen-এর ওয়াক্ত শুরু হয়েছে"
        }
        val message = when(waqtName) {
            "Sunrise", "সূর্যোদয়" -> "এখন সূর্যোদয় হয়েছে। ফজরের ওয়াক্ত শেষ।"
            "Maghrib", "মাগরিব" -> "সূর্যাস্ত হয়েছে। এখন মাগরিবের ওয়াক্ত।"
            else -> "পূর্বের ওয়াক্ত শেষ হয়েছে। ওজু এবং মিসওয়াক করে এই ওয়াক্তের জন্য প্রস্তুতি নিন।"
        }

        // নোটিফিকেশনে ক্লিক করলে অ্যাপ ওপেন হওয়ার জন্য Intent (ঐচ্ছিক)
        val adminIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, waqtName.hashCode(), adminIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ৪. নোটিফিকেশন বিল্ড করা
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7.1 বা তার নিচের জন্য
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // ডিফল্ট সাউন্ড ও ভাইব্রেশন অন করবে
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // নোটিফিকেশন পুশ করা (প্রতিটি ওয়াক্তের জন্য আলাদা ID দিতে hashCode ব্যবহার করা হয়েছে)
        notificationManager.notify(waqtName.hashCode(), builder.build())
        
        // Play Adhan sound if it's a real prayer and not Sunrise
        if (waqtName != "সূর্যোদয়" && waqtName != "Sunrise") {
            try {
                val adhanIntent = Intent(context, AdhanService::class.java).apply {
                    putExtra("PRAYER_NAME", prayerNameBen)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(adhanIntent)
                } else {
                    context.startService(adhanIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        saveNotificationToDb(context, title, message)
        
        // Schedule next alarms if needed
        AlarmHelper.reschedule(context)
    }
    
    private fun saveNotificationToDb(context: Context, title: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = com.example.database.TrackerDatabase.getDatabase(context)
                val notificationDao = db.notificationDao()
                val notification = com.example.database.NotificationEntity(
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
}
