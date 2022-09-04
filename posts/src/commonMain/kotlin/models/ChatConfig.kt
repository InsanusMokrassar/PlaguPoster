package dev.inmo.plaguposter.posts.models

import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    @SerialName("targetChat")
    val targetChatId: ChatId,
    @SerialName("sourceChat")
    val sourceChatId: ChatId,
    @SerialName("cacheChat")
    val cacheChatId: ChatId
)
