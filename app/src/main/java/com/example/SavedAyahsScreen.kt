package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BgLight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray

data class SavedAyah(val surah: Surah, val verse: Verse)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAyahsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("quran_bookmarks", Context.MODE_PRIVATE) }
    
    var savedAyahKeys by remember { 
        mutableStateOf(
            sharedPrefs.all.filter { it.value == true }.keys
                .filter { it.matches(Regex("\\d+_\\d+")) }
                .toSet()
        )
    }

    // Prepare list of SavedAyahs
    val allSurahs = remember { getSurahList() }
    val savedAyahsList = remember(savedAyahKeys) {
        val list = mutableListOf<SavedAyah>()
        for (key in savedAyahKeys) {
            val parts = key.split("_")
            if (parts.size == 2) {
                val surahId = parts[0].toIntOrNull()
                val verseNo = parts[1].toIntOrNull()
                if (surahId != null && verseNo != null) {
                    val surah = allSurahs.find { it.id == surahId }
                    val verse = surah?.verses?.find { it.number == verseNo }
                    if (surah != null && verse != null) {
                        list.add(SavedAyah(surah, verse))
                    }
                }
            }
        }
        // sort by Surah ID and then Verse number
        list.sortedWith(compareBy({ it.surah.id }, { it.verse.number }))
    }

    var selectedAyah by remember { mutableStateOf<SavedAyah?>(null) }

    if (selectedAyah != null) {
        // Open the Ayah View Page
        Box(modifier = Modifier.fillMaxSize().background(BgLight).statusBarsPadding()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedAyah = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                    Text(
                        text = "সূরা ${selectedAyah!!.surah.nameBn} - আয়াত ${selectedAyah!!.verse.number.toBnString()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    // Reuse VerseCardItem
                    VerseCardItem(
                        surahId = selectedAyah!!.surah.id,
                        verse = selectedAyah!!.verse,
                        showArabic = true,
                        showPronunciation = true,
                        showTranslation = true,
                        arabicFontSize = 24f,
                        banglaFontSize = 16f,
                        isPlaying = false,
                        onPlayClick = {}
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgLight)
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                }
                Text(
                    text = "বুকমার্ক করা আয়াত",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (savedAyahsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো আয়াত বুকমার্ক করা নেই", color = TextGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(savedAyahsList) { savedAyah ->
                        SavedAyahListItem(
                            savedAyah = savedAyah,
                            isSaved = true,
                            onClick = { selectedAyah = savedAyah },
                            onToggleSave = {
                                val key = "${savedAyah.surah.id}_${savedAyah.verse.number}"
                                val currentlySaved = sharedPrefs.getBoolean(key, false)
                                sharedPrefs.edit().putBoolean(key, !currentlySaved).apply()
                                savedAyahKeys = sharedPrefs.all.filter { it.value == true }.keys
                                    .filter { it.matches(Regex("\\d+_\\d+")) }
                                    .toSet()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedAyahListItem(savedAyah: SavedAyah, isSaved: Boolean, onClick: () -> Unit, onToggleSave: () -> Unit) {
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
                    text = "সূরা ${savedAyah.surah.nameBn} - আয়াত ${savedAyah.verse.number.toBnString()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = savedAyah.verse.translation,
                    fontSize = 14.sp,
                    color = TextGray,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = onToggleSave,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isSaved) Color(0xFFF59E0B) else TextGray
                )
            }
        }
        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
    }
}
