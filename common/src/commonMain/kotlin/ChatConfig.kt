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
    val sourceChatId: IdChatIdentifier?,
    @SerialName("cacheChat")
    @Serializable(FullChatIdentifierSerializer::class)
    val cacheChatId: IdChatIdentifier,
    @SerialName("targetChats")
    val targetChatIds: List<@Serializable(FullChatIdentifierSerializer::class) IdChatIdentifier> = emptyList(),
    @SerialName("sourceChats")
    val sourceChatIds: List<@Serializable(FullChatIdentifierSerializer::class) IdChatIdentifier> = emptyList(),
) {
    val allTargetChatIds by lazy {
        (listOfNotNull(targetChatId) + targetChatIds).toSet()
    }
    val allSourceChatIds by lazy {
        (listOfNotNull(sourceChatId) + sourceChatIds).toSet()
    }

    init {
        require(targetChatId != null || targetChatIds.isNotEmpty()) {
            "One of fields, 'targetChat' or 'targetChats' should be presented"
        }
    }

    fun check(chatId: IdChatIdentifier) = when (chatId) {
        in allTargetChatIds,
        in allSourceChatIds,
        cacheChatId -> true
        else -> false
    }
}
