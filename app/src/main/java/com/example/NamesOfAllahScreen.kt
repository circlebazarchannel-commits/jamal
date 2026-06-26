package com.example

import com.example.viewmodel.GlobalLanguage
import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesOfAllahScreen(onBack: () -> Unit) {
    val isEng = GlobalLanguage.isEnglish

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
                    text = if (isEng) "99 Names of Allah" else "আল্লাহর ৯৯ নাম",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allahNamesList) { name ->
                AllahNamePremiumCard(name = name)
            }
        }
    }
}

@Composable
fun AllahNamePremiumCard(name: AllahName) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${name.id}",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.transliteration,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name.meaning,
                    fontSize = 13.sp,
                    color = TextGray,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Arabic Text
            Text(
                text = name.arabic,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = PrimaryGreen,
                textAlign = TextAlign.End
            )
        }
    }
}
