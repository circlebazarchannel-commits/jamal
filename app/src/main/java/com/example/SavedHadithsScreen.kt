package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HadithData
import com.example.model.Hadith
import com.example.ui.theme.BgLight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedHadithsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("hadith_bookmarks", Context.MODE_PRIVATE) }
    
    var savedHadithIds by remember { 
        mutableStateOf(
            sharedPrefs.all.filter { it.value == true }.keys
                .filter { it.startsWith("hadith_") }
                .mapNotNull { it.removePrefix("hadith_").toIntOrNull() }
                .toSet()
        )
    }
    
    var selectedHadith by remember { mutableStateOf<Hadith?>(null) }

    val allHadiths = HadithData.hadithList
    val savedHadiths = allHadiths.filter { savedHadithIds.contains(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সেভ করা হাদিস",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
            )
        },
        containerColor = BgLight
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (savedHadiths.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো হাদিস সেভ করা নেই", color = TextGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(savedHadiths) { hadith ->
                        SavedHadithListItem(
                            hadith = hadith,
                            isSaved = true,
                            onClick = { selectedHadith = hadith },
                            onToggleSave = {
                                val currentlySaved = sharedPrefs.getBoolean("hadith_${hadith.id}", false)
                                sharedPrefs.edit().putBoolean("hadith_${hadith.id}", !currentlySaved).apply()
                                savedHadithIds = sharedPrefs.all.filter { it.value == true }.keys
                                    .filter { it.startsWith("hadith_") }
                                    .mapNotNull { it.removePrefix("hadith_").toIntOrNull() }
                                    .toSet()
                            }
                        )
                    }
                }
            }

            selectedHadith?.let { hadith ->
                HadithDetailDialog(hadith = hadith, onDismiss = { selectedHadith = null })
            }
        }
    }
}

@Composable
fun SavedHadithListItem(hadith: Hadith, isSaved: Boolean, onClick: () -> Unit, onToggleSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hadith.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )
                Text(
                    text = hadith.category,
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
            IconButton(
                onClick = onToggleSave,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                    contentDescription = "Save",
                    tint = if (isSaved) PrimaryGreen else TextGray
                )
            }
        }
        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
    }
}
