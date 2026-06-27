package com.example.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import com.example.Supabase
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.viewmodel.GlobalLanguage
import io.github.jan.supabase.auth.auth
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff

@Composable
fun WhatsOnYourMindSection(onNavigateToCreatePost: () -> Unit = {}) {
    var isUserLoggedIn by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("User") }
    var currentUserAvatar by remember { mutableStateOf<String?>(null) }
    
    val auth = remember { Supabase.client.auth }

    LaunchedEffect(Unit) {
        val user = auth.currentUserOrNull()
        isUserLoggedIn = user != null
        if (user != null) {
            currentUserName = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User"
            currentUserAvatar = user.userMetadata?.get("avatar_url")?.toString()?.replace("\"", "")
        }
    }

    if (!isUserLoggedIn) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(12.dp))
            .clickable { onNavigateToCreatePost() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile Logo Display
            com.example.ProfileLogoDisplay(
                modifier = Modifier.size(40.dp),
                userId = com.example.Supabase.client.auth.currentUserOrNull()?.id ?: "",
                showBorder = true
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                "What's on your mind?", 
                color = TextGray, 
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha=0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, contentDescription = "Photo", tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Photo", color = TextDark, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoLibrary, contentDescription = "Video", tint = Color(0xFFF44336))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Video", color = TextDark, fontSize = 14.sp)
            }
        }
    }
    
    // Video Feed below What's on your mind
    VideoFeedSection()
}

