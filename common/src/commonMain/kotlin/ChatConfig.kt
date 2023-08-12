package dev.inmo.plaguposter.common

import dev.inmo.tgbotapi.types.FullChatIdentifierSerializer
import dev.inmo.tgbotapi.types.IdChatIdentifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    @SerialName("targetChat")
    @Serializable(FullChatIdentifierSerializer::class)
    val targetChatId: IdChatIdentifier? = null,
    @SerialName("sourceChat")
    @Serializable(FullChatIdentifierSerializer::class)
    val sourceChatId: IdChatIdentifier,
    @SerialName("cacheChat")
    @Serializable(FullChatIdentifierSerializer::class)
    val cacheChatId: IdChatIdentifier,
    @SerialName("targetChats")
    val targetChatIds: List<@Serializable(FullChatIdentifierSerializer::class) IdChatIdentifier> = emptyList(),
) {
    val allTargetChatIds by lazy {
        listOfNotNull(targetChatId) + targetChatIds
    }

    init {
        require(targetChatId != null || targetChatIds.isNotEmpty()) {
            "One of fields, 'targetChat' or 'targetChats' should be presented"
        }
    }

    fun check(chatId: IdChatIdentifier) = when (chatId) {
        targetChatId,
        sourceChatId,
        cacheChatId -> true
        else -> false
    }
}
