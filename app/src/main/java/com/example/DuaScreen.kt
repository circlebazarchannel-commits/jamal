package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.DuaData
import com.example.model.Dua
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDua by remember { mutableStateOf<Dua?>(null) }

    val allDuas = DuaData.duaList
    val filteredDuas = if (searchQuery.isEmpty()) {
        allDuas
    } else {
        allDuas.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedDuas = filteredDuas.groupBy { it.category }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "দোয়া",
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
                // Search Bar
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("দোয়া খুঁজুন...", color = TextGray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = PrimaryGreen
                        ),
                        singleLine = true
                    )
                }
            }
        },
        containerColor = BgLight
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedDuas.forEach { (category, duas) ->
                    // Category Header - User said "সবুজ কালারের লেখাগুলো থাকবে না"
                    // We remove it entirely or change color. Let's try removing it for a cleaner look as they said "deleted".
                    // But if they want a flat list, we just iterate.
                
                    items(duas) { dua ->
                        DuaListItem(dua = dua, onClick = { selectedDua = dua })
                    }
                }
                
                if (filteredDuas.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("কোনো দোয়া পাওয়া যায়নি", color = TextGray)
                        }
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
fun DuaListItem(dua: Dua, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val duaStorage = remember { com.example.data.DuaStorage(context) }
    var bookmarked by remember { mutableStateOf(duaStorage.isDuaSaved(dua.id)) }

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
                // Small indicator of sub-title or first few words of Arabic
                Text(
                    text = dua.category,
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
            IconButton(
                onClick = {
                    duaStorage.toggleSavedDua(dua.id)
                    bookmarked = duaStorage.isDuaSaved(dua.id)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (bookmarked) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                    contentDescription = "Bookmark",
                    tint = if (bookmarked) PrimaryGreen else TextGray
                )
            }
        }
        Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)
    }
}

@Composable
fun DuaDetailDialog(dua: Dua, onDismiss: () -> Unit) {
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
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                }
                Text(
                    text = dua.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(start = 8.dp)
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
                    text = dua.arabic,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 48.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Pronunciation Card
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
                            text = dua.pronunciation,
                            fontSize = 16.sp,
                            color = TextDark,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Translation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), // Very light green tint
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "অর্থ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dua.translation,
                            fontSize = 16.sp,
                            color = TextDark,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Reference
                Text(
                    text = "সূত্র: ${dua.reference}",
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
