package com.example.social

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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

    fun addPost(post: Post) {
        _posts.value = listOf(post) + _posts.value
    }
    
    fun setPosts(newPosts: List<Post>) {
        _posts.value = newPosts
    }
}
