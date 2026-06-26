package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LocalAppStrings
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.viewmodel.AppLanguage
import com.example.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    prayerAlarms: Map<String, Boolean>,
    onTogglePrayerAlarm: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentLanguage by viewModel.language.collectAsState()
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    
    val selectedAdhan by viewModel.selectedAdhan.collectAsState()
    val customAdhanName by viewModel.customAdhanName.collectAsState()
    val isPlayingPreview by viewModel.isPlayingPreview.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPreview()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectCustomAdhan(context, it) }
    }

    val isEn = currentLanguage == AppLanguage.ENGLISH
    
    // Local Advanced Settings State (Mock for UI)
    var autoLocation by remember { mutableStateOf(true) }
    var timeFormat24h by remember { mutableStateOf(false) }
    var hadithNotif by remember { mutableStateOf(true) }
    var highContrast by remember { mutableStateOf(false) }
    
    // UI Expand States
    var showLanguageOptions by remember { mutableStateOf(false) }
    var showAdhanOptions by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = strings.back,
                        tint = TextDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings.app_settings,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // General Settings Section
            Text(
                text = if (isEn) "General Settings" else "সাধারণ সেটিংস",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextDark,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            // App Language
            SettingsItem(
                icon = Icons.Default.Settings,
                title = strings.select_language,
                subtitle = if (isEn) "English" else "বাংলা",
                onClick = { showLanguageOptions = !showLanguageOptions },
                trailingContent = {
                    Icon(
                        imageVector = if (showLanguageOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextGray
                    )
                }
            )
            AnimatedVisibility(visible = showLanguageOptions) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    LanguageOption(
                        language = AppLanguage.BENGALI,
                        isSelected = currentLanguage == AppLanguage.BENGALI,
                        onClick = { 
                            viewModel.setLanguage(AppLanguage.BENGALI)
                            showLanguageOptions = false
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LanguageOption(
                        language = AppLanguage.ENGLISH,
                        isSelected = currentLanguage == AppLanguage.ENGLISH,
                        onClick = { 
                            viewModel.setLanguage(AppLanguage.ENGLISH)
                            showLanguageOptions = false
                        }
                    )
                }
            }

            // Adhan Sound
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = if (isEn) "Adhan Sound" else "আযান সাউন্ড",
                subtitle = when (selectedAdhan) {
                    "mecca" -> if (isEn) "Mecca Adhan" else "মক্কার মোয়াজ্জিন আযান"
                    "medina" -> if (isEn) "Medina Adhan" else "মদীনার মোয়াজ্জিন আযান"
                    "custom" -> if (isEn) "Custom Sound" else "কাস্টম সাউন্ড"
                    else -> if (isEn) "System Tone" else "সিস্টেম টোন"
                },
                onClick = { showAdhanOptions = !showAdhanOptions },
                trailingContent = {
                    Icon(
                        imageVector = if (showAdhanOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextGray
                    )
                }
            )
            AnimatedVisibility(visible = showAdhanOptions) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    val adhanOptions = listOf(
                        Triple("pleasant", if (isEn) "System Tone" else "সিস্টেম টোন", "pleasant"),
                        Triple("mecca", if (isEn) "Mecca Adhan" else "মক্কার মোয়াজ্জিন আযান", "mecca"),
                        Triple("medina", if (isEn) "Medina Adhan" else "মদীনার মোয়াজ্জিন আযান", "medina"),
                        Triple("custom", if (isEn) "Custom (Upload MP3)" else "কাস্টম (এমপি৩ ফাইল আপলোড)", "custom")
                    )
                    
                    adhanOptions.forEach { (key, label, _) ->
                        val isSelected = selectedAdhan == key
                        val isPlaying = isPlayingPreview == key
                        
                        Surface(
                            onClick = { viewModel.setSelectedAdhan(context, key) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PrimaryGreen.copy(alpha = 0.08f) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) PrimaryGreen else Color.LightGray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.setSelectedAdhan(context, key) },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.togglePlayPreview(context, key) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = if (isPlaying) PrimaryGreen else TextGray
                                    )
                                }
                            }
                        }
                        if (key == "custom" && isSelected) {
                            Button(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = if (customAdhanName.isNotEmpty()) customAdhanName else (if (isEn) "Choose Audio File" else "অডিও ফাইল সিলেক্ট করুন"),
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(20.dp))

            // Advanced Settings Section
            Text(
                text = if (isEn) "Advanced Features" else "অ্যাডভান্সড ফিচারস",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextDark,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            SettingsSwitchItem(
                icon = Icons.Default.Place,
                title = if (isEn) "Auto Location Update" else "অটো লোকেশন আপডেট",
                subtitle = if (isEn) "Sync prayer times dynamically" else "জিপিএস অনুযায়ী স্বয়ংক্রিয় আপডেট",
                checked = autoLocation,
                onCheckedChange = { autoLocation = it }
            )

            SettingsSwitchItem(
                icon = Icons.Default.DateRange,
                title = if (isEn) "24-Hour Time Format" else "২৪ ঘণ্টা সময় বিন্যাস",
                subtitle = if (isEn) "Show time in 24h format" else "সময় ২৪ ঘণ্টা ফরমেটে দেখান",
                checked = timeFormat24h,
                onCheckedChange = { timeFormat24h = it }
            )

            SettingsSwitchItem(
                icon = Icons.Default.MenuBook,
                title = if (isEn) "Daily Hadith Notification" else "দৈনিক হাদিস নোটিফিকেশন",
                subtitle = if (isEn) "Receive one hadith every day" else "প্রতিদিন একটি হাদিস নোটিফিকেশন পান",
                checked = hadithNotif,
                onCheckedChange = { hadithNotif = it }
            )
            
            SettingsSwitchItem(
                icon = Icons.Default.Build, // Using Build as placeholder for UI/Contrast settings
                title = if (isEn) "High Contrast Mode" else "হাই কন্ট্রাস্ট মোড",
                subtitle = if (isEn) "Improve readability" else "লেখা আরও স্পষ্ট দেখানোর জন্য",
                checked = highContrast,
                onCheckedChange = { highContrast = it }
            )

            Spacer(modifier = Modifier.height(30.dp))
            
            Text(
                text = "${strings.version} ১.০.১",
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextDark)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = TextGray)
                }
            }
            trailingContent()
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextDark)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = TextGray)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryGreen,
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                ),
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}

@Composable
fun LanguageOption(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.08f) else Color.White,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language.label,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 15.sp,
                color = if (isSelected) PrimaryGreen else TextDark
            )
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
            }
        }
    }
}

