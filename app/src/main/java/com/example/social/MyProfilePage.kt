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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
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
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch

@Serializable
data class PostUpdate(
    val title: String,
    val description: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfilePage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = Supabase.client.auth.currentUserOrNull()?.id ?: "anonymous_user"
    val sharedPrefs = remember(currentUserId) { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    val isEnglish = GlobalLanguage.isEnglish
    val coroutineScope = rememberCoroutineScope()

    // Followers and Following logic
    val seedKey = "followers_count_seed_$currentUserId"
    if (!sharedPrefs.contains(seedKey)) {
        sharedPrefs.edit().putInt(seedKey, 142).apply()
    }
    val followersCount = sharedPrefs.getInt(seedKey, 142)

    val followingSeedKey = "following_count_seed_$currentUserId"
    if (!sharedPrefs.contains(followingSeedKey)) {
        sharedPrefs.edit().putInt(followingSeedKey, 89).apply()
    }
    val followingCount = sharedPrefs.getInt(followingSeedKey, 89)

    val myName = remember(currentUserId) {
        sharedPrefs.getString("user_name", "")?.takeIf { it.isNotEmpty() }
            ?: Supabase.client.auth.currentUserOrNull()?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
            ?: "User"
    }
    val myAvatar = remember(currentUserId) {
        sharedPrefs.getString("profile_image_url", "")?.takeIf { it.isNotEmpty() }
    }

    // Collect global posts list and filter only MY posts
    val allPosts by GlobalPostState.posts.collectAsState()
    val myPosts = remember(allPosts) {
        allPosts.filter { it.userId == currentUserId }
    }

    // Edit state
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var deleteConfirmPost by remember { mutableStateOf<Post?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileLogoDisplay(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            userId = currentUserId,
                            initialImageUrl = myAvatar ?: "",
                            showBorder = true
                        )
                        Text(
                            text = if (isEnglish) "Profile" else "প্রোফাইল",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 18.sp
                        )
                    }
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
            // 1. My Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(115.dp),
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
                        ProfileLogoDisplay(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            userId = currentUserId,
                            initialImageUrl = myAvatar ?: "",
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
                                    text = myName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextDark,
                                    maxLines = 1
                                )

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

                            Spacer(modifier = Modifier.height(6.dp))

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
                    text = if (isEnglish) "My Reels & Videos" else "আমার রিলস ও ভিডিওসমূহ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // 3. Videos
            if (myPosts.isEmpty()) {
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
                            text = if (isEnglish) "You haven't posted any videos yet" else "আপনি কোনো ভিডিও এখনো পোস্ট করেননি",
                            color = TextGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(myPosts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        VideoPostCard(
                            post = post,
                            enableEditDelete = true,
                            onEdit = { editingPost = it },
                            onDelete = { deleteConfirmPost = it }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    deleteConfirmPost?.let { post ->
        AlertDialog(
            onDismissRequest = { deleteConfirmPost = null },
            title = {
                Text(
                    text = if (isEnglish) "Delete Post" else "পোস্ট মুছে ফেলুন",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isEnglish) 
                        "Are you sure you want to permanently delete this post?" 
                    else 
                        "আপনি কি নিশ্চিত যে আপনি এই পোস্টটি চিরতরে মুছে ফেলতে চান?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPost = post
                        deleteConfirmPost = null
                        coroutineScope.launch {
                            try {
                                Supabase.client.postgrest["posts"].delete {
                                    filter {
                                        eq("id", targetPost.id)
                                    }
                                }
                                // Update local state
                                val updatedList = allPosts.filter { it.id != targetPost.id }
                                GlobalPostState.setPosts(updatedList)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (isEnglish) "Delete" else "মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPost = null }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // Edit Post Dialog
    editingPost?.let { post ->
        var editTitle by remember { mutableStateOf(post.title) }
        var editDesc by remember { mutableStateOf(post.description ?: "") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingPost = null },
            title = {
                Text(
                    text = if (isEnglish) "Edit Post" else "পোস্ট সম্পাদনা",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(if (isEnglish) "Title" else "শিরোনাম") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text(if (isEnglish) "Description" else "বর্ণনা") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPost = post
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                Supabase.client.postgrest["posts"].update(PostUpdate(editTitle.trim(), editDesc.trim())) {
                                    filter {
                                        eq("id", targetPost.id)
                                    }
                                }
                                // Update local state
                                val updatedList = allPosts.map {
                                    if (it.id == targetPost.id) {
                                        it.copy(title = editTitle.trim(), description = editDesc.trim())
                                    } else {
                                        it
                                    }
                                }
                                GlobalPostState.setPosts(updatedList)
                                editingPost = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    enabled = !isSaving && editTitle.isNotBlank()
                ) {
                    Text(if (isEnglish) "Save" else "সংরক্ষণ করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPost = null }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }
}
