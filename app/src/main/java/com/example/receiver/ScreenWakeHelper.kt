package com.example.receiver

import android.content.Context
import android.os.PowerManager
import android.os.Build

object ScreenWakeHelper {
    /**
     * Wakes up the screen of the device safely.
     * Uses PowerManager WakeLock with ACQUIRE_CAUSES_WAKEUP.
     */
    fun wakeScreen(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            
            // Check if the screen is already active
            val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager.isInteractive
            } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn
            }

            if (!isScreenOn) {
                @Suppress("DEPRECATION")
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "HalalCircle::ScreenWakeupNotification"
                )
                // Acquire for 6 seconds to light up the screen, then auto-release
                wakeLock.acquire(6000)
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenWakeHelper", "Exception while waking screen on notification", e)
        }
    }
}
