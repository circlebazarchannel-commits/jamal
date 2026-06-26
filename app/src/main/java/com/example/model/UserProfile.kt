package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val queue: Int = 0,
    val data1: String? = null,
    val data2: String? = null,
    val data3: String? = null,
    val data4: String? = null,
    val data5: String? = null,
    val data6: String? = null,
    val data7: String? = null,
    val data8: String? = null,
    val data9: String? = null,
    val data10: String? = null
)
