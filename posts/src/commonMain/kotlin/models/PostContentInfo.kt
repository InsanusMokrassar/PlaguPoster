package dev.inmo.plaguposter.posts.models

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import kotlinx.serialization.Serializable

@Serializable
data class PostContentInfo(
    val chatId: ChatId,
    val messageId: MessageIdentifier,
    val group: String?,
    val order: Int
)
