package com.example

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
import com.example.data.DuaData
import com.example.data.DuaStorage
import com.example.model.Dua
import com.example.ui.theme.BgLight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDuasScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val duaStorage = remember { DuaStorage(context) }
    
    // We use a State to recompose when a dua is unsaved
    var savedDuaIds by remember { mutableStateOf(duaStorage.getSavedDuaIds()) }
    var selectedDua by remember { mutableStateOf<Dua?>(null) }

    val allDuas = DuaData.duaList
    val savedDuas = allDuas.filter { savedDuaIds.contains(it.id.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সেভ করা দোয়া",
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
            if (savedDuas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো দোয়া সেভ করা নেই", color = TextGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(savedDuas) { dua ->
                        SavedDuaListItem(
                            dua = dua,
                            isSaved = true,
                            onClick = { selectedDua = dua },
                            onToggleSave = {
                                duaStorage.toggleSavedDua(dua.id)
                                savedDuaIds = duaStorage.getSavedDuaIds()
                            }
                        )
                    }
                }
            }

            // Full Screen Dua Detail
            selectedDua?.let { dua ->
                DuaDetailDialog(dua = dua, onDismiss = { selectedDua = null })
            }
        }
    }
}

@Composable
fun SavedDuaListItem(dua: Dua, isSaved: Boolean, onClick: () -> Unit, onToggleSave: () -> Unit) {
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
                    text = dua.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )
                Text(
                    text = dua.category,
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
                    contentDescription = "Bookmark",
                    tint = if (isSaved) PrimaryGreen else TextGray
                )
            }
        }
        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
    }
}
