package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.ui.theme.BgLight
import com.example.ui.theme.CardBg
import com.example.viewmodel.GlobalLanguage
import java.util.Calendar

object HijriCalendarHelper {
    data class HijriDate(val day: Int, val month: Int, val year: Int)

    // Hijri Month Names in Bangla
    val bnHijriMonths = listOf(
        "মহররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
        "জুমাদাল উলা", "জুমাদাল আখিরাহ", "রজব", "শাবান",
        "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ্জ"
    )

    // Hijri Month Names in English
    val enHijriMonths = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' ath-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    // Gregorian Month Names in Bangla
    val bnGregorianMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    val enGregorianMonths = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val bnWeekDays = listOf(
        "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহস্পতি", "শুক্র", "শনি"
    )

    val enWeekDays = listOf(
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    )

    val bnFullWeekDays = listOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    val enFullWeekDays = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    // Converted to Bengali Digits
    fun toBengaliNumber(number: Int): String {
        return number.toString().map {
            when (it) {
                '0' -> '০'
                '1' -> '১'
                '2' -> '২'
                '3' -> '৩'
                '4' -> '৪'
                '5' -> '৫'
                '6' -> '৬'
                '7' -> '৭'
                '8' -> '৮'
                '9' -> '৯'
                else -> it
            }
        }.joinToString("")
    }

    fun String.toBengaliNum(): String {
        return this.map {
            when (it) {
                '0' -> '০'
                '1' -> '১'
                '2' -> '২'
                '3' -> '৩'
                '4' -> '৪'
                '5' -> '৫'
                '6' -> '৬'
                '7' -> '৭'
                '8' -> '৮'
                '9' -> '৯'
                else -> it
            }
        }.joinToString("")
    }

