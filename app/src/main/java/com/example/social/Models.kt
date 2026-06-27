package com.example.social

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerialName
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Serializable
data class Post(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    
    // Virtual fields for mapping user profiles, likes, etc if needed in queries
    @kotlinx.serialization.Transient val likeCount: Int = 0,
    @kotlinx.serialization.Transient val commentCount: Int = 0,
    @kotlinx.serialization.Transient val shareCount: Int = 0,
    @kotlinx.serialization.Transient val isLikedByMe: Boolean = false,
    @kotlinx.serialization.Transient val userName: String = "User",
    @kotlinx.serialization.Transient val userAvatar: String? = null
)

@Serializable
data class Like(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Comment(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("comment_text") val commentText: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    
    @kotlinx.serialization.Transient val userName: String = "User",
    @kotlinx.serialization.Transient val userAvatar: String? = null
)

@Serializable
data class CommentLike(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Share(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Report(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("post_id") val postId: String,
    @SerialName("reporter_user_id") val reporterUserId: String,
    val reason: String,
    @SerialName("created_at") val createdAt: String? = null
)

object GlobalPostState {
    private val _posts = kotlinx.coroutines.flow.MutableStateFlow<List<Post>>(emptyList())
    val posts: kotlinx.coroutines.flow.StateFlow<List<Post>> = _posts

    // Reactive navigation states for the Creator Profile Page
    var selectedCreatorId by mutableStateOf<String?>(null)
    var selectedCreatorName by mutableStateOf<String>("User")
    var selectedCreatorAvatar by mutableStateOf<String?>(null)

    // Reactive states for dynamic likes and comments
    val postLikes = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    val postLikesCount = androidx.compose.runtime.mutableStateMapOf<String, Int>()
    val postComments = androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<Comment>>()
    val commentLikes = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    val commentLikesCount = androidx.compose.runtime.mutableStateMapOf<String, Int>()

    fun addPost(post: Post) {
        _posts.value = listOf(post) + _posts.value
    }
    
    fun setPosts(newPosts: List<Post>) {
        _posts.value = newPosts
    }

    // Likes management
    fun isPostLiked(context: android.content.Context, postId: String): Boolean {
        if (postId in postLikes) {
            return postLikes[postId]!!
        }
        val prefs = context.getSharedPreferences("social_prefs", android.content.Context.MODE_PRIVATE)
        val savedLiked = prefs.getBoolean("post_liked_$postId", false)
        postLikes[postId] = savedLiked
        return savedLiked
    }

    fun getLikeCount(context: android.content.Context, postId: String): Int {
        if (postId in postLikesCount) {
            return postLikesCount[postId]!!
        }
        val isLiked = isPostLiked(context, postId)
        val count = if (isLiked) 1 else 0
        postLikesCount[postId] = count
        return count
    }

    fun likePost(context: android.content.Context, postId: String, liked: Boolean) {
        postLikes[postId] = liked
        val newCount = if (liked) 1 else 0
        postLikesCount[postId] = newCount
        
        val prefs = context.getSharedPreferences("social_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("post_liked_$postId", liked)
            .putInt("post_like_count_$postId", newCount)
            .apply()
    }

    // Comments management
    fun getComments(context: android.content.Context, postId: String): androidx.compose.runtime.snapshots.SnapshotStateList<Comment> {
        if (postId in postComments) {
            return postComments[postId]!!
        }
        val prefs = context.getSharedPreferences("social_prefs", android.content.Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("post_comments_$postId", null)
        val list = if (jsonStr != null) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<Comment>>(jsonStr).toMutableList()
            } catch (e: Exception) {
                mutableListOf<Comment>()
            }
        } else {
            mutableListOf<Comment>()
        }
        
        val snapshotList = androidx.compose.runtime.mutableStateListOf<Comment>()
        snapshotList.addAll(list)
        postComments[postId] = snapshotList
        return snapshotList
    }

    fun addComment(context: android.content.Context, postId: String, comment: Comment) {
        val list = getComments(context, postId)
        list.add(comment)
        saveComments(context, postId, list)
    }

    private fun saveComments(context: android.content.Context, postId: String, list: List<Comment>) {
        val prefs = context.getSharedPreferences("social_prefs", android.content.Context.MODE_PRIVATE)
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(list)
        prefs.edit().putString("post_comments_$postId", jsonStr).apply()
    }

    // Comment Likes management
    fun isCommentLiked(context: android.content.Context, commentId: String): Boolean {
        if (commentId in commentLikes) {
            return commentLikes[commentId]!!
        }
        val prefs = context.getSharedPreferences("social_prefs", android.content.Context.MODE_PRIVATE)
        val savedLiked = prefs.getBoolean("comment_liked_$commentId", false)
        commentLikes[commentId] = savedLiked
        return savedLiked
    }

    fun getCommentLikeCount(context: android.content.Context, commentId: String): Int {
        if (commentId in commentLikesCount) {
            return commentLikesCount[commentId]!!
        }
        val prefs = context.getSharedPreferences("social_prefs", android.content.Context.MODE_PRIVATE)
        val savedCount = prefs.getInt("comment_like_count_$commentId", 0)
        commentLikesCount[commentId] = savedCount
        return savedCount
    }

    fun likeComment(context: android.content.Context, commentId: String, liked: Boolean) {
        commentLikes[commentId] = liked
        val currentCount = getCommentLikeCount(context, commentId)
        val newCount = if (liked) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
        commentLikesCount[commentId] = newCount
        
        val prefs = context.getSharedPreferences("social_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("comment_liked_$commentId", liked)
            .putInt("comment_like_count_$commentId", newCount)
            .apply()
    }
}
