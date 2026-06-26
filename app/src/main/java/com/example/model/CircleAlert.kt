package com.example.model

data class CircleAlert(
    val docId: String = "",
    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val mediaUri: String = "",
    val mediaType: String = "photo", // "photo" or "video"
    val contactNumber: String = "",
    val country: String = "Bangladesh",
    val location: String = "All Bangladesh",
    val timestamp: Long = 0L,
    val status: String = "PENDING", 
    val telegramFileId: String = ""
)
