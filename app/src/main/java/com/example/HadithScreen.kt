package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.data.HadithData
import com.example.model.Hadith
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedHadith by remember { mutableStateOf<Hadith?>(null) }
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("hadith_bookmarks", Context.MODE_PRIVATE) }

    val allHadiths = HadithData.hadithList
    val filteredHadiths = if (searchQuery.isEmpty()) {
        allHadiths
    } else {
        allHadiths.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.translation.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = BgLight
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedHadith == null) {
                    // Header and Search Bar Column (Connected Card style layout)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(bottom = 12.dp)
                    ) {
                        // Header Row (starts right below status bar)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextDark
                                )
                            }
                            Text(
                                text = "হাদিস সমূহ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        // Connected Premium Slim Search Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "হাদিস খুঁজুন (যেমন: ঈমান বা ১)",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 12.sp,
                                            color = TextDark,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Hadith List with Compact layout & minimal vertical/horizontal spacing
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredHadiths) { hadith ->
                        HadithListItem(
                            hadith = hadith,
                            sharedPrefs = sharedPrefs,
                            onClick = { selectedHadith = hadith }
                        )
                    }
                    
                    if (filteredHadiths.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("কোনো হাদিস পাওয়া যায়নি", color = TextGray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Full Screen Hadith Detail
            selectedHadith?.let { hadith ->
                HadithDetailDialog(hadith = hadith, onDismiss = { selectedHadith = null })
            }
        }
    }
}

@Composable
fun HadithListItem(
    hadith: Hadith,
    sharedPrefs: android.content.SharedPreferences,
    onClick: () -> Unit
) {
    var isSaved by remember(hadith.id) {
        mutableStateOf(sharedPrefs.getBoolean("hadith_${hadith.id}", false))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF0FDF4), CircleShape)
                    .border(1.dp, PrimaryGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hadith.id.toString(),
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hadith.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hadith.category,
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    isSaved = !isSaved
                    sharedPrefs.edit().putBoolean("hadith_${hadith.id}", isSaved).apply()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                    contentDescription = "Save Hadith",
                    tint = if (isSaved) PrimaryGreen else TextGray.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HadithDetailDialog(hadith: Hadith, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextDark
                    )
                }
                Text(
                    text = hadith.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Arabic
                Text(
                    text = hadith.arabic,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 48.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Pronunciation Card
                if (hadith.pronunciation.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BgLight),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "উচ্চারণ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = hadith.pronunciation,
                                fontSize = 16.sp,
                                color = TextDark,
                                lineHeight = 24.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Translation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), 
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "অনুবাদ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = hadith.translation,
                            fontSize = 16.sp,
                            color = TextDark,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Reference
                Text(
                    text = "সূত্র: ${hadith.reference}",
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
