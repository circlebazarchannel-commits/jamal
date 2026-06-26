package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        // If needed, you can send this token to your server
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Firebase-এ কোনো User Data, App Data বা Database Content Store করা যাবে না।
        // Firebase-এর কাজ হবে শুধুমাত্র Free Push Notification পাঠানো।

        // Handle notification payload
        remoteMessage.notification?.let {
            val title = it.title ?: "ইসলামিক রিমাইন্ডার"
            val body = it.body ?: ""
            showNotification(title, body)
        }

        // Also handle data payload if custom processing is needed
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "রিমাইন্ডার"
            val body = remoteMessage.data["body"] ?: ""
            // if both data and notification payloads exist, Android may display notification automatically in background.
            // but we process here if frontend is active or if only data payload is sent.
            if (remoteMessage.notification == null) {
                showNotification(title, body)
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "fcm_default_channel"
        val channelName = "General Notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push Notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        ScreenWakeHelper.wakeScreen(this)

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
}
