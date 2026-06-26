package com.example.receiver

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import java.io.File

class AdhanService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra("PRAYER_NAME") ?: ""
        val action = intent?.action

        if (action == "STOP_ADHAN") {
            stopPlayback()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Play the chosen Adhan audio
        startAdhan()

        // Create FGS Notification
        val notification = createNotification(prayerName)
        startForeground(9999, notification)

        return START_NOT_STICKY
    }

    private fun createNotification(prayerName: String): Notification {
        val channelId = "adhan_playback_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Adhan Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playing prayer time Adhan sound"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AdhanService::class.java).apply {
            action = "STOP_ADHAN"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1234,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isEn = com.example.viewmodel.GlobalLanguage.isEnglish
        val prayerDisplay = when (prayerName) {
            "Fajr", "ফজর" -> if (isEn) "Fajr" else "ফজর"
            "Dhuhr", "যোহর" -> if (isEn) "Dhuhr" else "যোহর"
            "Asr", "আসর" -> if (isEn) "Asr" else "আসর"
            "Maghrib", "মাগরিব" -> if (isEn) "Maghrib" else "মাগরিব"
            "Isha", "এশা" -> if (isEn) "Isha" else "এশা"
            else -> prayerName
        }

        val title = if (isEn) "Adhan is playing ($prayerDisplay)" else "আযান চলছে ($prayerDisplay)"
        val body = if (isEn) "Tap to stop the Adhan sound" else "আযান বন্ধ করতে এখানে ট্যাপ করুন"

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                if (isEn) "STOP" else "বন্ধ করুন",
                stopPendingIntent
            )
            .build()
    }

    private fun startAdhan() {
        val prefs = getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        val selectedAdhan = prefs.getString("pref_selected_adhan", "medina") ?: "medina"

        if (selectedAdhan == "none") {
            // Do not play anything, stop immediately
            stopSelf()
            return
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setOnCompletionListener {
                    stopSelf()
                }
            }

            var soundSourceSet = false

            when (selectedAdhan) {
                "mecca" -> {
                    mediaPlayer?.setDataSource("https://download.quranicaudio.com/adhan/makkah.mp3")
                    soundSourceSet = true
                }
                "medina" -> {
                    mediaPlayer?.setDataSource("https://download.quranicaudio.com/adhan/madinah.mp3")
                    soundSourceSet = true
                }
                "custom" -> {
                    val customFile = File(filesDir, "custom_adhan.mp3")
                    if (customFile.exists()) {
                        mediaPlayer?.setDataSource(customFile.absolutePath)
                        soundSourceSet = true
                    }
                }
                "pleasant" -> {
                    // Try to play a pleasant built-in system notification tone as a gentle alert
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    if (alertUri != null) {
                        mediaPlayer?.setDataSource(this, alertUri)
                        soundSourceSet = true
                    }
                }
            }

            if (soundSourceSet) {
                mediaPlayer?.prepareAsync()
                mediaPlayer?.setOnPreparedListener {
                    it.start()
                }
                mediaPlayer?.setOnErrorListener { _, _, _ ->
                    // Fallback to pleasant built-in alert if stream/file fails
                    playFallbackAlert()
                    true
                }
            } else {
                playFallbackAlert()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            playFallbackAlert()
        }
    }

    private fun playFallbackAlert() {
        try {
            mediaPlayer?.release()
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alertUri != null) {
                mediaPlayer = MediaPlayer.create(this, alertUri).apply {
                    start()
                    setOnCompletionListener {
                        stopSelf()
                    }
                }
            } else {
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
