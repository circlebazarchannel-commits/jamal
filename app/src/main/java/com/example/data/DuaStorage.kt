package com.example.data

import android.content.Context
import android.content.SharedPreferences

class DuaStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("saved_duas", Context.MODE_PRIVATE)
    private val KEY_SAVED_DUAS = "saved_duas_set"

    fun getSavedDuaIds(): Set<String> {
        return prefs.getStringSet(KEY_SAVED_DUAS, emptySet()) ?: emptySet()
    }

    fun isDuaSaved(id: Int): Boolean {
        return getSavedDuaIds().contains(id.toString())
    }

    fun toggleSavedDua(id: Int) {
        val currentSaved = getSavedDuaIds().toMutableSet()
        if (currentSaved.contains(id.toString())) {
            currentSaved.remove(id.toString())
        } else {
            currentSaved.add(id.toString())
        }
        prefs.edit().putStringSet(KEY_SAVED_DUAS, currentSaved).apply()
    }
}
