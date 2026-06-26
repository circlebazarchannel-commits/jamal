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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            // Profile Logo Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(currentUserName.take(1).uppercase(), color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            
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
            fetchedPosts = posts.sortedByDescending { it.createdAt }
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
fun VideoPostCard(post: Post) {
    var isLiked by remember { mutableStateOf(post.isLikedByMe) }
    var isSubscribed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(post.userName.firstOrNull()?.toString()?.uppercase() ?: "U", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(post.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text("Just now", fontSize = 12.sp, color = TextGray)
            }
            TextButton(
                onClick = { isSubscribed = !isSubscribed },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isSubscribed) TextGray else PrimaryGreen
                )
            ) {
                Text(if (isSubscribed) "Subscribed" else "Subscribe", fontWeight = FontWeight.Bold)
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                isLiked = !isLiked 
                // In full implementation, save to 'likes' table
            }) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    contentDescription = "Like", 
                    tint = if (isLiked) Color.Red else TextDark,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isLiked) "1" else "0", fontWeight = FontWeight.Bold, color = TextDark)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ChatBubbleOutline, 
                    contentDescription = "Comment", 
                    tint = TextDark,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("0", fontWeight = FontWeight.Bold, color = TextDark)
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
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
                                         userName = user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User"
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
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploadOpen by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val isEnglish = GlobalLanguage.isEnglish

    // Function to fetch video posts
    fun loadVideos() {
        isLoading = true
        coroutineScope.launch {
            try {
                val fetched = com.example.Supabase.client.postgrest["posts"]
                    .select().decodeList<Post>()
                // Filter only videos or mp4 urls
                posts = fetched.filter { 
                    it.mediaType == "video" || 
                    (it.mediaUrl.isNotEmpty() && it.mediaUrl.contains(".mp4", ignoreCase = true))
                }.sortedByDescending { it.createdAt }
                GlobalPostState.setPosts(fetched) // Sync to global
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
                    onClick = { isUploadOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEnglish) "Upload Reel" else "ভিডিও আপলোড করুন", fontWeight = FontWeight.Bold)
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
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp, bottom = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Profile
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.8f))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        // Likes
                        var likedByMe by remember(post.id) { mutableStateOf(post.isLikedByMe) }
                        var countLikes by remember(post.id) { mutableStateOf(12 + (post.id.hashCode() % 45).coerceAtLeast(0)) }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    likedByMe = !likedByMe
                                    if (likedByMe) countLikes++ else countLikes--
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
                            Text("5", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (showCommentSheet) {
                            VideoCommentsDialog(
                                postId = post.id,
                                onDismiss = { showCommentSheet = false }
                            )
                        }

                        // Share Button
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = {
                                android.widget.Toast.makeText(context, if (isEnglish) "Link copied!" else "লিঙ্ক কপি হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
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
                            .padding(start = 16.dp, bottom = 48.dp, end = 80.dp)
                    ) {
                        Text(
                            text = "@${post.userName}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = if (isEnglish) "Short Reels" else "শর্ট রিলস",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            IconButton(
                onClick = { isUploadOpen = true },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = "Upload Video", tint = Color.White)
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
    }
}

@Composable
fun VideoCommentsDialog(
    postId: String,
    onDismiss: () -> Unit
) {
    val isEnglish = GlobalLanguage.isEnglish
    var commentText by remember { mutableStateOf("") }
    val fakeComments = remember {
        mutableStateListOf(
            "মাশাআল্লাহ ভাই, দারুণ হয়েছে!",
            "সুবহানআল্লাহ! অনেক সুন্দর ভিডিও।",
            "জাজাকাল্লাহ খাইরান শেয়ার করার জন্য।",
            "খুব চমৎকার উপস্থাপন!",
            "আল্লাহ আমাদের আমল করার তৌফিক দিন।"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEnglish) "Comments" else "মন্তব্যসমূহ", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(fakeComments) { comment ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("U", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(if (isEnglish) "User" else "ব্যবহারকারী", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                Text(comment, fontSize = 13.sp, color = TextDark)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (isEnglish) "Write a comment..." else "মন্তব্য লিখুন...", color = Color.Gray) },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                fakeComments.add(commentText)
                                commentText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Close" else "বন্ধ করুন", color = PrimaryGreen)
            }
        }
    )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Sleek dark upload screen
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isEnglish) "Upload Short Reel" else "শর্ট রিলস আপলোড",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
        }

        Divider(color = Color.White.copy(alpha = 0.1f))

        Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            // Media Preview Area
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
                        Text(if (isEnglish) "Video Selected Successfully!" else "ভিডিও সফলভাবে নির্বাচন করা হয়েছে!", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                            Text(if (isEnglish) "Change Video" else "ভিডিও পরিবর্তন করুন", color = PrimaryGreen)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGreen.copy(alpha = 0.08f))
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { videoPickerLauncher.launch("video/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Videocam, contentDescription = "Add Video", tint = PrimaryGreen, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(if (isEnglish) "Select Video from Gallery" else "গ্যালারি থেকে ভিডিও সিলেক্ট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Supports short .mp4 videos", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Title Field
            Text(if (isEnglish) "Title" else "শিরোনাম", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (isEnglish) "Enter a catchy title..." else "শিরোনাম লিখুন...", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description Field
            Text(if (isEnglish) "Description" else "বর্ণনা (ঐচ্ছিক)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                placeholder = { Text(if (isEnglish) "Tell more about this video..." else "ভিডিও সম্পর্কে বর্ণনা লিখুন...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isUploading) {
                LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth().height(8.dp), color = PrimaryGreen, trackColor = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                if (processing) {
                    Text(if (isEnglish) "Processing... Publishing your reel." else "প্রসেসিং করা হচ্ছে... ভিডিও পাবলিশ হচ্ছে।", color = PrimaryGreen, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(if (isEnglish) "Uploading: ${(uploadProgress * 100).toInt()}%" else "আপলোড হচ্ছে: ${(uploadProgress * 100).toInt()}%", color = Color.White, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else {
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
                                         description = descriptionInput,
                                         userName = user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User"
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
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    enabled = !isUploading && titleInput.isNotBlank() && selectedVideoUri != null
                ) {
                    Text(if (isEnglish) "Publish Video" else "ভিডিও পাবলিশ করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

