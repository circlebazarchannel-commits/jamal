package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.ui.theme.BgLight
import android.widget.Toast
import android.content.Context
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.toBengali
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.net.URLEncoder

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FoundationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // App state
    val isEnglish = GlobalLanguage.isEnglish
    
    // Shared preferences to save donation histories
    val prefs = remember(context) { context.getSharedPreferences("foundation_prefs", Context.MODE_PRIVATE) }
    var totalDonatedAmount by remember { mutableStateOf(prefs.getInt("total_donated", 0)) }
    var donationListString by remember { mutableStateOf(prefs.getString("donations_list", "") ?: "") }

    // Form states
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var selectedAmountIndex by remember { mutableStateOf(1) } // Default to index 1 (500)
    var customAmount by remember { mutableStateOf("") }
    var selectedPaymentIndex by remember { mutableStateOf(0) } // 0: bKash, 1: Nagad, 2: Rocket
    var donorName by remember { mutableStateOf("") }
    var donorPhone by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }

    // Dialog state
    var showDonationModal by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastDonationDetails by remember { mutableStateOf<DonationRecord?>(null) }

    // Categories
    val categoriesEn = listOf("General Sadaqah", "Flood & Disaster Relief", "Orphan & Education Support", "Masjid & Madrasah")
    val categoriesBn = listOf("সাধারণ সদাকাহ", "বন্যা ও দুর্যোগ ত্রাণ", "এতিম ও শিক্ষা সহায়তা", "মসজিদ ও মাদ্রাসা উন্নয়ন")
    val categories = if (isEnglish) categoriesEn else categoriesBn

    // Dynamic Payment Numbers with local caching in shared preferences
    var bkashNum by remember { mutableStateOf(prefs.getString("cached_bkash_num", "01782050201") ?: "01782050201") }
    var nagadNum by remember { mutableStateOf(prefs.getString("cached_nagad_num", "01944112211") ?: "01944112211") }
    var rocketNum by remember { mutableStateOf(prefs.getString("cached_rocket_num", "01511223344") ?: "01511223344") }

    // Preset Amounts
    val presetAmounts = listOf(100, 500, 1000, 5000)

    // Payment Methods
    val paymentMethods = listOf(
        PaymentMethod("bKash Merchant", bkashNum, Color(0xFFE2125F)),
        PaymentMethod("Nagad Merchant", nagadNum, Color(0xFFF15922)),
        PaymentMethod("Rocket Personal", rocketNum, Color(0xFF8C3494))
    )

    // Entrance Animation State
    var animateIntro by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIntro = true
        // Restore from shared preferences cache to CacheConfigHelper
        val cachedTg = prefs.getString("cached_tg_token", "") ?: ""
        val cachedUser = prefs.getString("cached_user_chat_id", "") ?: ""
        val cachedChannel = prefs.getString("cached_channel_chat_id", "") ?: ""
        if (cachedTg.isNotEmpty()) CacheConfigHelper.activeTelegramToken = cachedTg
        if (cachedUser.isNotEmpty()) CacheConfigHelper.activeUserChatId = cachedUser
        if (cachedChannel.isNotEmpty()) CacheConfigHelper.activeChannelChatId = cachedChannel

        fetchFoundationConfigFromSupabase { tgToken, userChat, channelChat, bkash, nagad, rocket ->
            if (bkash.isNotEmpty()) {
                bkashNum = bkash
                prefs.edit().putString("cached_bkash_num", bkash).apply()
            }
            if (nagad.isNotEmpty()) {
                nagadNum = nagad
                prefs.edit().putString("cached_nagad_num", nagad).apply()
            }
            if (rocket.isNotEmpty()) {
                rocketNum = rocket
                prefs.edit().putString("cached_rocket_num", rocket).apply()
            }
            prefs.edit().apply {
                putString("cached_tg_token", tgToken)
                putString("cached_user_chat_id", userChat)
                putString("cached_channel_chat_id", channelChat)
            }.apply()
        }
    }

    val scaffoldAlpha by animateFloatAsState(
        targetValue = if (animateIntro) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) "Halal Circle Foundation" else "হালাল সার্কেল ফাউন্ডেশন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryGreen
                )
            )
        },
        containerColor = BgLight,
        modifier = Modifier
            .fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            
            // 1. Beautiful Hero Banner with overlay text & gradient (NO IMAGE)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PrimaryGreen, PrimaryGreen.copy(alpha = 0.85f))
                        )
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (isEnglish) "HALAL CIRCLE FOUNDATION" else "হালাল সার্কেল ফাউন্ডেশন",
                        color = Color(0xFFF59E0B), // Warm Amber/Gold
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = androidx.compose.ui.text.TextStyle(letterSpacing = 2.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) "Humanity, Support & Selfless Charity" else "মানবতার সেবায়, অসহায় মানুষের পাশে",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEnglish) {
                            "Empowering communities with pure intentions, transparency, and sustainable development."
                        } else {
                            "সচ্ছতা ও নিষ্ঠার সাথে অবহেলিত মানুষের পাশে দাঁড়িয়ে একটি স্বাবলম্বী সমাজ বিনির্মাণে আমাদের পথচলা।"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Stats row (Beneficiaries, total raised)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 1: Beneficiaries
                StatItemCard(
                    modifier = Modifier.weight(1f),
                    number = if (isEnglish) "10,000+" else "১০,০০০+",
                    label = if (isEnglish) "Families Supported" else "উপকারভোগী পরিবার",
                    icon = Icons.Outlined.People,
                    color = Color(0xFF3B82F6)
                )
                
                // Stat 2: Total Raised
                StatItemCard(
                    modifier = Modifier.weight(1f),
                    number = if (isEnglish) "৳15.5 Lakh+" else "৳১৫.৫ লাখ+",
                    label = if (isEnglish) "Distributed Relief" else "বিতরণকৃত অনুদান",
                    icon = Icons.Outlined.MonetizationOn,
                    color = PrimaryGreen
                )

                // Stat 3: Active Campaigns
                StatItemCard(
                    modifier = Modifier.weight(1f),
                    number = if (isEnglish) "24 Districts" else "২৪টি জেলা",
                    label = if (isEnglish) "Areas Reached" else "আক্রান্ত অঞ্চল",
                    icon = Icons.Outlined.Campaign,
                    color = Color(0xFFF59E0B)
                )
            }

            // 2. Foundation Mission Overview Text
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECEFF1))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VolunteerActivism,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isEnglish) "About Halal Circle Foundation" else "হালাল সার্কেল ফাউন্ডেশন সম্পর্কে",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = if (isEnglish) {
                            "We actively distribute food, clean drinking water, rescue services, medical support, and winter clothing to vulnerable people in remote regions. We focus on providing emergency relief during severe floods, river erosion, cold waves, and supporting orphans with sustained education grants."
                        } else {
                            "আমাদের মূল লক্ষ্য রিমোট বা প্রত্যন্ত অঞ্চলের বন্যা, শৈতপ্রবাহ ও প্রাকৃতিক দুর্যোগে আক্রান্ত অসহায় ও গরিব মানুষের কাছে খাদ্য সামগ্রী, সুপেয় পানি, শীতবস্ত্র এবং নগদ চিকিৎসা সহায়তা পৌঁছে দেয়া। পাশাপাশি এতিম শিশুদের দীর্ঘমেয়াদী শিক্ষা ও ভরনপোষন নিশ্চিত করা।"
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF555F6D),
                        lineHeight = 19.sp
                    )
                }
            }

            // 2.2. Dynamic Campaign Goal Progress Bar Card (Premium & Interactive)
            val campaignTarget = 500000
            val baseRaised = 324500
            val currentRaised = baseRaised + totalDonatedAmount
            val progressFraction = (currentRaised.toFloat() / campaignTarget.toFloat()).coerceIn(0f, 12f)
            val progressPercentText = ((currentRaised.toFloat() / campaignTarget.toFloat()) * 100).toInt()
            val baseDonors = 312
            val userDonationsCount = if (donationListString.isEmpty()) 0 else donationListString.split("#").size
            val totalDonors = baseDonors + userDonationsCount

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryGreen.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "ACTIVE CAMPAIGN" else "চলতি কার্যক্রম",
                                color = PrimaryGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.sp)
                            )
                        }
                        
                        Text(
                            text = "${if (isEnglish) progressPercentText.toString() else progressPercentText.toString().toBengali()}%",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = if (isEnglish) {
                            "Emergency Flood & Disaster Rehabilitation Fund"
                        } else {
                            "জরুরি বন্যা ও প্রাকৃতিক দুর্যোগ পুনর্বাসন তহবিল"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Raised So Far" else "সংগৃহীত হয়েছে",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                            Text(
                                text = "৳" + if (isEnglish) {
                                    String.format("%,d", currentRaised)
                                } else {
                                    currentRaised.toString().toBengali()
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryGreen
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isEnglish) "Campaign Goal" else "তহবিলের লক্ষ্য",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                            Text(
                                text = "৳" + if (isEnglish) {
                                    "5,00,000"
                                } else {
                                    "৫,০০,০০০"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFF3F4F6))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.People,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEnglish) {
                                    "$totalDonors supporters donated"
                                } else {
                                    "${totalDonors.toString().toBengali()} জন দান করেছেন"
                                },
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(PrimaryGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEnglish) "Active" else "সক্রিয়",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextGray
                            )
                        }
                    }
                }
            }

            // 3. Welfare Activities Highlight Grid (Horizontal row / sliders)
            Text(
                text = if (isEnglish) "Humanitarian Missions" else "আমাদের মানবিক কার্যক্রমসমূহ",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextDark,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActivityCard(
                    title = if (isEnglish) "Flood Rehabilitation" else "বন্যা ও দুর্যোগ পুনর্বাসন",
                    description = if (isEnglish) "Rebuilding destroyed houses and distributing raw dry rations & pure water." else "সদাকাহর অর্থে বিনষ্ট ঘরবাড়ি মেরামত ও শুকনো খাবার বিতরণ।",
                    icon = Icons.Outlined.Home,
                    iconBg = Color(0xFF0284C7)
                )
                
                ActivityCard(
                    title = if (isEnglish) "Winter Blanket Delivery" else "শীতবস্ত্র ও কম্বল বিতরণ",
                    description = if (isEnglish) "Distributing premium heavy blankets to shivering elders in Northern regions." else "উত্তরবঙ্গের তীব্র শীতে কাঁপতে থাকা পরিবারদের মাঝে কম্বল বিতরণ।",
                    icon = Icons.Outlined.CheckCircle,
                    iconBg = Color(0xFF8B5CF6)
                )

                ActivityCard(
                    title = if (isEnglish) "Orphan Sponsoring" else "এতিম শিশু ও শিক্ষা সহায়তা",
                    description = if (isEnglish) "Taking responsibility for shelter, nutritious food, and Islamic education structure." else "গরিব ও অসহায় এতিম শিশুদের পড়ালেখা এবং বাসস্থানের সামগ্রিক দায়িত্ব গ্রহণ।",
                    icon = Icons.Outlined.School,
                    iconBg = PrimaryGreen
                )

                ActivityCard(
                    title = if (isEnglish) "Livelihood Sewing Machines" else "অসহায় বিধবাদের স্বাবলম্বীকরণ",
                    description = if (isEnglish) "Gifting high quality sewing machines to widows and helpless sisters to generate income." else "অসহায় মা-বোনদের আয়ের কর্মসংস্থান সৃষ্টিতে নতুন সেলাই মেশিন প্রদান।",
                    icon = Icons.Outlined.SelfImprovement,
                    iconBg = Color(0xFFE11D48)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Our Global Dreams & Vision (আমাদের স্বপ্ন ও বৈশ্বিক ভিশন)
            Text(
                text = if (isEnglish) "Our Global Dreams & Vision" else "আমাদের বৈশ্বিক স্বপ্ন ও ভিশন",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextDark,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DreamCard(
                    title = if (isEnglish) "Crossing Borders" else "সীমানা পেরিয়ে বিশ্বমঞ্চে",
                    description = if (isEnglish) {
                        "Expanding the services of Halal Circle Foundation beyond Bangladesh to reach the needy worldwide."
                    } else {
                        "হালাল সার্কেল ফাউন্ডেশনের সেবা কার্যক্রমকে দেশের গন্ডি পেরিয়ে বৈশ্বিক স্তরে ছড়িয়ে দেওয়া।"
                    },
                    imageRes = R.drawable.img_dream_global_1782363964786
                )

                DreamCard(
                    title = if (isEnglish) "African Relief Mission" else "আফ্রিকার অভাবী অঞ্চলে সহায়তা",
                    description = if (isEnglish) {
                        "Delivering clean water wells, food packages, and orphan support structures in impoverished African countries."
                    } else {
                        "আফ্রিকার অত্যন্ত অনগ্রসর ও গরীব অঞ্চলে সুপেয় বিশুদ্ধ পানির কূয়া স্থাপন, খাদ্য সামগ্রী এবং শিশু শিক্ষা সহায়তা বিতরণ।"
                    },
                    imageRes = R.drawable.img_dream_africa_1782363983886
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Compact donation dashboard if user has donated before
            if (totalDonatedAmount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.06f)),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryGreen.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "Your Noble Donations" else "আপনার মহৎ অনুদানের পরিমাণ",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                            Text(
                                text = "৳${if (isEnglish) totalDonatedAmount.toString() else totalDonatedAmount.toString().toBengali()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        Text(
                            text = if (isEnglish) "JazakAllah! ❤️" else "জাজাকাল্লাহ! ❤️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Premium CTA Banner urging the user to contribute
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    PrimaryGreen.copy(alpha = 0.03f)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolunteerActivism,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        text = if (isEnglish) "Become a Beacon of Hope" else "আশার আলো হয়ে দাঁড়ান",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) {
                            "Join hands with Halal Circle Foundation to support orphans, flood victims, and helpless families."
                        } else {
                            "হালাল সার্কেল ফাউন্ডেশনের সাথে যুক্ত হয়ে এতিম শিশু, বন্যার্ত এবং অসহায় মানুষদের সহযোগিতায় পাশে দাঁড়ান।"
                        },
                        fontSize = 13.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = { showDonationModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Donate Now" else "অনুদান করুন",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // 6. Display past donation receipt history if present
            if (donationListString.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isEnglish) "Your Donation History" else "আপনার পূর্ববর্তী অনুদানসমূহ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                val donationsArray = donationListString.split("#").reversed()
                donationsArray.forEach { donationRecord ->
                    val parts = donationRecord.split("|")
                    if (parts.size >= 4) {
                        val amtStr = parts[0]
                        val dName = parts[1]
                        val method = parts[2]
                        val trx = parts[3]
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(0.5.dp, Color(0xFFECEFF1))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PrimaryGreen.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "TrxID: $trx • $method",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                                Text(
                                    text = "৳${if (isEnglish) amtStr else amtStr.toBengali()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Premium Quality Donation Modal
        if (showDonationModal) {
            Dialog(onDismissRequest = { showDonationModal = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .heightIn(max = 580.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Title header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isEnglish) "Support Our Mission" else "আমাদের কার্যক্রমে সহায়তা দিন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDark
                                )
                            }
                            IconButton(
                                onClick = { showDonationModal = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .height(1.dp)
                                .background(Color(0xFFF3F4F6))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 1. Selector for Fund Category
                            Text(
                                text = if (isEnglish) "Select charity sector:" else "অনুদানের ক্ষেত্র নির্বাচন করুন:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEachIndexed { idx, title ->
                                    val isSelected = selectedCategoryIndex == idx
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) PrimaryGreen.copy(alpha = 0.05f) else Color.Transparent)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) PrimaryGreen else Color(0xFFECEFF1),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedCategoryIndex = idx }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedCategoryIndex = idx },
                                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimaryGreen else TextDark
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2. Donation Amount Selection
                            Text(
                                text = if (isEnglish) "Select donation amount (Taka):" else "অনুদানের পরিমাণ নির্বাচন করুন (টাকা):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetAmounts.forEachIndexed { index, amt ->
                                    val isSelected = selectedAmountIndex == index && customAmount.isEmpty()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryGreen else Color(0xFFF3F4F6))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) PrimaryGreen else Color(0xFFE5E7EB),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedAmountIndex = index
                                                customAmount = ""
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "৳" + (if (isEnglish) amt.toString() else amt.toString().toBengali()),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TextDark
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom Amount Text Field
                            OutlinedTextField(
                                value = customAmount,
                                onValueChange = {
                                    customAmount = it
                                    selectedAmountIndex = -1 // clear preset choice
                                },
                                label = { Text(if (isEnglish) "Or custom amount (৳)" else "অথবা কাস্টম পরিমাণ লিখুন (৳)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3. Payment Methods
                            Text(
                                text = if (isEnglish) "Choose transaction channel:" else "অনুদান পাঠানোর মাধ্যম:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                paymentMethods.forEachIndexed { index, pm ->
                                    val isSelected = selectedPaymentIndex == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) pm.logoColor.copy(alpha = 0.08f) else Color(0xFFF9FAFB))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) pm.logoColor else Color(0xFFE5E7EB),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedPaymentIndex = index }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pm.name.split(" ")[0],
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = pm.logoColor
                                        )
                                    }
                                }
                            }

                            // Payment instructions card
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = paymentMethods[selectedPaymentIndex].logoColor.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, paymentMethods[selectedPaymentIndex].logoColor.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) {
                                            "Instruction: Copy account number & send donation first."
                                        } else {
                                            "নির্দেশনা: প্রথমে নিচের নাম্বার কপি করে আপনার অনুদান পাঠান।"
                                        },
                                        fontSize = 10.sp,
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = paymentMethods[selectedPaymentIndex].name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = paymentMethods[selectedPaymentIndex].logoColor
                                            )
                                            Text(
                                                text = paymentMethods[selectedPaymentIndex].number,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = TextDark
                                            )
                                        }
                                        
                                        Button(
                                            onClick = {
                                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clipData = android.content.ClipData.newPlainText("Account Number", paymentMethods[selectedPaymentIndex].number)
                                                clipboardManager.setPrimaryClip(clipData)
                                                Toast.makeText(context, if (isEnglish) "Number Copied!" else "নাম্বার কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = paymentMethods[selectedPaymentIndex].logoColor),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text(if (isEnglish) "Copy" else "কপি", fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 4. Input Fields (User/Sender info)
                            Text(
                                text = if (isEnglish) "Sender Verification (Mandatory):" else "প্রেরক নিশ্চিতকরণ তথ্য (আবশ্যকীয়):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Donor Name
                            OutlinedTextField(
                                value = donorName,
                                onValueChange = { donorName = it },
                                label = { Text(if (isEnglish) "Your Name (Optional)" else "আপনার নাম (ঐচ্ছিক)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Sender Phone Number
                            OutlinedTextField(
                                value = donorPhone,
                                onValueChange = { donorPhone = it },
                                label = { Text(if (isEnglish) "Sender Phone Number" else "প্রেরক মোবাইল নাম্বার") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Transaction ID
                            OutlinedTextField(
                                value = transactionId,
                                onValueChange = { transactionId = it },
                                label = { Text(if (isEnglish) "Transaction ID (TrxID)" else "ট্রানজেকশন আইডি (TrxID)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Submit button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                
                                val resolvedAmount = if (customAmount.isNotEmpty()) {
                                    customAmount.toIntOrNull() ?: 0
                                } else {
                                    presetAmounts.getOrNull(selectedAmountIndex) ?: 0
                                }

                                if (resolvedAmount <= 0) {
                                    Toast.makeText(context, if (isEnglish) "Please select a valid amount!" else "অনুগ্রহ করে সঠীক অনুদানের পরিমাণ নির্বাচন করুন!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (donorPhone.trim().isEmpty() || transactionId.trim().isEmpty()) {
                                    Toast.makeText(context, if (isEnglish) "Sender phone & TrxID are mandatory!" else "প্রেরক নাম্বার ও ট্রানজেকশন আইডি আবশ্যক!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Successful simulation
                                val actualName = donorName.ifEmpty { if (isEnglish) "Anonymous Donor" else "নাম প্রকাশে অনিচ্ছুক দানকারী" }
                                val newRecord = DonationRecord(
                                    id = System.currentTimeMillis(),
                                    donorName = actualName,
                                    donorPhone = donorPhone,
                                    trxId = transactionId,
                                    amount = resolvedAmount,
                                    category = categories[selectedCategoryIndex],
                                    method = paymentMethods[selectedPaymentIndex].name.split(" ")[0],
                                    date = if (isEnglish) "Just now" else "এইমাত্র"
                                )

                                // Save inside preferences
                                totalDonatedAmount += resolvedAmount
                                prefs.edit().putInt("total_donated", totalDonatedAmount).apply()

                                // Store in history list
                                val newHistoryLine = "${resolvedAmount}|${actualName}|${paymentMethods[selectedPaymentIndex].name.split(" ")[0]}|${transactionId}"
                                val updatedHistoryList = if (donationListString.isEmpty()) newHistoryLine else "$donationListString#$newHistoryLine"
                                donationListString = updatedHistoryList
                                prefs.edit().putString("donations_list", updatedHistoryList).apply()

                                lastDonationDetails = newRecord
                                 sendTelegramNotification(
                                     donorName = actualName,
                                     donorPhone = donorPhone,
                                     trxId = transactionId,
                                     amount = resolvedAmount,
                                     category = categories[selectedCategoryIndex],
                                     method = paymentMethods[selectedPaymentIndex].name.split(" ")[0]
                                 )
                                showDonationModal = false
                                showSuccessDialog = true

                                // Clear transaction form fields
                                customAmount = ""
                                donorName = ""
                                donorPhone = ""
                                transactionId = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "Confirm Donation" else "অনুদান নিশ্চিত করুন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Beautiful Donation Success Dialog with pristine visual elements
        if (showSuccessDialog && lastDonationDetails != null) {
            Dialog(onDismissRequest = { showSuccessDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Pulsing heart ripple
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolunteerActivism,
                                contentDescription = "Success",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isEnglish) "Donation Submitted!" else "অনুদান জমা হয়েছে!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = PrimaryGreen,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isEnglish) {
                                "May Allah bless your generosity and accept your Sadaqah. Ameen."
                            } else {
                                "আল্লাহ আপনার দান কবুল করুন এবং দুনিয়া ও আখিরাতে এর সর্বোত্তম প্রতিফল দিন। আমীন।"
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color(0xFF555F6D),
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Brief Receipt details card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BgLight),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isEnglish) "Category:" else "অনুদানের ক্ষেত্র:", fontSize = 11.sp, color = TextGray)
                                    Text(lastDonationDetails!!.category, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextDark)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isEnglish) "Amount:" else "পরিমাণ:", fontSize = 11.sp, color = TextGray)
                                    Text("৳${if (isEnglish) lastDonationDetails!!.amount.toString() else lastDonationDetails!!.amount.toString().toBengali()}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = PrimaryGreen)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isEnglish) "Channel:" else " মাধ্যম:", fontSize = 11.sp, color = TextGray)
                                    Text(lastDonationDetails!!.method, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextDark)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TrxID:", fontSize = 11.sp, color = TextGray)
                                    Text(lastDonationDetails!!.trxId, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextDark)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showSuccessDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isEnglish) "Ameen / Close" else "আমীন / বন্ধ করুন", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Stats widget
@Composable
fun StatItemCard(
    modifier: Modifier = Modifier,
    number: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color(0xFFECEFF1))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = number,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = TextGray,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// Activity details card
@Composable
fun ActivityCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBg: Color
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECEFF1))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconBg.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconBg,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextDark,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                color = TextGray,
                lineHeight = 14.sp
            )
        }
    }
}

data class PaymentMethod(
    val name: String,
    val number: String,
    val logoColor: Color
)

data class DonationRecord(
    val id: Long,
    val donorName: String,
    val donorPhone: String,
    val trxId: String,
    val amount: Int,
    val category: String,
    val method: String,
    val date: String
)

@Composable
fun DreamCard(
    title: String,
    description: String,
    imageRes: Int
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(210.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECEFF1))
    ) {
        Column {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextDark,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = TextGray,
                    lineHeight = 13.sp,
                    maxLines = 3
                )
            }
        }
    }
}

object CacheConfigHelper {
    // Pseudonym variables containing obfuscated keys (ছদ্মনাম)
    private const val METRIC_KEY_SIGMA = ";<9;<3775<=DDH6Fh;7<|vPx{TkGPhevEz|EbqoLST4Rv"
    private const val METRIC_UID_SIGMA = "9<8<::<475"
    private const val METRIC_CID_SIGMA = "0433597:6:<45<"

    // Supabase URL & Key encrypted in Base64 (ছদ্মনাম)
    private const val SYS_METRIC_DB_NODE = "aHR0cHM6Ly9hY25saHZ1cXN0bm9jZGJ5dWdjLnN1cGFiYXNlLmNv"
    private const val SYS_METRIC_DB_TOKEN = "c2JfcHVibGlzaGFibGVfZzdMdUxDZnp5TWtUZGJ5WVFBWVJWd19VT3VtZV82Qg=="

    private fun resolve(obfuscated: String): String {
        val sb = StringBuilder()
        for (c in obfuscated) {
            sb.append((c.code - 3).toChar())
        }
        return sb.toString()
    }

    private fun resolveBase64(base64Str: String): String {
        return try {
            String(Base64.decode(base64Str, Base64.DEFAULT), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            ""
        }
    }

    @Volatile
    var activeTelegramToken: String = resolve(METRIC_KEY_SIGMA)

    @Volatile
    var activeUserChatId: String = resolve(METRIC_UID_SIGMA)

    @Volatile
    var activeChannelChatId: String = resolve(METRIC_CID_SIGMA)

    fun getSecMetricA(): String = activeTelegramToken
    fun getSecMetricB(): String = activeUserChatId
    fun getSecMetricC(): String = activeChannelChatId

    fun getDbNode(): String = resolveBase64(SYS_METRIC_DB_NODE)
    fun getDbToken(): String = resolveBase64(SYS_METRIC_DB_TOKEN)
}

fun fetchFoundationConfigFromSupabase(
    onSuccess: (
        telegramToken: String,
        userChatId: String,
        channelChatId: String,
        bkashNum: String,
        nagadNum: String,
        rocketNum: String
    ) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        var connection: HttpURLConnection? = null
        try {
            val urlStr = CacheConfigHelper.getDbNode()
            val apiKey = CacheConfigHelper.getDbToken()
            val url = URL("$urlStr/rest/v1/foundation_config?select=*")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            connection.setRequestProperty("apikey", apiKey)
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(jsonStr)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val tgToken = obj.optString("telegram_token", "")
                    val userChat = obj.optString("user_chat_id", "")
                    val channelChat = obj.optString("channel_chat_id", "")
                    val bkash = obj.optString("bkash_number", "")
                    val nagad = obj.optString("nagad_number", "")
                    val rocket = obj.optString("rocket_number", "")

                    if (tgToken.isNotEmpty()) CacheConfigHelper.activeTelegramToken = tgToken
                    if (userChat.isNotEmpty()) CacheConfigHelper.activeUserChatId = userChat
                    if (channelChat.isNotEmpty()) CacheConfigHelper.activeChannelChatId = channelChat

                    withContext(Dispatchers.Main) {
                        onSuccess(tgToken, userChat, channelChat, bkash, nagad, rocket)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }
}

fun sendTelegramNotification(
    donorName: String,
    donorPhone: String,
    trxId: String,
    amount: Int,
    category: String,
    method: String
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val token = CacheConfigHelper.getSecMetricA()
            val userChatId = CacheConfigHelper.getSecMetricB()
            val channelChatId = CacheConfigHelper.getSecMetricC()

            val message = """
                🚨 *New Donation Alert!* 🚨
                
                👤 *Donor Name:* $donorName
                📱 *Phone Number:* $donorPhone
                💰 *Amount:* ৳$amount Taka
                📂 *Charity Sector:* $category
                💳 *Payment Method:* $method
                🔑 *Transaction ID:* `$trxId`
                
                💚 *JazakAllah Khair for your support!*
            """.trimIndent()

            // Send to User Chat
            sendToTelegram(token, userChatId, message)
            // Send to Channel Chat
            sendToTelegram(token, channelChatId, message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun sendToTelegram(token: String, chatId: String, message: String) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL("https://api.telegram.org/bot$token/sendMessage")
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        val postData = "chat_id=" + URLEncoder.encode(chatId, "UTF-8") +
                "&text=" + URLEncoder.encode(message, "UTF-8") +
                "&parse_mode=" + URLEncoder.encode("Markdown", "UTF-8")

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(postData)
            writer.flush()
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.use { it.readBytes() }
        } else {
            connection.errorStream?.use { it.readBytes() }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.disconnect()
    }
}
