package com.example

import android.app.Application
import com.example.config.AppCore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(AppCore.k1())
                    .setApplicationId(AppCore.k2())
                    .setProjectId(AppCore.k3())
                    .setStorageBucket(AppCore.k4())
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
