package dev.inmo.plaguposter.common

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.FullChatIdentifierSerializer
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.Message
import kotlinx.serialization.Serializable

@Serializable
data class ShortMessageInfo(
    @Serializable(FullChatIdentifierSerializer::class)
    val chatId: IdChatIdentifier,
    val messageId: MessageIdentifier
)

fun Message.short() = ShortMessageInfo(chat.id, messageId)
