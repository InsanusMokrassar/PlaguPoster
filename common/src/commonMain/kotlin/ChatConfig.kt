package dev.inmo.plaguposter.common

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.FullChatIdentifierSerializer
import dev.inmo.tgbotapi.types.IdChatIdentifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    @SerialName("targetChat")
    @Serializable(FullChatIdentifierSerializer::class)
    val targetChatId: IdChatIdentifier,
    @SerialName("sourceChat")
    @Serializable(FullChatIdentifierSerializer::class)
    val sourceChatId: IdChatIdentifier,
    @SerialName("cacheChat")
    @Serializable(FullChatIdentifierSerializer::class)
    val cacheChatId: IdChatIdentifier
) {
    fun check(chatId: IdChatIdentifier) = when (chatId) {
        targetChatId,
        sourceChatId,
        cacheChatId -> true
        else -> false
    }
}
