package dev.inmo.plaguposter.posts.models

import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.Serializable

@Serializable
sealed interface Post {
    val content: List<PostContentInfo>
}

@Serializable
data class NewPost(
    override val content: List<PostContentInfo>
) : Post

@Serializable
data class RegisteredPost(
    val id: PostId,
    override val content: List<PostContentInfo>
) : Post
