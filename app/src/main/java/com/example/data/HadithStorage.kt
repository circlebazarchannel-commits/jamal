package com.example.data

import android.content.Context
import android.content.SharedPreferences

class HadithStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("saved_hadiths", Context.MODE_PRIVATE)
    private val KEY_SAVED_HADITHS = "saved_hadiths_set"

    fun getSavedHadithIds(): Set<String> {
        return prefs.getStringSet(KEY_SAVED_HADITHS, emptySet()) ?: emptySet()
    }

    fun isHadithSaved(id: Int): Boolean {
        return getSavedHadithIds().contains(id.toString())
    }

    fun toggleSavedHadith(id: Int) {
        val currentSaved = getSavedHadithIds().toMutableSet()
        if (currentSaved.contains(id.toString())) {
            currentSaved.remove(id.toString())
        } else {
            currentSaved.add(id.toString())
        }
        prefs.edit().putStringSet(KEY_SAVED_HADITHS, currentSaved).apply()
    }
}
