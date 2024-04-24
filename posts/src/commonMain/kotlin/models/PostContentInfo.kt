package dev.inmo.plaguposter.posts.models

import dev.inmo.tgbotapi.extensions.utils.possiblyMediaGroupMessageOrNull
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import kotlinx.serialization.Serializable

@Serializable
data class PostContentInfo(
    @Serializable(FullChatIdentifierSerializer::class)
    val chatId: IdChatIdentifier,
    val messageId: MessageId,
    val group: MediaGroupId?,
    val order: Int
) {
    companion object {
        private fun fromMessage(message: ContentMessage<*>, order: Int) = PostContentInfo(
            message.chat.id,
            message.messageId,
            message.possiblyMediaGroupMessageOrNull() ?.mediaGroupId,
            order
        )
        fun fromMessage(message: ContentMessage<*>): List<PostContentInfo> {
            val content = message.content

            return if (content is MediaGroupContent<*>) {
                content.group.mapIndexed { i, it ->
                    fromMessage(it.sourceMessage, i)
                }
            } else {
                listOf(fromMessage(message, 0))
            }
        }
    }
}
