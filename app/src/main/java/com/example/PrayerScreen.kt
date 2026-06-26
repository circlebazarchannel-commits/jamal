package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ViewState
import com.example.viewmodel.GlobalLanguage

data class PrayerQuad(val id: String, val name: String, val startTime: String, val endTime: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    state: ViewState,
    onBack: () -> Unit,
    onToggleAlarm: (String) -> Unit,
    onOpenAlarmPage: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgLight,
        topBar = {
            TopAppBar(
                title = { Text(if (GlobalLanguage.isEnglish) "Prayer schedule" else "সালাতের সময়সূচী", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAlarmPage) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Alarms", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            
            // Header Section
            item {
                PrayerHeaderCard(state)
            }
            
            // Islamic Info Section (Forbidden times)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (GlobalLanguage.isEnglish) "Daily Schedule" else "প্রতিদিনের নামাজ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                    Text(
                        text = state.currentDate,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PrimaryGreen
                    )
                }
            }
            
            // Prayer List
            val times = state.prayerTimes
            if (times != null) {
                val prayers = listOf(
                    PrayerQuad("Fajr", if (GlobalLanguage.isEnglish) "Fajr" else "ফজর", times.fajr, times.sunrise, Icons.Outlined.WbTwilight),
                    PrayerQuad("Dhuhr", if (GlobalLanguage.isEnglish) "Dhuhr" else "যোহর", times.dhuhr, times.asr, Icons.Outlined.WbSunny),
                    PrayerQuad("Asr", if (GlobalLanguage.isEnglish) "Asr" else "আসর", times.asr, times.maghrib, Icons.Outlined.WbSunny),
                    PrayerQuad("Maghrib", if (GlobalLanguage.isEnglish) "Maghrib" else "মাগরিব", times.maghrib, times.isha, Icons.Outlined.WbTwilight),
                    PrayerQuad("Isha", if (GlobalLanguage.isEnglish) "Isha" else "এশা", times.isha, times.fajr, Icons.Outlined.NightsStay)
                )

                itemsIndexed(prayers) { _, p ->
                    val isActive = p.id == state.currentPrayerName
                    val isAlarmOn = state.alarms[p.id] == true
                    UnifiedPrayerCard(
                        name = p.name,
                        startTime = p.startTime,
                        endTime = p.endTime,
                        icon = p.icon,
                        isActive = isActive,
                        isAlarmOn = isAlarmOn,
                        onToggleAlarm = { onToggleAlarm(p.id) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ForbiddenTimesCard(state)
                }
            }
        }
    }
}

@Composable
fun PrayerHeaderCard(state: ViewState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(PrimaryGreen, Color(0xFF064E3B)))) // Darker green gradient
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = Color.White.copy(alpha=0.8f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    state.locationName,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                state.nextPrayerRemaining,
                color = Color.White,
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                if (GlobalLanguage.isEnglish) "Remaining for next prayer" else "পরবর্তী ওয়াক্তের বাকি সময়",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        if (GlobalLanguage.isEnglish) "Current" else "বর্তমান ওয়াক্ত",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Text(
                        state.currentPrayerNameBen.ifEmpty { "--" },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                ContainerDivider()
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (GlobalLanguage.isEnglish) "Next" else "পরবর্তী ওয়াক্ত",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Text(
                        state.nextPrayerNameBen.ifEmpty { "--" },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ContainerDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.3f))
    )
}

@Composable
fun UnifiedPrayerCard(
    name: String,
    startTime: String,
    endTime: String,
    icon: ImageVector,
    isActive: Boolean,
    isAlarmOn: Boolean,
    onToggleAlarm: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) PrimaryGreen else Color.White,
        label = "bgColor"
    )
    val contentColor = if (isActive) Color.White else TextDark
    val subContentColor = if (isActive) Color.White.copy(alpha = 0.8f) else PrimaryGreen

    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 8.dp else 1.dp),
        border = if (!isActive) BorderStroke(1.dp, Color(0xFFF3F4F6)) else null
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
                        .size(46.dp)
                        .background(if (isActive) Color.White.copy(alpha = 0.2f) else PrimaryGreen.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = if (isActive) Color.White else PrimaryGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = name,
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$startTime - $endTime",
                        color = subContentColor,
                        fontSize = 14.sp
                    )
                }
            }

            IconButton(
                onClick = onToggleAlarm,
                modifier = Modifier
                    .background(
                        if (isAlarmOn) (if (isActive) Color.White.copy(alpha = 0.2f) else PrimaryGreen.copy(alpha = 0.1f)) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isAlarmOn) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                    contentDescription = if (isAlarmOn) "Alarm On" else "Alarm Off",
                    tint = if (isActive) Color.White else (if (isAlarmOn) PrimaryGreen else TextGray)
                )
            }
        }
    }
}

@Composable
fun ForbiddenTimesCard(state: ViewState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
        border = BorderStroke(1.dp, Color(0xFFFECDD3))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (GlobalLanguage.isEnglish) "Forbidden Times for Prayer" else "নামাজের নিষিদ্ধ সময়",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFE11D48)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (GlobalLanguage.isEnglish) "Sunrise:" else "সূর্যোদয়:", fontSize=13.sp, color=Color.Black)
                Text("${state.forbiddenSunrise} - ${state.forbiddenSunriseEnd}", fontSize=13.sp, fontWeight = FontWeight.Bold, color=Color.Black)
            }
            Spacer(modifier=Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (GlobalLanguage.isEnglish) "Zenith (Noon):" else "দ্বিপ্রহর:", fontSize=13.sp, color=Color.Black)
                Text("${state.forbiddenNoon} - ${state.forbiddenNoonEnd}", fontSize=13.sp, fontWeight = FontWeight.Bold, color=Color.Black)
            }
            Spacer(modifier=Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (GlobalLanguage.isEnglish) "Sunset:" else "সূর্যাস্ত:", fontSize=13.sp, color=Color.Black)
                Text("${state.forbiddenSunset} - ${state.forbiddenSunsetEnd}", fontSize=13.sp, fontWeight = FontWeight.Bold, color=Color.Black)
            }
        }
    }
}