    // Algorithmic Gregorian to Hijri converter based on Tabular Islamic Chronology
    fun gregorianToHijri(year: Int, month: Int, day: Int): HijriDate {
        var y = year
        var m = month
        val d = day
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = (y / 100).toInt()
        val b = 2 - a + (a / 4).toInt()

        val jd = (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + d + b - 1524.5
        
        val epochJd = 1948439.5
        val days = jd - epochJd

        val cycles = (days / 10631).toInt()
        var remain = (days % 10631).toInt()
        if (remain < 0) {
            remain += 10631
        }

        val leapYears = intArrayOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        var hYear = cycles * 30 + 1
        var yearInCycle = 1

        for (i in 1..30) {
            val isLeap = leapYears.contains(i)
            val daysInY = if (isLeap) 355 else 354
            if (remain < daysInY) {
                yearInCycle = i
                break
            }
            remain -= daysInY
        }
        hYear += yearInCycle - 1

        val isLeapYear = leapYears.contains(yearInCycle)
        val monthLengths = IntArray(12)
        for (i in 0..11) {
            monthLengths[i] = if ((i + 1) % 2 != 0) 30 else 29
        }
        if (isLeapYear) {
            monthLengths[11] = 30
        }

        var hMonth = 1
        for (i in 0..11) {
            if (remain < monthLengths[i]) {
                hMonth = i + 1
                break
            }
            remain -= monthLengths[i]
        }
        val hDay = remain + 1

        return HijriDate(hDay, hMonth, hYear)
    }

    // Distinct list of key Annual Islamic Days (Hijri Dates)
    data class IslamicEvent(
        val hMonth: Int,
        val hDay: Int,
        val nameEn: String,
        val nameBn: String,
        val descEn: String,
        val descBn: String
    )

    val events = listOf(
        IslamicEvent(1, 1, "Islamic New Year", "হিজরি নববর্ষ", "1st of Muharram", "হিজরি নববর্ষের প্রথম দিন"),
        IslamicEvent(1, 10, "Holy Ashura", "পবিত্র আশুরা", "10th of Muharram", "ঐতিহাসিক আশুরা দিবস"),
        IslamicEvent(3, 12, "Milad-un-Nabi", "ঈদে মিলাদুন্নবী (সা.)", "12th of Rabi' al-Awwal", "রাসুলুল্লাহ (সা.)-এর পবিত্র মিলাদ ও ওফাত"),
        IslamicEvent(7, 27, "Shab-e-Meraj", "পবিত্র শবে মেরাজ", "27th of Rajab", "অলৌকিক শবে মেরাজ রজনী"),
        IslamicEvent(8, 15, "Shab-e-Barat", "পবিত্র শবে বরাত", "15th of Sha'ban", "বরকতময় মুক্তির রজনী"),
        IslamicEvent(9, 1, "First Day of Ramadan", "পবিত্র রমজান শুরু", "1st of Ramadan", "সিয়াম সাধনার মাস শুরু"),
        IslamicEvent(9, 27, "Laylatul Qadr", "মহিমান্বিত লাইলাতুল কদর", "27th of Ramadan", "হজার মাসের চেয়েও শ্রেষ্ঠ রজনী"),
        IslamicEvent(10, 1, "Eid-ul-Fitr", "পবিত্র ঈদুল ফিতর", "1st of Shawwal", "সিয়াম সমাপনান্তে মুসলিমদের প্রধান আনন্দোৎসব"),
        IslamicEvent(12, 1, "Dhu al-Hijjah Commences", "জিলহজ্জ মাস শুরু", "1st of Dhu al-Hijjah", "হজ ও কোরবানির মহিমান্বিত ত্যাগের মাস শুরু"),
        IslamicEvent(12, 9, "Day of Arafah (Hajj)", "আরাফাহর দিন / পবিত্র হজ", "9th of Dhu al-Hijjah", "মহিমান্বিত হজের মূল দিন"),
        IslamicEvent(12, 10, "Eid-ul-Adha", "পবিত্র ঈদুল আজহা", "10th of Dhu al-Hijjah", "হজরত ইব্রাহিম (আ.)-এর ত্যাগের স্মরণে কোরবানি"),
        IslamicEvent(12, 11, "Days of Tashreeq", "আইয়ামে তাশরিক শুরু", "11th of Dhu al-Hijjah", "তাকবিরে তাশরিক উচ্চারণের সম্মানিত দিন")
    )

    fun getEventForDate(hMonth: Int, hDay: Int): IslamicEvent? {
        return events.firstOrNull { it.hMonth == hMonth && it.hDay == hDay }
    }

    // Scan selected Gregorian year to pinpoint the concrete English date corresponding to this Islamic occasion
    fun findGregorianForHijri(hMonth: Int, hDay: Int, gregorianYear: Int): Calendar? {
        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.YEAR, gregorianYear)
        tempCal.set(Calendar.MONTH, 0)
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        
        val totalDays = if (tempCal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365) 366 else 365
        for (dayOfYear in 1..totalDays) {
            tempCal.set(Calendar.DAY_OF_YEAR, dayOfYear)
            val gYear = tempCal.get(Calendar.YEAR)
            val gMonth = tempCal.get(Calendar.MONTH) + 1
            val gDay = tempCal.get(Calendar.DAY_OF_MONTH)
            val hDate = gregorianToHijri(gYear, gMonth, gDay)
            if (hDate.month == hMonth && hDate.day == hDay) {
                return tempCal
            }
        }
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val isEn = GlobalLanguage.isEnglish
    
    // Core calendar states
    val today = remember { Calendar.getInstance() }
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH) // 0-11
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    
    // Screen viewing state (defaults to current year/month)
    var currentYear by remember { mutableStateOf(todayYear) }
    var currentMonth by remember { mutableStateOf(todayMonth) }
    
    // Dynamic selected state (defaults to today)
    var selectedYear by remember { mutableStateOf(todayYear) }
    var selectedMonth by remember { mutableStateOf(todayMonth) }
    var selectedDay by remember { mutableStateOf(todayDay) }

