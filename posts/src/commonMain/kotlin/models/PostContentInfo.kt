package dev.inmo.plaguposter.posts.models

import dev.inmo.tgbotapi.extensions.utils.mediaGroupMessageOrNull
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import kotlinx.serialization.Serializable

@Serializable
data class PostContentInfo(
    val chatId: ChatId,
    val messageId: MessageIdentifier,
    val group: String?,
    val order: Int
) {
    companion object {
        fun fromMessage(message: ContentMessage<*>, order: Int) = PostContentInfo(
            message.chat.id,
            message.messageId,
            message.mediaGroupMessageOrNull() ?.mediaGroupId,
            order
        )
    }
}
