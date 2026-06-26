package com.example

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.viewmodel.GlobalLanguage
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.json.JSONObject

class AlertUploadService : Service() {

    private val CHANNEL_ID = "alert_upload_channel"
    private val NOTIFICATION_ID = 1003

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaUriStr = intent?.getStringExtra("mediaUri")
        val mediaType = intent?.getStringExtra("mediaType") ?: "photo"
        val title = intent?.getStringExtra("title") ?: ""
        val description = intent?.getStringExtra("description") ?: ""
        val contactNumber = intent?.getStringExtra("contactNumber") ?: ""
        val location = intent?.getStringExtra("location") ?: ""
        val docId = intent?.getStringExtra("docId") ?: ""

        if (mediaUriStr != null) {
            val mediaUri = Uri.parse(mediaUriStr)
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createProgressNotification(0, 0))
            uploadMedia(mediaUri, mediaType, title, description, contactNumber, location, docId)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Alert Upload Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createProgressNotification(progress: Int, totalSize: Long): Notification {
        val isEnglish = GlobalLanguage.isEnglish
        val titleText = if (isEnglish) "Uploading Alert..." else "অ্যালার্ট আপলোড হচ্ছে..."
        
        val progressText = if (totalSize > 0) {
            val sizeMb = String.format("%.2f", totalSize / (1024f * 1024f))
            "$progress% ($sizeMb MB)"
        } else {
            "$progress%"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun uploadMedia(mediaUri: Uri, mediaType: String, title: String, description: String, contactNumber: String, location: String, docId: String) {
        val chatId = "-1002647379129"
        val botToken = "8968904429:AAE3Ce849ysMuaxQhdMebsBwyB_nlIPQ1Os"
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)

        Thread {
            try {
                var totalSize = 0L
                contentResolver.query(mediaUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            totalSize = cursor.getLong(sizeIndex)
                        }
                    }
                }
                if (totalSize == 0L) {
                    contentResolver.openInputStream(mediaUri)?.use { totalSize = it.available().toLong() }
                }

                val mimeType = if (mediaType == "photo") "image/jpeg" else "video/mp4"
                val fieldName = if (mediaType == "photo") "photo" else "video"
                val urlParam = if (mediaType == "photo") "sendPhoto" else "sendVideo"

                val mediaBody = object : RequestBody() {
                    override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()
                    override fun contentLength(): Long = totalSize
                    override fun writeTo(sink: BufferedSink) {
                        contentResolver.openInputStream(mediaUri)?.use { input ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var uploaded = 0L
                            var lastUpdate = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                                uploaded += read
                                val currentProgress = ((uploaded * 100) / totalSize.coerceAtLeast(1L)).toInt()
                                if (currentProgress > lastUpdate) {
                                    notificationManager.notify(NOTIFICATION_ID, createProgressNotification(currentProgress, totalSize))
                                    lastUpdate = currentProgress.toLong()
                                }
                            }
                        }
                    }
                }

                val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("caption", "🚨 **Circle Alert!**\n\n**ID:** $docId\n**Location:** $location\n**Contact:** $contactNumber\n**Title:** $title\n**Description:** $description\n\nএডমিন, দয়া করে অ্যালার্টটি পর্যালোচনা করে অ্যাপ্রুভ বা রিজেক্ট করুন।")
                    .addFormDataPart("reply_markup", """
                        {
                            "inline_keyboard": [
                                [
                                    {"text": "Approve ✅", "callback_data": "approve_$docId"},
                                    {"text": "Reject ❌", "callback_data": "reject_$docId"}
                                ],
                                [
                                    {"text": "Permanently Delete 🗑️", "callback_data": "delete_$docId"}
                                ]
                            ]
                        }
                    """.trimIndent())
                    .addFormDataPart(fieldName, if(mediaType == "photo") "upload.jpg" else "upload.mp4", mediaBody)

                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/$urlParam")
                    .post(bodyBuilder.build())
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseStr = response.body?.string()
                    handleSuccess(responseStr, docId, mediaType)
                } else {
                    handleError("Server error: ${response.code}")
                }
            } catch (e: Exception) {
                handleError(e.message ?: "Unknown error")
                e.printStackTrace()
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    private fun handleSuccess(responseStr: String?, docId: String, mediaType: String) {
        if (responseStr != null) {
            try {
                val jsonObj = JSONObject(responseStr)
                if (jsonObj.optBoolean("ok")) {
                    val result = jsonObj.optJSONObject("result")
                    val fileId = if (mediaType == "photo") {
                        val photoArray = result?.optJSONArray("photo")
                        photoArray?.optJSONObject(photoArray.length() - 1)?.optString("file_id")
                    } else {
                        result?.optJSONObject("video")?.optString("file_id")
                    }
                    if (!fileId.isNullOrEmpty()) {
                        FirebaseFirestore.getInstance().collection("videos").document(docId)
                            .update("telegramFileId", fileId)
                    }
                    showCompletionNotification()
                } else {
                    handleError("Telegram API error")
                }
            } catch (e: Exception) {
                handleError("Parsing error")
            }
        }
    }

    private fun showCompletionNotification() {
        val isEnglish = GlobalLanguage.isEnglish
        val title = if (isEnglish) "Alert upload successful" else "অ্যালার্ট আপলোড সফল হয়েছে"
        val body = if (isEnglish) "Await admin approval." else "এডমিন অ্যাপ্রুভালের জন্য অপেক্ষা করুন।"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java).notify(SUCCESS_NOTIFICATION_ID, notification)
    }

    private val SUCCESS_NOTIFICATION_ID = 1004

    private fun handleError(msg: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Upload Failed")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notif)
    }
}
