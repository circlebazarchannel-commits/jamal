package com.example

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import com.example.receiver.AlarmReceiver
import com.example.receiver.AlarmService
import java.util.Calendar

class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // স্ক্রিন অফ থাকলে অন করা এবং লক স্ক্রিনের উপরে দেখানোর জন্য ফ্ল্যাগ সেট করা
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContent {
            com.example.ui.theme.MyApplicationTheme {
                androidx.compose.material3.Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            val label = intent.getStringExtra("ALARM_LABEL") ?: "HalalCircle Alarm"
                            androidx.compose.material3.Text(label, color = androidx.compose.ui.graphics.Color.White, fontSize = 24.sp)
                            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(32.dp))
                            androidx.compose.material3.Button(onClick = { 
                                val fromService = intent.getBooleanExtra("FROM_SERVICE", false)
                                val alarmId = intent.getIntExtra("ALARM_ID", -1)
                                if (fromService) {
                                    val stopServiceIntent = Intent(this@AlarmActivity, com.example.receiver.AlarmService::class.java)
                                    stopServiceIntent.action = "STOP_ALARM"
                                    stopService(stopServiceIntent)
                                    
                                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                    notificationManager.cancel(alarmId + 3000)
                                    notificationManager.cancel(alarmId + 4000)
                                } else {
                                    stopAlarmSound()
                                }
                                Toast.makeText(this@AlarmActivity, "অ্যালার্ম বন্ধ করা হলো।", Toast.LENGTH_SHORT).show()
                                finish()
                             }) {
                                androidx.compose.material3.Text("Stop Alarm")
                            }
                        }
                    }
                }
            }
        }

        val fromService = intent.getBooleanExtra("FROM_SERVICE", false)
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val ringtoneUriStr = intent.getStringExtra("RINGTONE_URI") ?: ""

        if (!fromService) {
            // ডিফল্ট অ্যালার্ম রিংটোন বাজানো শুরু করা
            val alarmUri = if (ringtoneUriStr.isNotEmpty()) {
                android.net.Uri.parse(ringtoneUriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()
        }
    }

    // নতুন করে অ্যালার্ম সেট করার ফাংশন
    private fun scheduleAlarm(triggerTimeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("IS_USER_ALARM", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + triggerTimeInMillis

        // অ্যান্ড্রয়েড ১২+ এর জন্য সঠিক সময়ে অ্যালার্ম ট্রিগার করার কোড
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun stopAlarmSound() {
        if (ringtone != null && ringtone!!.isPlaying) {
            ringtone!!.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound() // অ্যাক্টিভিটি কোনো কারণে বন্ধ হয়ে গেলে সাউন্ডও বন্ধ হবে
    }
}
