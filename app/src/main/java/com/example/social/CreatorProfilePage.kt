package com.example.social

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ProfileLogoDisplay
import com.example.Supabase
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.viewmodel.GlobalLanguage
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfilePage(
    userId: String,
    initialAvatarUrl: String? = null,
    initialName: String = "User",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember(userId) { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    val currentUserId = Supabase.client.auth.currentUserOrNull()?.id ?: "anonymous_user"
    val isEnglish = GlobalLanguage.isEnglish

    // 1. Follower state seed logic
    val seedKey = "followers_count_seed_$userId"
    if (!sharedPrefs.contains(seedKey)) {
        val randomSeed = (userId.hashCode().coerceAtLeast(0) % 450) + 50
        sharedPrefs.edit().putInt(seedKey, randomSeed).apply()
    }
    val baseCount = sharedPrefs.getInt(seedKey, 100)

    // 2. Following state seed logic
    val followingSeedKey = "following_count_seed_$userId"
    if (!sharedPrefs.contains(followingSeedKey)) {
        val randomSeed = (userId.hashCode().coerceAtLeast(0) % 150) + 20
        sharedPrefs.edit().putInt(followingSeedKey, randomSeed).apply()
    }
    val followingCount = sharedPrefs.getInt(followingSeedKey, 50)

    var isFollowing by remember(userId) {
        mutableStateOf(sharedPrefs.getBoolean("is_following_$userId", false))
    }

    val followersCount = if (isFollowing) baseCount + 1 else baseCount

    // Dynamic name logic for current user
    val myName = remember(currentUserId) {
        sharedPrefs.getString("user_name", "")?.takeIf { it.isNotEmpty() }
            ?: Supabase.client.auth.currentUserOrNull()?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
            ?: "User"
    }
    val displayName = if (userId == currentUserId) myName else initialName

    // Collect global posts list and filter for this creator
    val allPosts by GlobalPostState.posts.collectAsState()
    val creatorPosts = remember(allPosts, userId) {
        allPosts.filter { it.userId == userId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) "Creator Profile" else "নির্মাতা প্রোফাইল",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F6)) // Facebook-style grayish light background
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Creator Profile Card (approx 1 inch size ~ 110dp)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(115.dp), // One inch is ~100-115dp depending on screen density
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Logo Display with overlay badge
                        ProfileLogoDisplay(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            userId = userId,
                            initialImageUrl = initialAvatarUrl ?: "",
                            showBorder = true
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextDark,
                                    maxLines = 1
                                )

                                // Follow / Unfollow button if not current user
                                if (userId != currentUserId) {
                                    Button(
                                        onClick = {
                                            isFollowing = !isFollowing
                                            sharedPrefs.edit().putBoolean("is_following_$userId", isFollowing).apply()
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFollowing) Color.LightGray.copy(alpha = 0.5f) else PrimaryGreen,
                                            contentColor = if (isFollowing) TextDark else Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(
                                            text = if (isFollowing) (if (isEnglish) "Unfollow" else "আনফলো") else (if (isEnglish) "Follow" else "ফলো"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "You" else "আপনি",
                                            color = PrimaryGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Followers and Following Count Side-by-Side
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$followersCount " + (if (isEnglish) "followers" else "অনুসারী"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextGray
                                )
                                Text(
                                    text = "•",
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$followingCount " + (if (isEnglish) "following" else "অনুসরণকারী"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }

            // 2. Video Posts header
            item {
                Text(
                    text = if (isEnglish) "Creator's Reels & Videos" else "নির্মাতার রিলস ও ভিডিওসমূহ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // 3. Creator's Videos in Facebook-style Layout
            if (creatorPosts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isEnglish) "No videos posted yet" else "কোনো ভিডিও এখনো পোস্ট করা হয়নি",
                            color = TextGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(creatorPosts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        VideoPostCard(post = post)
                    }
                }
            }
        }
    }
}
