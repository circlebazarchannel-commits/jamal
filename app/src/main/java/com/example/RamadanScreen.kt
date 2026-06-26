package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ViewState
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.toBengali
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamadanScreen(
    state: ViewState,
    onBack: () -> Unit
) {
    val isEnglish = GlobalLanguage.isEnglish
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEnglish) "Ramadan" else "রমজান", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B),
                    navigationIconContentColor = Color(0xFF1E293B)
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Countdown/Status Card
            RamadanFeatureCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isEnglish) "Upcoming" else "আসন্ন সময়",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.specialCountdownLabel,
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        text = state.nextPrayerRemaining,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Timings
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    RamadanTimingCard(
                        title = if (isEnglish) "Sehri Ends" else "সাহরির শেষ সময়",
                        time = state.prayerTimes?.fajr?.toBengali() ?: "--:--",
                        icon = Icons.Outlined.WbTwilight,
                        color = Color(0xFF3B82F6),
                        timeColor = PrimaryGreen
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    RamadanTimingCard(
                        title = if (isEnglish) "Iftar Starts" else "ইফতার শুরু",
                        time = state.prayerTimes?.maghrib?.toBengali() ?: "--:--",
                        icon = Icons.Outlined.RestaurantMenu,
                        color = Color(0xFFF59E0B),
                        timeColor = PrimaryGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tarabi Timing
            RamadanFeatureCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ModeNight, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isEnglish) "Taraweeh Time" else "তারাবীহ সময়",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                        Text(
                            text = state.prayerTimes?.isha?.toBengali() ?: "--:--",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duas
            Text(
                text = if (isEnglish) "Ramadan Duas" else "রমজানের দোয়া",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1E293B),
                modifier = Modifier.align(Alignment.Start).padding(vertical = 8.dp)
            )
            
            RamadanDuaCard(
                title = if (isEnglish) "Taraweeh Dua" else "তারাবীর পর দোয়া",
                arabic = "سُبْحَانَ ذِي الْمُلْكِ وَالْمَلَكُوتِ، سُبْحَانَ ذِي الْعِزَّةِ وَالْعَظَمَةِ وَالْهَيْبَةِ وَالْقُدْرَةِ وَالْكِبْرِيَاءِ وَالْجَبَرُوتِ. سُبْحَانَ الْمَلِكِ الْحَيِّ الَّذِي لَا يَنَامُ وَلَا يَمُوتُ. أَبَدًا أَبَدًا سُبُّوحٌ قُدُّوسٌ رَبُّنَا وَرَبُّ الْمَلَائِكَةِ وَالرُّوحِ",
                translation = if (isEnglish) "Glory be to the Master of earthly and heavenly domain. Glory be to the Possessor of might, greatness, reverence, power, pride, and majesty..." else "পবিত্রতা ঘোষণা করছি সেই সত্তার যিনি রাজ্য ও রাজত্বের মালিক..."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            RamadanDuaCard(
                title = if (isEnglish) "Dua for Fasting (Niyyah)" else "রোজার নিয়ত",
                arabic = "نَوَيْتُ اَنْ اُصُوْمَ غَدًا مِّنْ شَهْرِ رَمَضَانَ الْمُبَارَكِ فَرْضَا لَكَ يَا اللهُ فَتَقَبَّل مِنِّى اِنَّكَ اَنْتَ السَّمِيْعُ الْعَلِيْم",
                translation = if (isEnglish) "I intend to keep the fast tomorrow for the month of Ramadan, O Allah! So accept it from me; You are the All-Hearing, All-Knowing." else "হে আল্লাহ! আমি আগামীকাল পবিত্র রমজানের ফরজ রোজা রাখার নিয়ত করছি, অতএব তুমি তা কবুল কর, নিশ্চয়ই তুমি সর্বশ্রোতা ও সর্বজ্ঞ।"
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            RamadanDuaCard(
                title = if (isEnglish) "Dua for Iftar" else "ইফতারের দোয়া",
                arabic = "اللَّهُمَّ لَكَ صُمْتُ وَبِكَ آمَنْتُ وَعَلَيْكَ تَوَكَّلْتُ وَعَلَى رِزْقِكَ أَفْطَرْتُ",
                translation = if (isEnglish) "O Allah, I fasted for You and I believe in You and I put my trust in You and I break my fast with Your sustenance." else "হে আল্লাহ! আপনার জন্যই রোজা রেখেছি, আপনার ওপর ঈমান এনেছি, আপনার ওপর ভরসা করেছি এবং আপনার দেওয়া রিজিক দিয়েই ইফতার করছি।"
            )
        }
    }
}

@Composable
fun RamadanFeatureCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
fun RamadanTimingCard(title: String, time: String, icon: ImageVector, color: Color, timeColor: Color = TextDark) {
    RamadanFeatureCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                color = timeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun RamadanDuaCard(title: String, arabic: String, translation: String) {
    RamadanFeatureCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                color = TextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = arabic,
                color = Color(0xFF1E293B),
                fontSize = 22.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = translation,
                color = Color(0xFF64748B),
                fontSize = 14.sp
            )
        }
    }
}
