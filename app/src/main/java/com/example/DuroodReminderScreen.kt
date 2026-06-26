package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiver.DuroodHelper
import com.example.ui.theme.BgLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.viewmodel.GlobalLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuroodReminderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isEnglish = GlobalLanguage.isEnglish

    // Local state variables for configuration
    var isEnabled by remember { mutableStateOf(DuroodHelper.isEnabled(context)) }
    var selectedIntervalMins by remember { mutableStateOf(DuroodHelper.getIntervalMins(context)) }
    var isVoiceEnabled by remember { mutableStateOf(DuroodHelper.isVoiceEnabled(context)) }
    var selectedText by remember { mutableStateOf(DuroodHelper.getSelectedText(context)) }
    
    var isBusyEnabled by remember { mutableStateOf(DuroodHelper.isBusyEnabled(context)) }
    var busyStartMins by remember { mutableStateOf(DuroodHelper.getBusyStartMins(context)) }
    var busyEndMins by remember { mutableStateOf(DuroodHelper.getBusyEndMins(context)) }
    var customVoiceUri by remember { mutableStateOf(DuroodHelper.getCustomVoiceUri(context)) }

    val audioPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream: java.io.InputStream? = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "custom_durood_voice.m4a")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                outputStream.close()
                inputStream?.close()
                customVoiceUri = android.net.Uri.fromFile(file).toString()
                android.widget.Toast.makeText(context, if (isEnglish) "Voice selected" else "ভয়েস নির্বাচন করা হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showTimePicker(initialMins: Int, onTimeSelected: (Int) -> Unit) {
        val hour = initialMins / 60
        val minute = initialMins % 60
        android.app.TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            onTimeSelected(selectedHour * 60 + selectedMinute)
        }, hour, minute, false).show()
    }

    fun formatTime(mins: Int): String {
        val h = mins / 60
        val m = mins % 60
        val amPm = if (h >= 12) "PM" else "AM"
        val hour12 = if (h % 12 == 0) 12 else h % 12
        return String.format(java.util.Locale.US, "%02d:%02d %s", hour12, m, amPm)
    }

    val intervalOptions = listOf(
        Triple(15, if (isEnglish) "15 Mins" else "১৫ মিনিট", Icons.Default.Schedule),
        Triple(30, if (isEnglish) "30 Mins" else "৩০ মিনিট", Icons.Default.Schedule),
        Triple(60, if (isEnglish) "1 Hour" else "১ ঘণ্টা", Icons.Default.HourglassTop),
        Triple(120, if (isEnglish) "2 Hours" else "২ ঘণ্টা", Icons.Default.HourglassBottom),
        Triple(300, if (isEnglish) "5 Hours" else "৫ ঘণ্টা", Icons.Default.Update)
    )

    val duroodTexts = listOf(
        Pair("ﷺ", if (isEnglish) "Shortest Sallallahu Alayhi Wa Sallam symbol" else "সংক্ষিপ্ত সাঃ প্রতীক"),
        Pair("সাল্লাল্লাহু আলাইহি ওয়াসাল্লাম", if (isEnglish) "Full Sallallahu Alayhi Wa Sallam" else "পূর্ণ সাল্লাল্লাহু আলাইহি ওয়াসাল্লাম"),
        Pair("আল্লাহুম্মা সাল্লি আলা মুহাম্মাদ", if (isEnglish) "Allahumma Salli Ala Muhammad" else "আল্লাহুম্মা সাল্লি আলা মুহাম্মাদ (সংক্ষিপ্ত)"),
        Pair("আল্লাহুম্মা সাল্লি ওয়া সাল্লিম আলা নাবিয়্যিনা মুহাম্মাদ", if (isEnglish) "Allahumma Salli Wa Sallim Ala Nabiyyina Muhammad" else "নবীজির প্রতি রহমত ও শান্তির দোয়া")
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextDark
                    )
                }
                Text(
                    text = if (isEnglish) "Durood Reminder" else "দরুদ রিমাইন্ডার",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        containerColor = BgLight,
        bottomBar = {
            if (isEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            DuroodHelper.saveConfig(
                                context,
                                isEnabled,
                                selectedIntervalMins,
                                isVoiceEnabled,
                                selectedText,
                                isBusyEnabled,
                                busyStartMins,
                                busyEndMins,
                                customVoiceUri
                            )
                            android.widget.Toast.makeText(context, if (isEnglish) "Settings Saved" else "সেটিংস সেভ হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Save Configuration" else "সেভ করুন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = if (isEnabled) 70.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Reminder Enable/Disable toggle card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryGreen.copy(alpha = 0.10f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Durood Reminder" else "দরুদ রিমাইন্ডার চালু করুন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                            Text(
                                text = if (isEnabled) {
                                    if (isEnglish) "Reminder is Active" else "রিমাইন্ডার এখন সচল আছে"
                                } else {
                                    if (isEnglish) "Reminder is Off" else "রিমাইন্ডার এখন বন্ধ আছে"
                                },
                                fontSize = 12.sp,
                                color = if (isEnabled) PrimaryGreen else TextGray
                            )
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            isEnabled = checked
                            if (!checked) {
                                // Direct save if turning off
                                DuroodHelper.saveConfig(
                                    context = context,
                                    enabled = false,
                                    interval = selectedIntervalMins,
                                    voiceEnabled = isVoiceEnabled,
                                    text = selectedText,
                                    busyEnabled = isBusyEnabled,
                                    busyStartMins = busyStartMins,
                                    busyEndMins = busyEndMins,
                                    customVoiceUri = customVoiceUri
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryGreen,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = BgLight
                        )
                    )
                }
            }

            // Options container (enabled states)
            if (isEnabled) {
                // Select Interval Option Section
                Text(
                    text = if (isEnglish) "Select Interval" else "রিমাইন্ডার ইন্টারভাল নির্বাচন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        intervalOptions.forEach { option ->
                            val isSelected = selectedIntervalMins == option.first
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryGreen.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { selectedIntervalMins = option.first }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = option.third,
                                        contentDescription = null,
                                        tint = if (isSelected) PrimaryGreen else TextGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = option.second,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryGreen else TextDark
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice Reminder Switch
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PrimaryGreen.copy(alpha = 0.10f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isVoiceEnabled) Icons.Default.RecordVoiceOver else Icons.Default.VolumeMute,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Voice Reminder" else "ভয়েস রিমাইন্ডার",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = if (isEnglish) {
                                            "App speaks aloud"
                                        } else {
                                            "রিমাইন্ডারে শব্দ হবে"
                                        },
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            Switch(
                                checked = isVoiceEnabled,
                                onCheckedChange = { isVoiceEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryGreen,
                                    uncheckedThumbColor = TextGray,
                                    uncheckedTrackColor = BgLight
                                )
                            )
                        }

                        if (isVoiceEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BgLight)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { audioPicker.launch("audio/*") },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (isEnglish) "Select Custom Audio" else "কাস্টম অডিও নির্বাচন করুন",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PrimaryGreen
                                    )
                                    Text(
                                        text = if (customVoiceUri != null) {
                                            if (isEnglish) "Custom audio loaded" else "কাস্টম অডিও সেট করা আছে"
                                        } else {
                                            if (isEnglish) "Using default voice" else "ডিফল্ট ভয়েস চলছে"
                                        },
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                }
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload", tint = PrimaryGreen)
                            }
                        }
                    }
                }

                // Busy Times Section
                Text(
                    text = if (isEnglish) "Quiet Hours (Busy Time)" else "নীরব সময় (ব্যস্ত সময়)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isEnglish) "Enable Quiet Hours" else "নীরব সময় চালু করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = if (isEnglish) "Mute reminder during this time" else "এই সময়ে রিমাইন্ডার আসবে না",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                            Switch(
                                checked = isBusyEnabled,
                                onCheckedChange = { isBusyEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryGreen,
                                    uncheckedThumbColor = TextGray,
                                    uncheckedTrackColor = BgLight
                                )
                            )
                        }

                        if (isBusyEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BgLight)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Start Time
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showTimePicker(busyStartMins) { busyStartMins = it } }
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Start Time" else "শুরুর সময়",
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                    Text(
                                        text = formatTime(busyStartMins),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }
                                // End Time
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showTimePicker(busyEndMins) { busyEndMins = it } }
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = if (isEnglish) "End Time" else "শেষের সময়",
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                    Text(
                                        text = formatTime(busyEndMins),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Extra padding for bottom bar
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
