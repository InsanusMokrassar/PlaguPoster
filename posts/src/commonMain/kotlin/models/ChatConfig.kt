package dev.inmo.plaguposter.posts.models

import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    val targetChatId: ChatId,
    val sourceChatId: ChatId,
    val cacheChatId: ChatId
)