@Composable
fun VideoFeedSection() {
    val globalPosts by com.example.social.GlobalPostState.posts.collectAsState()
    var fetchedPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            // First fetch from Supabase
            val posts = com.example.Supabase.client.postgrest["posts"]
                .select().decodeList<Post>()
            val profiles = try {
                com.example.Supabase.client.postgrest["profiles"]
                    .select().decodeList<com.example.model.UserProfile>()
            } catch (pe: Exception) {
                pe.printStackTrace()
                emptyList()
            }
            val profileMap = profiles.associateBy { it.id }
            val mappedPosts = posts.map { post ->
                val prof = profileMap[post.userId]
                if (prof != null && !prof.data1.isNullOrEmpty()) {
                    post.copy(userName = prof.data1)
                } else {
                    post
                }
            }
            fetchedPosts = mappedPosts.sortedByDescending { it.createdAt }
            com.example.social.GlobalPostState.setPosts(fetchedPosts)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to fetch from existing backend if available, though Supabase is preferred
        } finally {
            isLoading = false
        }
    }

    val displayPosts = globalPosts.ifEmpty { fetchedPosts }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Shorts Feed & Recent Posts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isLoading && displayPosts.isEmpty()) {
            CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (displayPosts.isEmpty()) {
            Text("No posts available right now.", color = TextGray, fontSize = 14.sp)
        } else {
            displayPosts.forEach { post ->
                VideoPostCard(post)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun VideoPostCard(
    post: Post,
    enableEditDelete: Boolean = false,
    onEdit: (Post) -> Unit = {},
    onDelete: (Post) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLiked = GlobalPostState.isPostLiked(context, post.id)
    val countLikes = GlobalPostState.getLikeCount(context, post.id)
    val commentCount = GlobalPostState.getComments(context, post.id).size
    var showCommentSheet by remember { mutableStateOf(false) }

    if (showCommentSheet) {
        VideoCommentsDialog(
            postId = post.id,
            onDismiss = { showCommentSheet = false }
        )
    }
    val sharedPrefs = remember(post.userId) { context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE) }
    val currentUserId = com.example.Supabase.client.auth.currentUserOrNull()?.id ?: "anonymous_user"
    
    val seedKey = "followers_count_seed_${post.userId}"
    if (!sharedPrefs.contains(seedKey)) {
        val randomSeed = (post.userId.hashCode().coerceAtLeast(0) % 450) + 50
        sharedPrefs.edit().putInt(seedKey, randomSeed).apply()
    }
    val baseCount = sharedPrefs.getInt(seedKey, 100)

    var isFollowing by remember(post.userId) {
        mutableStateOf(sharedPrefs.getBoolean("is_following_${post.userId}", false))
    }

    val followersCount = if (isFollowing) baseCount + 1 else baseCount
    val isEnglish = GlobalLanguage.isEnglish

    val myName = remember(currentUserId) {
        sharedPrefs.getString("user_name", "")?.takeIf { it.isNotEmpty() }
        ?: com.example.Supabase.client.auth.currentUserOrNull()?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
        ?: "User"
    }
    val displayName = if (post.userId == currentUserId) myName else post.userName

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(12.dp))
            .padding(bottom = 16.dp)
    ) {
        // User Header
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ProfileLogoDisplay(
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        GlobalPostState.selectedCreatorId = post.userId
                        GlobalPostState.selectedCreatorName = post.userName
                        GlobalPostState.selectedCreatorAvatar = post.userAvatar
                    },
                userId = post.userId,
                initialImageUrl = post.userAvatar ?: "",
                showBorder = true
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        GlobalPostState.selectedCreatorId = post.userId
                        GlobalPostState.selectedCreatorName = post.userName
                        GlobalPostState.selectedCreatorAvatar = post.userAvatar
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    
                    if (post.userId != currentUserId) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                isFollowing = !isFollowing
                                sharedPrefs.edit().putBoolean("is_following_${post.userId}", isFollowing).apply()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) Color.LightGray.copy(alpha = 0.5f) else PrimaryGreen,
                                contentColor = if (isFollowing) TextDark else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isFollowing) (if (isEnglish) "Unfollow" else "আনফলো") else (if (isEnglish) "Follow" else "ফলো"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "You" else "আপনি",
                                color = PrimaryGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$followersCount " + (if (isEnglish) "followers" else "অনুসারী") + " • Just now",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }

            if (enableEditDelete && post.userId == currentUserId) {
                var showOptionsPopup by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showOptionsPopup = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Post Options",
                            tint = TextGray
                        )
                    }
                    DropdownMenu(
                        expanded = showOptionsPopup,
                        onDismissRequest = { showOptionsPopup = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Edit" else "সম্পাদনা করুন") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryGreen) },
                            onClick = {
                                showOptionsPopup = false
                                onEdit(post)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isEnglish) "Delete" else "মুছে ফেলুন", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                showOptionsPopup = false
                                onDelete(post)
                            }
                        )
                    }
                }
            }
        }
        
        val isImage = post.mediaType == "photo" || post.mediaUrl.endsWith(".jpg", ignoreCase = true) || 
                      post.mediaUrl.endsWith(".jpeg", ignoreCase = true) ||
                      post.mediaUrl.endsWith(".png", ignoreCase = true)
                      
        if (isImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = "Post Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        } else {
            // Video Player using ExoPlayer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (post.mediaUrl.isNotEmpty()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val exoPlayer = remember {
                        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                            setMediaItem(androidx.media3.common.MediaItem.fromUri(post.mediaUrl))
                            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                            playWhenReady = true
                            prepare()
                        }
                    }
                    
                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer.release() }
                    }
                    
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            androidx.media3.ui.PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.VideoLibrary, contentDescription = "Play Video", tint = Color.White.copy(alpha=0.5f), modifier = Modifier.size(64.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Post Title/Caption
        Text(
            text = post.title, 
            color = TextDark, 
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (!post.description.isNullOrBlank()) {
             Text(
                text = post.description, 
                color = TextGray, 
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
             )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { 
                    GlobalPostState.likePost(context, post.id, !isLiked)
                }
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    contentDescription = "Like", 
                    tint = if (isLiked) Color.Red else TextDark,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(countLikes.toString(), fontWeight = FontWeight.Bold, color = TextDark)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showCommentSheet = true }
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline, 
                    contentDescription = "Comment", 
                    tint = TextDark,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(commentCount.toString(), fontWeight = FontWeight.Bold, color = TextDark)
            }
            
            Icon(
                Icons.Default.Send, 
                contentDescription = "Share", 
                tint = TextDark,
                modifier = Modifier.size(26.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.BookmarkBorder, 
                contentDescription = "Save", 
                tint = TextDark,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    
    val coroutineScope = rememberCoroutineScope()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedMediaUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Post", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
        }

        Divider(color = Color.LightGray.copy(alpha=0.5f))

        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            // Media Preview
            if (selectedMediaUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = selectedMediaUri,
                        contentDescription = "Selected Photo Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGreen.copy(alpha = 0.05f))
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { mediaPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = "Add Media", tint = PrimaryGreen, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add Photo", color = PrimaryGreen, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Title Field
            Text("Title", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter post title...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description Field
            Text("Description (Optional)", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Say something about this...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isUploading) {
                LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth().height(8.dp), color = PrimaryGreen)
                Spacer(modifier = Modifier.height(12.dp))
                if (processing) {
                    Text("Processing... After processing, your photo will be available.", color = PrimaryGreen, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("Uploading to server... ${(uploadProgress * 100).toInt()}%", color = TextGray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = { 
                        if (titleInput.isNotBlank() && selectedMediaUri != null) {
                             isUploading = true
                             coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                 try {
                                     val mimeTypeStr = context.contentResolver.getType(selectedMediaUri!!) ?: "image/jpeg"
                                     val ext = if (mimeTypeStr.startsWith("image")) "jpg" else "png"
                                     
                                     processing = false
                                     val finalUrl = com.example.network.R2Uploader.uploadFile(
                                         context = context,
                                         fileUri = selectedMediaUri!!,
                                         ext = ext,
                                         onProgress = { prog ->
                                             uploadProgress = prog
                                         }
                                     )
                                     
                                     processing = true
                                     
                                     val user = com.example.Supabase.client.auth.currentUserOrNull()
                                     val currentUserId = user?.id ?: "anonymous_user"
                                     
                                     val newPost = com.example.social.Post(
                                         userId = currentUserId,
                                         mediaType = "photo",
                                         mediaUrl = finalUrl,
                                         title = titleInput,
                                         description = descriptionInput,
                                         userName = context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE).getString("user_name", "")?.takeIf { it.isNotEmpty() } ?: (user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User")
                                     )
                                     
                                     try {
                                         com.example.Supabase.client.postgrest["posts"].insert<com.example.social.Post>(newPost)
                                     } catch(e: Exception) {
                                         e.printStackTrace()
                                     }
                                     
                                     com.example.social.GlobalPostState.addPost(newPost)
                                     
                                     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                         onNavigateBack()
                                     }
                                 } catch(e: Exception) {
                                     e.printStackTrace()
                                     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                         android.widget.Toast.makeText(context, "Upload Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                         isUploading = false
                                     }
                                 }
                             }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    enabled = !isUploading && titleInput.isNotBlank() && selectedMediaUri != null
                ) {
                    Text("Publish Post", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SocialVideosScreen(
    onBack: () -> Unit
) {
    var showMyProfile by remember { mutableStateOf(false) }

    if (showMyProfile) {
        androidx.activity.compose.BackHandler {
            showMyProfile = false
        }
        MyProfilePage(
            onBack = { showMyProfile = false }
        )
        return
    }

    val selectedCreatorId = GlobalPostState.selectedCreatorId
    if (selectedCreatorId != null) {
        androidx.activity.compose.BackHandler {
            GlobalPostState.selectedCreatorId = null
        }
        CreatorProfilePage(
            userId = selectedCreatorId,
            initialAvatarUrl = GlobalPostState.selectedCreatorAvatar,
            initialName = GlobalPostState.selectedCreatorName,
            onBack = { GlobalPostState.selectedCreatorId = null }
        )
        return
    }

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploadOpen by remember { mutableStateOf(false) }
    var showLoginScreen by remember { mutableStateOf(false) }
    var showRegisterScreen by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val isEnglish = GlobalLanguage.isEnglish

    // Function to check login before upload
    fun handleUploadClick() {
        val user = com.example.Supabase.client.auth.currentUserOrNull()
        if (user == null) {
            showLoginScreen = true
        } else {
            isUploadOpen = true
        }
    }

    // Function to fetch video posts
    fun loadVideos() {
        isLoading = true
        coroutineScope.launch {
            try {
                val fetched = com.example.Supabase.client.postgrest["posts"]
                    .select().decodeList<Post>()
                val profiles = try {
                    com.example.Supabase.client.postgrest["profiles"]
                        .select().decodeList<com.example.model.UserProfile>()
                } catch (pe: Exception) {
                    pe.printStackTrace()
                    emptyList()
                }
                val profileMap = profiles.associateBy { it.id }
                val mappedFetched = fetched.map { post ->
                    val prof = profileMap[post.userId]
                    if (prof != null && !prof.data1.isNullOrEmpty()) {
                        post.copy(userName = prof.data1)
                    } else {
                        post
                    }
                }
                // Filter only videos or mp4 urls
                posts = mappedFetched.filter { 
                    it.mediaType == "video" || 
                    (it.mediaUrl.isNotEmpty() && it.mediaUrl.contains(".mp4", ignoreCase = true))
                }.sortedByDescending { it.createdAt }
                GlobalPostState.setPosts(mappedFetched) // Sync to global
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadVideos()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Classic dark TikTok aesthetic
    ) {

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else if (posts.isEmpty()) {
            // Beautiful Empty/Onboarding view for reels
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isEnglish) "No Reels Uploaded" else "কোন ভিডিও রিলস পাওয়া যায়নি",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isEnglish) 
                        "Be the first one to upload an inspiring short Islamic video or tutorial!" 
                    else 
                        "প্রথম ইসলামিক শর্ট বা অনুপ্রেরণামূলক ভিডিও আপলোড করুন!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { handleUploadClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEnglish) "Upload Reel" else "ভিডিও আপলোড করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Vertical Pager for reels
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { posts.size }
            )

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val post = posts[page]
                val isPageActive = pagerState.currentPage == page
                
                val context = androidx.compose.ui.platform.LocalContext.current
                val sharedPrefs = remember(post.userId) { context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE) }
                val currentUserId = com.example.Supabase.client.auth.currentUserOrNull()?.id ?: "anonymous_user"
                
                val seedKey = "followers_count_seed_${post.userId}"
                if (!sharedPrefs.contains(seedKey)) {
                    val randomSeed = (post.userId.hashCode().coerceAtLeast(0) % 450) + 50
                    sharedPrefs.edit().putInt(seedKey, randomSeed).apply()
                }
                val baseCount = sharedPrefs.getInt(seedKey, 100)

                var isFollowing by remember(post.userId) {
                    mutableStateOf(sharedPrefs.getBoolean("is_following_${post.userId}", false))
                }

                val followersCount = if (isFollowing) baseCount + 1 else baseCount
                
                val myName = remember(currentUserId) {
                    sharedPrefs.getString("user_name", "")?.takeIf { it.isNotEmpty() }
                    ?: com.example.Supabase.client.auth.currentUserOrNull()?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
                    ?: "User"
                }
                val displayName = if (post.userId == currentUserId) myName else post.userName

                Box(modifier = Modifier.fillMaxSize()) {
                    FullScreenVideoPlayer(
                        videoUrl = post.mediaUrl,
                        isActive = isPageActive
                    )

                    // Overlay bottom gradient for text contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )

                    // Right-side actions (Likes, Comments, Share)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Likes
                        val localContextForLikes = androidx.compose.ui.platform.LocalContext.current
                        val likedByMe = GlobalPostState.isPostLiked(localContextForLikes, post.id)
                        val countLikes = GlobalPostState.getLikeCount(localContextForLikes, post.id)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    GlobalPostState.likePost(localContextForLikes, post.id, !likedByMe)
                                },
                                modifier = Modifier
                                    .size(45.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (likedByMe) Color.Red else Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(countLikes.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Comments Button
                        var showCommentSheet by remember { mutableStateOf(false) }
                        val commentCount = GlobalPostState.getComments(localContextForLikes, post.id).size
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { showCommentSheet = true },
                                modifier = Modifier
                                    .size(45.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Comments",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(commentCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (showCommentSheet) {
                            VideoCommentsDialog(
                                postId = post.id,
                                onDismiss = { showCommentSheet = false }
                            )
                        }

                        // Save Button
                        IconButton(
                            onClick = {
                                android.widget.Toast.makeText(localContextForLikes, if (isEnglish) "Saved!" else "সংরক্ষণ করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(45.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Save",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Share Button
                        val localContext = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = {
                                android.widget.Toast.makeText(localContext, if (isEnglish) "Link copied!" else "লিঙ্ক কপি হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(45.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Left-side metadata (Creator, title, description)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 20.dp, end = 80.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.ProfileLogoDisplay(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        GlobalPostState.selectedCreatorId = post.userId
                                        GlobalPostState.selectedCreatorName = post.userName
                                        GlobalPostState.selectedCreatorAvatar = post.userAvatar
                                    },
                                userId = post.userId,
                                initialImageUrl = post.userAvatar ?: "",
                                showBorder = true
                            )

                            Column(
                                modifier = Modifier.clickable {
                                    GlobalPostState.selectedCreatorId = post.userId
                                    GlobalPostState.selectedCreatorName = post.userName
                                    GlobalPostState.selectedCreatorAvatar = post.userAvatar
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "@$displayName",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )

                                    if (post.userId != currentUserId) {
                                        Button(
                                            onClick = {
                                                isFollowing = !isFollowing
                                                sharedPrefs.edit().putBoolean("is_following_${post.userId}", isFollowing).apply()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(24.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isFollowing) Color.White.copy(alpha = 0.3f) else PrimaryGreen,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = if (isFollowing) (if (isEnglish) "Unfollow" else "আনফলো") else (if (isEnglish) "Follow" else "ফলো"),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isEnglish) "You" else "আপনি",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "$followersCount " + (if (isEnglish) "followers" else "অনুসারী"),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = post.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!post.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = post.description,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Top Header Row with Camera Icon and Back Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 2.dp), // Move further up!
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isEnglish) "Following" else "ফলোয়িং",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha = 0.5f), CircleShape))
                Text(
                    text = if (isEnglish) "For you" else "আপনার জন্য",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            IconButton(
                onClick = { handleUploadClick() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Upload Video", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // Overlay Upload Video Screen
        androidx.compose.animation.AnimatedVisibility(
            visible = isUploadOpen,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            UploadVideoScreen(
                onNavigateBack = {
                    isUploadOpen = false
                    loadVideos() // Refresh
                }
            )
        }

        if (showLoginScreen) {
            com.example.LoginScreen(
                onBack = { showLoginScreen = false },
                onNavigateToRegister = {
                    showLoginScreen = false
                    showRegisterScreen = true
                },
                onLoginSuccess = {
                    showLoginScreen = false
                    isUploadOpen = true
                }
            )
        }

        if (showRegisterScreen) {
            com.example.RegisterScreen(
                onBack = { showRegisterScreen = false },
                onNavigateToLogin = {
                    showRegisterScreen = false
                    showLoginScreen = true
                },
                onRegisterSuccess = {
                    showRegisterScreen = false
                    isUploadOpen = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCommentsDialog(
    postId: String,
    onDismiss: () -> Unit
) {
    val isEnglish = GlobalLanguage.isEnglish
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUserId = com.example.Supabase.client.auth.currentUserOrNull()?.id ?: "anonymous_user"
    val sharedPrefs = remember(currentUserId) { context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE) }
    
    val myName = remember(currentUserId) {
        sharedPrefs.getString("user_name", "")?.takeIf { it.isNotEmpty() }
            ?: com.example.Supabase.client.auth.currentUserOrNull()?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
            ?: "User"
    }
    val myAvatar = remember(currentUserId) {
        sharedPrefs.getString("profile_image_url", "")?.takeIf { it.isNotEmpty() }
    }

    // Get reactive comments list
    val allComments = GlobalPostState.getComments(context, postId)
    
    // Separate root comments and replies
    val rootComments = allComments.filter { it.parentCommentId == null }
    
    var commentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<Comment?>(null) }
    var editingComment by remember { mutableStateOf<Comment?>(null) }
    var editCommentText by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White // Entirely white background
        ) {
            Scaffold(
                containerColor = Color.White,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (isEnglish) "Comments" else "মন্তব্যসমূহ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = if (isEnglish) "${allComments.size} comments" else "${allComments.size} টি মন্তব্য",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextDark
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(bottom = 24.dp) // Lift up from the bottom of the mobile screen
                        ) {
                            // Replying to banner
                            replyingToComment?.let { replyTarget ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PrimaryGreen.copy(alpha = 0.08f))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isEnglish) "Replying to @${replyTarget.userName}" else "@${replyTarget.userName}-এর উত্তরের জন্য",
                                        fontSize = 12.sp,
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = { replyingToComment = null },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel reply",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Input Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.ProfileLogoDisplay(
                                    modifier = Modifier.size(36.dp),
                                    userId = currentUserId,
                                    initialImageUrl = myAvatar ?: "",
                                    showBorder = true
                                )
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text(
                                            text = if (replyingToComment != null) {
                                                if (isEnglish) "Write a reply..." else "একটি উত্তর লিখুন..."
                                            } else {
                                                if (isEnglish) "Add a comment..." else "একটি মন্তব্য যোগ করুন..."
                                            },
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    },
                                    maxLines = 4,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF9FAFB),
                                        unfocusedContainerColor = Color(0xFFF9FAFB),
                                        focusedBorderColor = PrimaryGreen,
                                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.7f)
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            val newComment = Comment(
                                                postId = postId,
                                                userId = currentUserId,
                                                commentText = commentText.trim(),
                                                parentCommentId = replyingToComment?.id,
                                                userName = myName,
                                                userAvatar = myAvatar,
                                                createdAt = if (isEnglish) "Just now" else "এইমাত্র"
                                            )
                                            GlobalPostState.addComment(context, postId, newComment)
                                            commentText = ""
                                            replyingToComment = null
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = PrimaryGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                if (rootComments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "No comments",
                                tint = Color.LightGray,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isEnglish) "No Comments Yet" else "এখনো কোনো মন্তব্য নেই",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEnglish) "Be the first to share your thoughts!" else "সবার আগে আপনার সুন্দর মন্তব্যটি এখানে পেশ করুন!",
                                fontSize = 13.sp,
                                color = TextGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
                    ) {
                        items(rootComments) { comment ->
                            val replies = allComments.filter { it.parentCommentId == comment.id }
                            val isLiked = GlobalPostState.isCommentLiked(context, comment.id)
                            val likeCount = GlobalPostState.getCommentLikeCount(context, comment.id)
                            
                            CommentCard(
                                comment = comment,
                                replies = replies,
                                isEnglish = isEnglish,
                                currentUserId = currentUserId,
                                onLikeClick = {
                                    GlobalPostState.likeComment(context, comment.id, !isLiked)
                                },
                                onReplyClick = {
                                    replyingToComment = comment
                                },
                                onEdit = {
                                    editingComment = comment
                                    editCommentText = comment.commentText
                                },
                                onDelete = {
                                    GlobalPostState.deleteComment(context, postId, comment.id)
                                },
                                onChildEdit = { child ->
                                    editingComment = child
                                    editCommentText = child.commentText
                                },
                                onChildDelete = { child ->
                                    GlobalPostState.deleteComment(context, postId, child.id)
                                },
                                isCommentLiked = isLiked,
                                commentLikeCount = likeCount,
                                isChildCommentLiked = { child ->
                                    GlobalPostState.isCommentLiked(context, child.id)
                                },
                                getChildCommentLikeCount = { child ->
                                    GlobalPostState.getCommentLikeCount(context, child.id)
                                },
                                onChildLikeClick = { child ->
                                    val childLiked = GlobalPostState.isCommentLiked(context, child.id)
                                    GlobalPostState.likeComment(context, child.id, !childLiked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Comment Dialog
    if (editingComment != null) {
        AlertDialog(
            onDismissRequest = { editingComment = null },
            title = { Text(if (isEnglish) "Edit Comment" else "মন্তব্য সম্পাদনা") },
            text = {
                OutlinedTextField(
                    value = editCommentText,
                    onValueChange = { editCommentText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (isEnglish) "Write something..." else "কিছু লিখুন...") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingComment?.let {
                            GlobalPostState.updateComment(context, postId, it.id, editCommentText)
                        }
                        editingComment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(if (isEnglish) "Update" else "আপডেট")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingComment = null }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }
}

@Composable
fun CommentCard(
    comment: Comment,
    replies: List<Comment>,
    isEnglish: Boolean,
    currentUserId: String,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChildEdit: (Comment) -> Unit,
    onChildDelete: (Comment) -> Unit,
    onChildLikeClick: (Comment) -> Unit,
    isCommentLiked: Boolean,
    commentLikeCount: Int,
    isChildCommentLiked: (Comment) -> Boolean,
    getChildCommentLikeCount: (Comment) -> Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ProfileLogoDisplay(
                        modifier = Modifier.size(36.dp),
                        userId = comment.userId,
                        initialImageUrl = comment.userAvatar ?: "",
                        showBorder = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = comment.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                        Text(
                            text = comment.createdAt ?: (if (isEnglish) "Just now" else "এইমাত্র"),
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                if (comment.userId == currentUserId) {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextGray)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Edit Comment" else "মন্তব্য সম্পাদনা") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isEnglish) "Delete Comment" else "মন্তব্য মুছে ফেলুন") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Text Content
            Text(
                text = comment.commentText,
                fontSize = 14.sp,
                color = TextDark,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Like Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isCommentLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isCommentLiked) Color.Red else TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (commentLikeCount > 0) commentLikeCount.toString() else (if (isEnglish) "Like" else "লাইক"),
                        fontSize = 12.sp,
                        color = if (isCommentLiked) Color.Red else TextGray,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Reply Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReplyClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "Reply",
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEnglish) "Reply" else "উত্তর",
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Nested Replies
            if (replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(start = 12.dp) // Indentation for thread look
                ) {
                    replies.forEach { reply ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            com.example.ProfileLogoDisplay(
                                modifier = Modifier.size(28.dp),
                                userId = reply.userId,
                                initialImageUrl = reply.userAvatar ?: "",
                                showBorder = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = reply.userName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            text = reply.createdAt ?: (if (isEnglish) "Just now" else "এইমাত্র"),
                                            fontSize = 10.sp,
                                            color = TextGray
                                        )
                                    }
                                    
                                    // Reply Actions
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Reply Like Button
                                        val isReplyLiked = isChildCommentLiked(reply)
                                        val replyLikeCount = getChildCommentLikeCount(reply)
                                        IconButton(
                                            onClick = { onChildLikeClick(reply) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isReplyLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Like Reply",
                                                tint = if (isReplyLiked) Color.Red else TextGray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        if (reply.userId == currentUserId) {
                                            var showReplyMenu by remember { mutableStateOf(false) }
                                            Box {
                                                IconButton(
                                                    onClick = { showReplyMenu = true },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.MoreVert,
                                                        contentDescription = "Menu",
                                                        tint = TextGray,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showReplyMenu,
                                                    onDismissRequest = { showReplyMenu = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text(if (isEnglish) "Edit Comment" else "মন্তব্য সম্পাদনা") },
                                                        onClick = {
                                                            showReplyMenu = false
                                                            onChildEdit(reply)
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(if (isEnglish) "Delete Comment" else "মন্তব্য মুছে ফেলুন") },
                                                        onClick = {
                                                            showReplyMenu = false
                                                            onChildDelete(reply)
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp)) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reply.commentText,
                                    fontSize = 13.sp,
                                    color = TextDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenVideoPlayer(
    videoUrl: String,
    isActive: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isMuted by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUrl) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUrl))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            playWhenReady = isActive
            prepare()
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                isMuted = !isMuted
                exoPlayer.volume = if (isMuted) 0f else 1f
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Mute indicator overlay
        if (isMuted) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VolumeOff, contentDescription = "Muted", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun UploadVideoScreen(
    onNavigateBack: () -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    
    val coroutineScope = rememberCoroutineScope()
    val isEnglish = GlobalLanguage.isEnglish

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedVideoUri = uri }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isEnglish) "Upload Short Reel" else "শর্ট রিলস আপলোড",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
            }

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Card grouping Title and Video Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Media Preview inside Card
                    if (selectedVideoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(1.dp, PrimaryGreen, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (isEnglish) "Video Selected Successfully!" else "ভিডিও সফলভাবে নির্বাচন করা হয়েছে!", 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                                    Text(if (isEnglish) "Change Video" else "ভিডিও পরিবর্তন করুন", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryGreen.copy(alpha = 0.05f))
                                .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { videoPickerLauncher.launch("video/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Add Video", tint = PrimaryGreen, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (isEnglish) "Select Video from Gallery" else "গ্যালারি থেকে ভিডিও সিলেক্ট করুন", 
                                    color = TextDark, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Supports short .mp4 videos", color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title Field inside Card
                    Text(if (isEnglish) "Title" else "শিরোনাম", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isEnglish) "Enter a catchy title..." else "শিরোনাম লিখুন...", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isUploading) {
                LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth().height(8.dp), color = PrimaryGreen, trackColor = Color.LightGray)
                Spacer(modifier = Modifier.height(12.dp))
                if (processing) {
                    Text(if (isEnglish) "Processing... Publishing your reel." else "প্রসেসিং করা হচ্ছে... ভিডিও পাবলিশ হচ্ছে।", color = PrimaryGreen, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(if (isEnglish) "Uploading: ${(uploadProgress * 100).toInt()}%" else "আপলোড হচ্ছে: ${(uploadProgress * 100).toInt()}%", color = TextGray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Pinned Post Button at the very bottom
        if (!isUploading) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = { 
                    if (titleInput.isNotBlank() && selectedVideoUri != null) {
                         isUploading = true
                         coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                              try {
                                  processing = false
                                  // Video Upload to R2 (mp4 format)
                                  val finalUrl = com.example.network.R2Uploader.uploadFile(
                                      context = context,
                                      fileUri = selectedVideoUri!!,
                                      ext = "mp4",
                                      onProgress = { prog ->
                                          uploadProgress = prog
                                      }
                                  )
                                  
                                  processing = true
                                  
                                  val user = com.example.Supabase.client.auth.currentUserOrNull()
                                  val currentUserId = user?.id ?: "anonymous_user"
                                  
                                  val newPost = Post(
                                      userId = currentUserId,
                                      mediaType = "video",
                                      mediaUrl = finalUrl,
                                      title = titleInput,
                                      description = "", // No description box present
                                      userName = context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE).getString("user_name", "")?.takeIf { it.isNotEmpty() } ?: (user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User")
                                  )
                                  
                                  try {
                                      com.example.Supabase.client.postgrest["posts"].insert<Post>(newPost)
                                  } catch(e: Exception) {
                                      e.printStackTrace()
                                  }
                                  
                                  GlobalPostState.addPost(newPost)
                                  
                                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                      onNavigateBack()
                                  }
                              } catch(e: Exception) {
                                  e.printStackTrace()
                                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                      android.widget.Toast.makeText(context, "Upload Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                      isUploading = false
                                  }
                              }
                         }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                enabled = titleInput.isNotBlank() && selectedVideoUri != null
            ) {
                Text(if (isEnglish) "Publish Video" else "ভিডিও পাবলিশ করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
}

