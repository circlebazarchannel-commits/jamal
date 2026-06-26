package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class DuroodReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.ACTION_DUROOD_REMINDER" || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            
            // 1. Reschedule next alarm
            DuroodHelper.scheduleNextDurood(context)

            // If action is boot or not enabled, don't show notification
            if (intent.action == Intent.ACTION_BOOT_COMPLETED || !DuroodHelper.isEnabled(context)) {
                return
            }

            // Check if it's currently busy time
            if (DuroodHelper.isCurrentlyBusy(context)) {
                return // skip reminding
            }

            // 2. Show notification
            showNotification(context)

            // 3. Play voice reminder if enabled
            if (DuroodHelper.isVoiceEnabled(context)) {
                playVoiceReminder(context)
            }
        }
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "durood_reminder_channel"
        val channelName = "দরুদ রিমাইন্ডার"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "দরুদ পড়ার জন্য সময়সূচী অনুযায়ী রিমাইন্ডার"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app on click
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1212,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "দরুদ পড়ুন"
        val body = DuroodHelper.getSelectedText(context)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Fallback to launcher round icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        ScreenWakeHelper.wakeScreen(context)

        notificationManager.notify(777, notification)
    }

    private fun playVoiceReminder(context: Context) {
        val customUriStr = DuroodHelper.getCustomVoiceUri(context)
        if (customUriStr != null) {
            try {
                val uri = android.net.Uri.parse(customUriStr)
                val mediaPlayer = android.media.MediaPlayer.create(context, uri)
                if (mediaPlayer != null) {
                    mediaPlayer.setOnCompletionListener { mp ->
                        mp.release()
                    }
                    mediaPlayer.start()
                    return // successfully started custom audio
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val resId = context.resources.getIdentifier("durood_voice", "raw", context.packageName)
        if (resId != 0) {
            try {
                val mediaPlayer = android.media.MediaPlayer.create(context, resId)
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.release()
                }
                mediaPlayer.start()
            } catch (e: Exception) {
                e.printStackTrace()
                DuroodVoicePlayer.speak(context, "দরুদ পড়ুন")
            }
        } else {
            DuroodVoicePlayer.speak(context, "দরুদ পড়ুন")
        }
    }
}

object DuroodVoicePlayer {
    private var tts: TextToSpeech? = null

    fun speak(context: Context, text: String) {
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeBn = Locale("bn", "BD")
                val result = tts?.setLanguage(localeBn)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "DuroodTTS")
            }
        }
    }
}
