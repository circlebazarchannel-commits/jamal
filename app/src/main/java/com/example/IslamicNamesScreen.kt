package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicNamesScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("boys") } // "boys" or "girls"

    val namesList = if (selectedCategory == "boys") boysNamesList else girlsNamesList
    val filteredNames = if (searchQuery.isBlank()) {
        namesList
    } else {
        namesList.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.meaning.contains(searchQuery, ignoreCase = true) 
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
            }
            Text(
                text = "ইসলামিক নাম",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Clean and Slim Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("নাম খুঁজুন...", color = Color.Gray, fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = PrimaryGreen,
                cursorColor = PrimaryGreen
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Categories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryButton(
                text = "ছেলেদের নাম",
                isSelected = selectedCategory == "boys",
                onClick = { selectedCategory = "boys" },
                modifier = Modifier.weight(1f)
            )
            CategoryButton(
                text = "মেয়েদের নাম",
                isSelected = selectedCategory == "girls",
                onClick = { selectedCategory = "girls" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredNames) { nameObj ->
                NameItemCard(nameObj)
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun CategoryButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) PrimaryGreen else Color.White
    val textColor = if (isSelected) Color.White else Color(0xFF475569)
    val borderColor = if (isSelected) PrimaryGreen else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NameItemCard(islamicName: IslamicName) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = islamicName.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = islamicName.meaning,
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )
        }
        Icon(
            imageVector = Icons.Default.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(24.dp)
        )
    }
}
