package com.example.model

data class Hadith(
    val id: Int,
    val category: String,
    val title: String,
    val arabic: String,
    val pronunciation: String,
    val translation: String,
    val reference: String
)
