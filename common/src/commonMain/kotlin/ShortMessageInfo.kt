package dev.inmo.plaguposter.common

import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.message.abstracts.Message
import kotlinx.serialization.Serializable

@Serializable
data class ShortMessageInfo(
    @Serializable(FullChatIdentifierSerializer::class)
    val chatId: IdChatIdentifier,
    val messageId: MessageId
)

fun Message.short() = ShortMessageInfo(chat.id, messageId)