    // Dynamic state calculations
    val currentSelectedCalendar = remember(selectedYear, selectedMonth, selectedDay) {
        Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay)
        }
    }

    val selectedHijri = remember(selectedYear, selectedMonth, selectedDay) {
        HijriCalendarHelper.gregorianToHijri(selectedYear, selectedMonth + 1, selectedDay)
    }

    val selectedEvent = remember(selectedHijri) {
        HijriCalendarHelper.getEventForDate(selectedHijri.month, selectedHijri.day)
    }

    // Tab Selection state: 0 = Monthly Grid, 1 = Islamic Occasions
    var activeTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = BgLight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEn) "Islamic & English Calendar" else "হিজরি ও ইংরেজি ক্যালেন্ডার", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryGreen
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(BgLight)
        ) {
            // Elegant Emerald Mesh Gradient Top Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PrimaryGreen, PrimaryGreen.copy(alpha = 0.05f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Interactive Hero Date Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.background(PrimaryGreen.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "DYNAMIC SELECTED DATE" else "নির্বাচিত তারিখের বিবরণ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Converted Full Weekday
                        val weekdayIdx = currentSelectedCalendar.get(Calendar.DAY_OF_WEEK) - 1
                        val weekdayStr = if (isEn) HijriCalendarHelper.enFullWeekDays[weekdayIdx] else HijriCalendarHelper.bnFullWeekDays[weekdayIdx]
                        
                        Text(
                            text = weekdayStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large beautiful Converted Hijri date
                        val hijriMonthName = if (isEn) HijriCalendarHelper.enHijriMonths[selectedHijri.month - 1] else HijriCalendarHelper.bnHijriMonths[selectedHijri.month - 1]
                        val hijriDayStr = if (isEn) selectedHijri.day.toString() else HijriCalendarHelper.toBengaliNumber(selectedHijri.day)
                        val hijriYearStr = if (isEn) selectedHijri.year.toString() else HijriCalendarHelper.toBengaliNumber(selectedHijri.year)

                        Text(
                            text = if (isEn) "$hijriDayStr $hijriMonthName, $hijriYearStr AH" else "$hijriDayStr-ই $hijriMonthName, $hijriYearStr হিজরি",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Supporting Gregorian date
                        val gregMonthName = if (isEn) HijriCalendarHelper.enGregorianMonths[selectedMonth] else HijriCalendarHelper.bnGregorianMonths[selectedMonth]
                        val gregDayStr = if (isEn) selectedDay.toString() else HijriCalendarHelper.toBengaliNumber(selectedDay)
                        val gregYearStr = if (isEn) selectedYear.toString() else HijriCalendarHelper.toBengaliNumber(selectedYear)

                        Text(
                            text = if (isEn) "$gregDayStr $gregMonthName $gregYearStr AD" else "$gregDayStr $gregMonthName $gregYearStr ইংরেজি",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark.copy(alpha = 0.8f)
                        )

                        // Highlight Special Event Row if visible
                        selectedEvent?.let { event ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = if (isEn) event.nameEn else event.nameBn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = if (isEn) event.descEn else event.descBn,
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Tab Segments
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(PrimaryGreen.copy(alpha = 0.05f), RoundedCornerShape(30.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Monthly Dynamic Grid View
                Button(
                    onClick = { activeTabIndex = 0 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTabIndex == 0) PrimaryGreen else Color.Transparent,
                        contentColor = if (activeTabIndex == 0) Color.White else TextDark.copy(alpha = 0.6f)
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    elevation = null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (activeTabIndex == 0) Color.White else TextDark.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Calendar Grid" else "ক্যালেন্ডার গ্রিড",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Tab 2: Islamic Event Directory List
                Button(
                    onClick = { activeTabIndex = 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTabIndex == 1) PrimaryGreen else Color.Transparent,
                        contentColor = if (activeTabIndex == 1) Color.White else TextDark.copy(alpha = 0.6f)
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    elevation = null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (activeTabIndex == 1) Color.White else TextDark.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Important Days" else "ইসলামিক দিবসসমূহ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Area based on Tab Selection
            AnimatedContent(
                targetState = activeTabIndex,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "calendar_body_view"
            ) { targetIndex ->
                if (targetIndex == 0) {
                    // ---- CALENDAR MONTHLY VIEW ----
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Header Navigation bar for Month/Year Toggle
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.15f)),
                            colors = CardDefaults.cardColors(containerColor = CardBg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentMonth == 0) {
                                            currentMonth = 11
                                            currentYear -= 1
                                        } else {
                                            currentMonth -= 1
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ChevronLeft, "Previous Month", tint = PrimaryGreen)
                                }

                                // Centered Month-Year Titles
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val topMonthStr = if (isEn) HijriCalendarHelper.enGregorianMonths[currentMonth] else HijriCalendarHelper.bnGregorianMonths[currentMonth]
                                    val topYearStr = if (isEn) currentYear.toString() else HijriCalendarHelper.toBengaliNumber(currentYear)
                                    
                                    Text(
                                        text = "$topMonthStr $topYearStr",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextDark
                                    )

                                    // Estimate current Hijri month span
                                    val currentMonthStartHijri = remember(currentYear, currentMonth) {
                                        HijriCalendarHelper.gregorianToHijri(currentYear, currentMonth + 1, 1)
                                    }
                                    val currentMonthEndHijri = remember(currentYear, currentMonth) {
                                        val c = Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }
                                        val maxD = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                                        HijriCalendarHelper.gregorianToHijri(currentYear, currentMonth + 1, maxD)
                                    }

                                    val startMonthName = if (isEn) HijriCalendarHelper.enHijriMonths[currentMonthStartHijri.month - 1] else HijriCalendarHelper.bnHijriMonths[currentMonthStartHijri.month - 1]
                                    val endMonthName = if (isEn) HijriCalendarHelper.enHijriMonths[currentMonthEndHijri.month - 1] else HijriCalendarHelper.bnHijriMonths[currentMonthEndHijri.month - 1]
                                    val startYearStr = if (isEn) currentMonthStartHijri.year.toString() else HijriCalendarHelper.toBengaliNumber(currentMonthStartHijri.year)
                                    
                                    val hijriHeaderStr = if (startMonthName == endMonthName) {
                                        "$startMonthName $startYearStr AH"
                                    } else {
                                        "$startMonthName - $endMonthName $startYearStr"
                                    }

                                    Text(
                                        text = hijriHeaderStr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PrimaryGreen
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (currentMonth == 11) {
                                            currentMonth = 0
                                            currentYear += 1
                                        } else {
                                            currentMonth += 1
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ChevronRight, "Next Month", tint = PrimaryGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Multi-Columns Days of week headers
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    val weekdays = if (isEn) HijriCalendarHelper.enWeekDays else HijriCalendarHelper.bnWeekDays
                                    weekdays.forEachIndexed { idx, day ->
                                        // Highlight Friday custom color index
                                        val isFriday = idx == 5
                                        Text(
                                            text = day,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isFriday) PrimaryGreen else TextDark.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Divider(color = PrimaryGreen.copy(alpha = 0.08f), thickness = 1.dp)

                                // Calculations of days cells
                                val gridCalendar = remember(currentYear, currentMonth) {
                                    Calendar.getInstance().apply {
                                        set(Calendar.YEAR, currentYear)
                                        set(Calendar.MONTH, currentMonth)
                                        set(Calendar.DAY_OF_MONTH, 1)
                                    }
                                }
                                val firstDayOfWeek = gridCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sun
                                val maxDaysInMonth = gridCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                                val totalCells = firstDayOfWeek + maxDaysInMonth
                                val rowsCount = (totalCells + 6) / 7

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (row in 0 until rowsCount) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (col in 0..6) {
                                                val cellIndex = row * 7 + col
                                                if (cellIndex < firstDayOfWeek || cellIndex >= totalCells) {
                                                    // Empty cell
                                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                                } else {
                                                    // Real Day cell
                                                    val dayNum = cellIndex - firstDayOfWeek + 1
                                                    
                                                    // Converted Hijri corresponding info
                                                    val cellHijri = remember(currentYear, currentMonth, dayNum) {
                                                        HijriCalendarHelper.gregorianToHijri(currentYear, currentMonth + 1, dayNum)
                                                    }

                                                    val isSelectedDay = selectedDay == dayNum && selectedMonth == currentMonth && selectedYear == currentYear
                                                    val isTodayDay = todayDay == dayNum && todayMonth == currentMonth && todayYear == currentYear

                                                    val hasEvent = remember(cellHijri) {
                                                        HijriCalendarHelper.getEventForDate(cellHijri.month, cellHijri.day) != null
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(
                                                                when {
                                                                    isSelectedDay -> PrimaryGreen
                                                                    isTodayDay -> PrimaryGreen.copy(alpha = 0.12f)
                                                                    else -> Color.Transparent
                                                                }
                                                            )
                                                            .border(
                                                                width = if (isTodayDay && !isSelectedDay) 1.5.dp else 0.dp,
                                                                color = if (isTodayDay && !isSelectedDay) PrimaryGreen else Color.Transparent,
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable {
                                                                selectedDay = dayNum
                                                                selectedMonth = currentMonth
                                                                selectedYear = currentYear
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            // Gregorian Day Text
                                                            Text(
                                                                text = if (isEn) dayNum.toString() else HijriCalendarHelper.toBengaliNumber(dayNum),
                                                                fontWeight = if (isSelectedDay || isTodayDay) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                                fontSize = 14.sp,
                                                                color = when {
                                                                    isSelectedDay -> Color.White
                                                                    isTodayDay -> PrimaryGreen
                                                                    col == 5 -> PrimaryGreen.copy(alpha = 0.85f)
                                                                    else -> TextDark
                                                                }
                                                            )

                                                            // Arabic/Hijri Day Subtext
                                                            Text(
                                                                text = if (isEn) cellHijri.day.toString() else HijriCalendarHelper.toBengaliNumber(cellHijri.day),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = when {
                                                                    isSelectedDay -> Color.White.copy(alpha = 0.8f)
                                                                    isTodayDay -> PrimaryGreen.copy(alpha = 0.8f)
                                                                    else -> Color(0xFFD97706) // Rich Warm gold/brown color
                                                                }
                                                            )

                                                            // Sparkle Event Dot inside
                                                            if (hasEvent) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(4.dp)
                                                                        .background(
                                                                            color = if (isSelectedDay) Color.White else Color(0xFFF59E0B),
                                                                            shape = CircleShape
                                                                        )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Calendar User Guidelines Card
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            colors = CardDefaults.cardColors(containerColor = CardBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isEn) 
                                        "Note: Hijri dates are computed tabulary and might vary by 1-2 days depending on moon sightings."
                                        else "বিশেষ দ্রষ্টব্য: হিজরি তারিখটি চাঁদ দেখার ওপর নির্ভরশীল বিধায় বাস্তবতার নিরিখে কিছু ক্ষেত্রে ১-২ দিন পার্থক্য হতে পারে।",
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    // ---- ISLAMIC HOLIDAYS DIRECTORY SCREEN ----
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (isEn) "Annual Prophetic Occasions" else "বার্ষিক বিশেষ ইসলামিক দিবসসমূহ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = if (isEn) 
                                "Tap on any day to navigate the calendar directly onto that corresponding date."
                                else "যেকোনো দিবসে ট্যাপ করলে ক্যালেন্ডারটি সরাসরি ওই হিজরি তারিখে নিয়ে যাবে।",
                            fontSize = 11.sp,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Elegant List Layout of Events inside the current active Gregorian Year
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HijriCalendarHelper.events.forEach { event ->
                                val targetCal = remember(currentYear) {
                                    HijriCalendarHelper.findGregorianForHijri(event.hMonth, event.hDay, currentYear)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            targetCal?.let { cal ->
                                                val y = cal.get(Calendar.YEAR)
                                                val m = cal.get(Calendar.MONTH)
                                                val d = cal.get(Calendar.DAY_OF_MONTH)
                                                
                                                // Jump active view screen coordinates
                                                currentYear = y
                                                currentMonth = m
                                                
                                                // Jump chosen dates coordinates
                                                selectedYear = y
                                                selectedMonth = m
                                                selectedDay = d
                                                
                                                // Switch tab back smoothly to monthly layout
                                                activeTabIndex = 0
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBg),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Dynamic Left badge with Islamic Day/Month index
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(PrimaryGreen.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                val numStr = if (isEn) event.hDay.toString() else HijriCalendarHelper.toBengaliNumber(event.hDay)
                                                Text(
                                                    text = numStr,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = PrimaryGreen
                                                )
                                                val monthShort = if (isEn) {
                                                    HijriCalendarHelper.enHijriMonths[event.hMonth - 1].take(3)
                                                } else {
                                                    HijriCalendarHelper.bnHijriMonths[event.hMonth - 1]
                                                }
                                                Text(
                                                    text = monthShort,
                                                    fontSize = if (isEn) 8.sp else 6.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryGreen.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Center Column Info text details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEn) event.nameEn else event.nameBn,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextDark
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isEn) event.descEn else event.descBn,
                                                fontSize = 11.sp,
                                                color = TextGray
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Right Dynamic Navigation Helper
                                        if (targetCal != null) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                val gMonthShort = if (isEn) {
                                                    HijriCalendarHelper.enGregorianMonths[targetCal.get(Calendar.MONTH)].take(3)
                                                } else {
                                                    HijriCalendarHelper.bnGregorianMonths[targetCal.get(Calendar.MONTH)]
                                                }
                                                val gDayStr = if (isEn) {
                                                    targetCal.get(Calendar.DAY_OF_MONTH).toString()
                                                } else {
                                                    HijriCalendarHelper.toBengaliNumber(targetCal.get(Calendar.DAY_OF_MONTH))
                                                }
                                                Text(
                                                    text = "$gDayStr $gMonthShort",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD97706)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = "Navigate to date",
                                                    tint = PrimaryGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "N/A",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}
