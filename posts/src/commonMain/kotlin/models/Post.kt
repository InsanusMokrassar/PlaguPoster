package dev.inmo.plaguposter.posts.models

import korlibs.time.DateTime
import dev.inmo.plaguposter.common.DateTimeSerializer
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
    @Serializable(DateTimeSerializer::class)
    val created: DateTime,
    override val content: List<PostContentInfo>
) : Post
