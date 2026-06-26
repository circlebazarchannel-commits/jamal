package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.ui.theme.BgLight
import com.example.viewmodel.GlobalLanguage
import com.example.widget.WidgetUtils

@Composable
fun HomeScreenWidgetsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isEng = GlobalLanguage.isEnglish

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
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
                    text = if (isEng) "Home Screen Widgets" else "হোম স্ক্রিন উইজেট",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        containerColor = BgLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Intro info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isEng) "Premium Home Screen Widgets" else "প্রিমিয়াম হোম স্ক্রিন উইজেট",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEng) {
                                "Add beautifully crafted Islamic widgets directly to your mobile home screen. These widgets show accurate real-time prayer times and countdowns based on your selected location without opening the app!"
                            } else {
                                "আপনার মোবাইলের হোম স্ক্রিনে সরাসরি চমৎকার সব ইসলামিক উইজেট যোগ করুন। অ্যাপ ওপেন না করেই আপনার নির্বাচিত লোকেশন অনুযায়ী সঠিক নামাজের সময়, হিজরি তারিখ ও পরবর্তী নামাজের কাউন্টডাউন রিয়েল-টাইমে দেখতে পারবেন।"
                            },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = TextDark.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Widget 1: Five Daily Prayers (Medium/Large Size)
            WidgetPreviewCard(
                title = if (isEng) "1. Five Daily Prayers Widget (White Theme)" else "১. পাঁচ ওয়াক্ত নামাজের সময় উইজেট (হোয়াইট থিম)",
                description = if (isEng) "Displays all daily prayer times, location, Gregorian and Hijri dates." else "পাঁচ ওয়াক্ত নামাজের সময়, হিজরি ও ইংরেজি তারিখ এবং আপনার নির্বাচিত লোকেশন দেখায়।",
                sizeLabel = if (isEng) "Suggested Size: 4x2 or 4x3" else "প্রস্তাবিত সাইজ: ৪x২ অথবা ৪x৩"
            ) {
                // Actual interactive/styled preview of the widget layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    // Header inside preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEng) "Tuesday, 23 June 2026" else "মঙ্গলবার, ২৩ জুন ২০২৬",
                                color = Color(0xFF334155),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEng) "08 Muharram, 1448 AH" else "০৮ মহররম, ১৪৪৮ হিজরি",
                                color = Color(0xFF15803D),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isEng) "Dhaka" else "ঢাকা",
                                color = Color(0xFF1E293B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulated rows
                    val prayerList = if (isEng) {
                        listOf("Fajr" to "04:12 AM", "Dhuhr" to "12:15 PM", "Asr" to "04:30 PM", "Maghrib" to "06:45 PM", "Isha" to "08:15 PM")
                    } else {
                        listOf("ফজর (Fajr)" to "০৪:১২ এএম", "যোহর (Dhuhr)" to "১২:১৫ পিএম", "আসর (Asr)" to "০৪:৩০ পিএম", "মাগরিব (Maghrib)" to "০৬:৪৫ পিএম", "এশা (Isha)" to "০৮:১৫ পিএম")
                    }

                    Column {
                        prayerList.forEach { (name, time) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, color = Color(0xFF1E293B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(time, color = Color(0xFF15803D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        WidgetUtils.pinWidget(context, "com.example.widget.PrayerTimesWidgetProvider")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEng) "Add Widget to Home Screen" else "হোম স্ক্রিনে যোগ করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Widget 2: Next Prayer Countdown (Small/Medium Size)
            WidgetPreviewCard(
                title = if (isEng) "2. Next Prayer Countdown Widget (White Theme)" else "২. পরবর্তী নামাজের Countdown উইজেট (হোয়াইট থিম)",
                description = if (isEng) "Displays next prayer name, a live countdown timer, and selected location." else "পরবর্তী নামাজের নাম এবং নামাজ শুরু হতে কত সময় বাকি আছে তার লাইভ কাউন্টডাউন দেখায়।",
                sizeLabel = if (isEng) "Suggested Size: 2x2 or 3x2" else "প্রস্তাবিত সাইজ: ২x২ অথবা ৩x২"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEng) "Dhaka" else "ঢাকা",
                            color = Color(0xFF15803D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEng) "08 Muharram" else "০৮ মহররম",
                            color = Color(0xFF475569),
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEng) "NEXT PRAYER IN" else "পরবর্তী নামাজের বাকি",
                            color = Color(0xFF475569),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEng) "Dhuhr (যোহর)" else "যোহর (Dhuhr)",
                            color = Color(0xFF1E293B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = if (isEng) "02:15:30" else "০২:১৫:৩০",
                            color = Color(0xFF15803D),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEng) "23 June 2026" else "২৩ জুন ২০২৬",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        WidgetUtils.pinWidget(context, "com.example.widget.NextPrayerWidgetProvider")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEng) "Add Widget to Home Screen" else "হোম স্ক্রিনে যোগ করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Widget 3: Sunrise (Small Size)
            WidgetPreviewCard(
                title = if (isEng) "3. Sunrise Widget (White Theme)" else "৩. সূর্যোদয় উইজেট (হোয়াইট থিম)",
                description = if (isEng) "Displays precise sunrise time, current Islamic Hijri date, and location." else "সূর্যোদয়ের সুনির্দিষ্ট সময়, আজকের হিজরি তারিখ ও আপনার লোকেশন দেখায়।",
                sizeLabel = if (isEng) "Suggested Size: 2x1 or 2x2" else "প্রস্তাবিত সাইজ: ২x১ অথবা ২x২"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEng) "Dhaka" else "ঢাকা",
                            color = Color(0xFF15803D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEng) "08 Muharram" else "০৮ মহররম",
                            color = Color(0xFF475569),
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEng) "SUNRISE" else "সূর্যোদয়",
                            color = Color(0xFFB45309),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEng) "05:12 AM" else "০৫:১২ এএম",
                            color = Color(0xFF1E293B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEng) "23 June 2026" else "২৩ জুন ২০২৬",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        WidgetUtils.pinWidget(context, "com.example.widget.SunriseWidgetProvider")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEng) "Add Widget to Home Screen" else "হোম স্ক্রিনে যোগ করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Widget 4: Sunset (Small Size)
            WidgetPreviewCard(
                title = if (isEng) "4. Sunset Widget (White Theme)" else "৪. সূর্যাস্ত উইজেট (হোয়াইট থিম)",
                description = if (isEng) "Displays precise sunset time, current Islamic Hijri date, and location." else "সূর্যাস্তের সুনির্দিষ্ট সময়, আজকের হিজরি তারিখ ও আপনার লোকেশন দেখায়।",
                sizeLabel = if (isEng) "Suggested Size: 2x1 or 2x2" else "প্রস্তাবিত সাইজ: ২x১ অথবা ২x২"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEng) "Dhaka" else "ঢাকা",
                            color = Color(0xFF15803D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEng) "08 Muharram" else "০৮ মহররম",
                            color = Color(0xFF475569),
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEng) "SUNSET" else "সূর্যাস্ত",
                            color = Color(0xFFBE123C),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEng) "06:45 PM" else "০৬:৪৫ পিএম",
                            color = Color(0xFF1E293B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEng) "23 June 2026" else "২৩ জুন ২০২৬",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        WidgetUtils.pinWidget(context, "com.example.widget.SunsetWidgetProvider")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEng) "Add Widget to Home Screen" else "হোম স্ক্রিনে যোগ করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Widget 5: Date (Islamic + English) (Small/Medium Size)
            WidgetPreviewCard(
                title = if (isEng) "5. Date Widget (White Theme)" else "৫. তারিখ উইজেট (হোয়াইট থিম)",
                description = if (isEng) "Displays both the current Islamic Hijri date and the English Gregorian date beautifully." else "আজকের হিজরি তারিখ এবং ইংরেজি তারিখ একসাথে অত্যন্ত সুন্দর ও প্রিমিয়াম ডিজাইনে দেখায়।",
                sizeLabel = if (isEng) "Suggested Size: 2x1 or 2x2" else "প্রস্তাবিত সাইজ: ২x১ অথবা ২x২"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEng) "Dhaka" else "ঢাকা",
                            color = Color(0xFF15803D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEng) "Today's Date" else "আজকের তারিখ",
                            color = Color(0xFF475569),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEng) "08 Muharram, 1448 AH" else "০৮ মহররম, ১৪৪৮ হিজরি",
                            color = Color(0xFF15803D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(modifier = Modifier.width(30.dp).height(0.5.dp).background(Color(0xFFE2E8F0)).padding(vertical = 3.dp))
                        Text(
                            text = if (isEng) "Tuesday, 23 June 2026" else "মঙ্গলবার, ২৩ জুন ২০২৬",
                            color = Color(0xFF1E293B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        WidgetUtils.pinWidget(context, "com.example.widget.DateWidgetProvider")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEng) "Add Widget to Home Screen" else "হোম স্ক্রিনে যোগ করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Manual Adding Guide
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isEng) "Having issues adding directly?" else "সরাসরি যোগ করতে সমস্যা হচ্ছে?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEng) {
                            "If your mobile phone's default launcher does not support direct adding, you can still add them manually:\n1. Go to your mobile Home Screen.\n2. Tap and hold on an empty space.\n3. Select 'Widgets' from the menu.\n4. Scroll and find 'Halal Circle'.\n5. Choose your preferred widget and drag it to your screen!"
                        } else {
                            "আপনার ফোনের লঞ্চার যদি সরাসরি যোগ করা সাপোর্ট না করে, তাহলে নিচের নিয়ম অনুসরণ করে ম্যানুয়ালি যোগ করতে পারবেন:\n১. মোবাইলের হোম স্ক্রিনের ফাঁকা জায়গায় ট্যাপ করে ধরে রাখুন।\n২. নিচে আসা অপশনগুলো থেকে 'Widgets' (উইজেট) নির্বাচন করুন।\n৩. লিস্ট থেকে স্ক্রল করে 'Halal Circle' অ্যাপটি খুঁজুন।\n৪. আপনার পছন্দের সাইজের উইজেটটি ট্যাপ করে ধরে হোম স্ক্রিনে টেনে এনে বসান!"
                        },
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetPreviewCard(
    title: String,
    description: String,
    sizeLabel: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = TextGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(sizeLabel, fontSize = 10.sp, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
            
            Spacer(modifier = Modifier.height(14.dp))
            
            content()
        }
    }
}
