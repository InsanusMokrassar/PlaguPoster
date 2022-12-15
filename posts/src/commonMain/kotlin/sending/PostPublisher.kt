package dev.inmo.plaguposter.posts.sending

import dev.inmo.kslog.common.logger
import dev.inmo.kslog.common.w
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.forwardMessage
import dev.inmo.tgbotapi.extensions.api.send.copyMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent

class PostPublisher(
    private val bot: TelegramBot,
    private val postsRepo: PostsRepo,
    private val cachingChatId: IdChatIdentifier,
    private val targetChatId: IdChatIdentifier,
    private val deleteAfterPosting: Boolean = true
) {
    suspend fun publish(postId: PostId) {
        val messagesInfo = postsRepo.getById(postId) ?: let {
            logger.w { "Unable to get post with id $postId for publishing" }
            return
        }
        val sortedMessagesContents = messagesInfo.content.groupBy { it.group }.flatMap { (group, list) ->
            if (group == null) {
                list.map {
                    it.order to listOf(it)
                }
            } else {
                listOf(list.first().order to list)
            }
        }.sortedBy { it.first }

        sortedMessagesContents.forEach { (_, contents) ->
            contents.singleOrNull() ?.also {
                runCatching {
                    bot.copyMessage(targetChatId, it.chatId, it.messageId)
                }.onFailure { _ ->
                    runCatching {
                        bot.forwardMessage(
                            it.chatId,
                            targetChatId,
                            it.messageId
                        )
                    }.onSuccess {
                        bot.copyMessage(targetChatId, it)
                    }
                }
                return@forEach
            }
            val resultContents = contents.mapNotNull {
                it.order to (bot.forwardMessage(toChatId = cachingChatId, fromChatId = it.chatId, messageId = it.messageId).contentMessageOrNull() ?: return@mapNotNull null)
            }.sortedBy { it.first }.mapNotNull { (_, forwardedMessage) ->
                forwardedMessage.withContentOrNull<MediaGroupPartContent>() ?: null.also { _ ->
                    bot.copyMessage(targetChatId, forwardedMessage)
                }
            }
            resultContents.singleOrNull() ?.also {
                bot.copyMessage(targetChatId, it)
                return@forEach
            } ?: resultContents.chunked(mediaCountInMediaGroup.last).forEach {
                bot.send(
                    targetChatId,
                    it.map { it.content.toMediaGroupMemberTelegramMedia() }
                )
            }
        }

        if (deleteAfterPosting) {
            postsRepo.deleteById(postId)
        }

    }
}
