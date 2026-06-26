package com.example.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

enum class AppLanguage(val code: String, val label: String) {
    BENGALI("bn", "বাংলা"),
    ENGLISH("en", "English")
}

class SettingsViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val alarmPrefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        AppLanguage.values().find { it.code == sharedPrefs.getString("language", "bn") } ?: AppLanguage.BENGALI
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _selectedCountryCode = MutableStateFlow(
        sharedPrefs.getString("selected_country_code", "") ?: ""
    )
    val selectedCountryCode: StateFlow<String> = _selectedCountryCode.asStateFlow()

    private val _selectedAdhan = MutableStateFlow(
        alarmPrefs.getString("pref_selected_adhan", "medina") ?: "medina"
    )
    val selectedAdhan: StateFlow<String> = _selectedAdhan.asStateFlow()

    private val _customAdhanName = MutableStateFlow(
        alarmPrefs.getString("pref_custom_adhan_name", "") ?: ""
    )
    val customAdhanName: StateFlow<String> = _customAdhanName.asStateFlow()

    private val _customLogoPath = MutableStateFlow(
        sharedPrefs.getString("pref_custom_logo_path", "") ?: ""
    )
    val customLogoPath: StateFlow<String> = _customLogoPath.asStateFlow()

    private var previewPlayer: MediaPlayer? = null
    private val _isPlayingPreview = MutableStateFlow<String?>(null)
    val isPlayingPreview: StateFlow<String?> = _isPlayingPreview.asStateFlow()

    init {
        val savedCountryCode = sharedPrefs.getString("selected_country_code", null)
        if (savedCountryCode == null) {
            // No country saved yet - first launch detection
            val systemLocale = try {
                appContext.resources.configuration.locales.get(0)
            } catch (e: Exception) {
                java.util.Locale.getDefault()
            } ?: java.util.Locale.getDefault()
            val systemCountry = systemLocale.country ?: ""
            val systemLanguage = systemLocale.language ?: ""
            
            // Logically detect country and language
            val matchedCountry = com.example.model.CountryData.countries.find { 
                it.code.equals(systemCountry, ignoreCase = true) 
            }
            
            val detectedCountryCode = matchedCountry?.code ?: "US"
            val detectedLanguage = if (systemLanguage == "bn" || detectedCountryCode == "BD") {
                AppLanguage.BENGALI
            } else {
                AppLanguage.ENGLISH
            }
            
            // Save detected settings
            sharedPrefs.edit()
                .putString("selected_country_code", detectedCountryCode)
                .putString("language", detectedLanguage.code)
                .apply()
                
            _selectedCountryCode.update { detectedCountryCode }
            _language.update { detectedLanguage }
            GlobalLanguage.isEnglish = detectedLanguage == AppLanguage.ENGLISH
        } else {
            _selectedCountryCode.update { savedCountryCode }
            GlobalLanguage.isEnglish = _language.value == AppLanguage.ENGLISH
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.update { lang }
        GlobalLanguage.isEnglish = lang == AppLanguage.ENGLISH
        val correspondingCountryCode = if (lang == AppLanguage.BENGALI) "BD" else "US"
        _selectedCountryCode.update { correspondingCountryCode }
        sharedPrefs.edit()
            .putString("language", lang.code)
            .putString("selected_country_code", correspondingCountryCode)
            .apply()
        com.example.widget.WidgetUtils.updateAllWidgets(appContext)
    }

    fun setSelectedCountryAndLanguage(countryCode: String) {
        val matchedCountry = com.example.model.CountryData.countries.find { it.code == countryCode }
        val lang = if (countryCode == "BD") {
            AppLanguage.BENGALI
        } else {
            AppLanguage.ENGLISH
        }
        
        _selectedCountryCode.update { countryCode }
        _language.update { lang }
        GlobalLanguage.isEnglish = lang == AppLanguage.ENGLISH
        
        sharedPrefs.edit()
            .putString("selected_country_code", countryCode)
            .putString("language", lang.code)
            .apply()
            
        com.example.widget.WidgetUtils.updateAllWidgets(appContext)
    }

    fun setSelectedAdhan(context: Context, type: String) {
        _selectedAdhan.update { type }
        alarmPrefs.edit().putString("pref_selected_adhan", type).apply()
    }

    fun selectCustomAdhan(context: Context, uri: Uri) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "custom_adhan.mp3"
            
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val foundName = cursor.getString(nameIndex)
                    if (!foundName.isNullOrEmpty()) {
                        fileName = foundName
                    }
                }
            }

            val destinationFile = File(context.filesDir, "custom_adhan.mp3")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            alarmPrefs.edit().apply {
                putString("pref_selected_adhan", "custom")
                putString("pref_custom_adhan_name", fileName)
                apply()
            }

            _selectedAdhan.update { "custom" }
            _customAdhanName.update { fileName }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectCustomLogo(context: Context, uri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val destinationFile = File(context.filesDir, "custom_logo.png")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            sharedPrefs.edit().putString("pref_custom_logo_path", destinationFile.absolutePath).apply()
            _customLogoPath.update { destinationFile.absolutePath }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPreview(context: Context, type: String) {
        if (_isPlayingPreview.value == type) {
            stopPreview()
        } else {
            startPreview(context, type)
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isPlayingPreview.update { null }
    }

    private fun startPreview(context: Context, type: String) {
        stopPreview()
        try {
            previewPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setOnCompletionListener {
                    _isPlayingPreview.update { null }
                }
            }

            var sourceSet = false
            when (type) {
                "mecca" -> {
                    previewPlayer?.setDataSource("https://download.quranicaudio.com/adhan/makkah.mp3")
                    sourceSet = true
                }
                "medina" -> {
                    previewPlayer?.setDataSource("https://download.quranicaudio.com/adhan/madinah.mp3")
                    sourceSet = true
                }
                "custom" -> {
                    val file = File(context.filesDir, "custom_adhan.mp3")
                    if (file.exists()) {
                        previewPlayer?.setDataSource(file.absolutePath)
                        sourceSet = true
                    }
                }
                "pleasant" -> {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    if (uri != null) {
                        previewPlayer?.setDataSource(context, uri)
                        sourceSet = true
                    }
                }
            }

            if (sourceSet) {
                _isPlayingPreview.update { type }
                previewPlayer?.prepareAsync()
                previewPlayer?.setOnPreparedListener {
                    it.start()
                }
                previewPlayer?.setOnErrorListener { _, _, _ ->
                    _isPlayingPreview.update { null }
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlayingPreview.update { null }
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewPlayer?.release()
    }
}
