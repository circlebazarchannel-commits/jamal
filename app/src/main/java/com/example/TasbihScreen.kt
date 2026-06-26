package com.example

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.TrackerViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(onBack: () -> Unit) {
    val trackerViewModel: TrackerViewModel = viewModel()
    val state by trackerViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    val isEng = com.example.viewmodel.GlobalLanguage.isEnglish
    
    // Scale animation for the count text
    var countForAnimation by remember { mutableIntStateOf(state.currentTracker.tasbihCount) }
    val scale = remember { Animatable(1f) }
    
    LaunchedEffect(state.currentTracker.tasbihCount) {
        if (state.currentTracker.tasbihCount > countForAnimation) {
            countForAnimation = state.currentTracker.tasbihCount
            scale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
            )
        } else {
            countForAnimation = state.currentTracker.tasbihCount
        }
    }
    
    fun performVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isEng) "Digital Tasbih" else "ডিজিটাল তসবীহ",
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Header Info Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryGreen.copy(alpha = 0.8f), PrimaryGreen)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isEng) "Daily Dhikr Goal" else "প্রতিদিনের জিকির লক্ষ্য",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isEng) "Keep your tongue moist with the remembrance of Allah." else "সর্বদা আল্লাহর জিকিরে জিহবাকে সিক্ত রাখুন।",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Tasbih Counter Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB).copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEng) "Current Count" else "বর্তমান গণনা",
                            fontWeight = FontWeight.SemiBold,
                            color = TextGray,
                            fontSize = 14.sp
                        )
                        
                        IconButton(
                            onClick = { 
                                trackerViewModel.resetTasbih() 
                                performVibration()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = TextDark, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // The Circular Counter Display
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(180.dp)
                    ) {
                        // Background circle
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFFF1F5F9),
                            strokeWidth = 10.dp,
                            strokeCap = StrokeCap.Round
                        )
                        
                        // Animated progress circle (fake progress based on count % 100)
                        val progress = (state.currentTracker.tasbihCount % 100) / 100f
                        CircularProgressIndicator(
                            progress = { if (progress == 0f && state.currentTracker.tasbihCount > 0) 1f else progress },
                            modifier = Modifier.fillMaxSize(),
                            color = PrimaryGreen,
                            strokeWidth = 10.dp,
                            strokeCap = StrokeCap.Round
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.scale(scale.value)
                        ) {
                            Text(
                                text = state.currentTracker.tasbihCount.toBengaliDigits(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Big Tap Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(PrimaryGreen.copy(alpha = 0.9f), PrimaryGreen)
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current
                            ) {
                                performVibration()
                                trackerViewModel.incrementTasbih()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEng) "TAP TO COUNT" else "জিকির করুন",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Suggestions / Popular Dhikr Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEng) "Suggested Dhikr" else "প্রস্তাবিত জিকির",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    val suggestions = if (isEng) {
                        listOf("Subhanallah (33 times)", "Alhamdulillah (33 times)", "Allahu Akbar (34 times)")
                    } else {
                        listOf("সুবহানাল্লাহ (৩৩ বার)", "আলহামদুলিল্লাহ (৩৩ বার)", "আল্লাহু আকবার (৩৪ বার)")
                    }
                    
                    suggestions.forEachIndexed { index, text ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PrimaryGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = text, color = TextDark, fontSize = 14.sp)
                        }
                        if (index < suggestions.size - 1) {
                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
