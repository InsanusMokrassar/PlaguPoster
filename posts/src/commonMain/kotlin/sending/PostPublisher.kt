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
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent

class PostPublisher(
    private val bot: TelegramBot,
    private val postsRepo: PostsRepo,
    private val cachingChatId: IdChatIdentifier,
    private val targetChatIds: List<IdChatIdentifier>,
    private val deleteAfterPosting: Boolean = true
) {
    suspend fun publish(postId: PostId): Boolean {
        val messagesInfo = postsRepo.getById(postId) ?: let {
            logger.w { "Unable to get post with id $postId for publishing" }
            return false
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
        var haveSentMessages = false

        sortedMessagesContents.forEach { (_, contents) ->
            contents.singleOrNull() ?.also {
                targetChatIds.forEach { targetChatId ->
                    runCatching {
                        bot.copyMessage(fromChatId = it.chatId, messageId = it.messageId, toChatId = targetChatId)
                    }.onFailure { _ ->
                        runCatching {
                            bot.forwardMessage(
                                fromChatId = it.chatId,
                                messageId = it.messageId,
                                toChatId = cachingChatId
                            )
                        }.onSuccess {
                            bot.copyMessage(targetChatId, it)
                            haveSentMessages = true
                        }
                    }.onSuccess {
                        haveSentMessages = true
                    }
                }
                return@forEach
            }
            val resultContents = contents.mapNotNull {
                it.order to (bot.forwardMessage(toChatId = cachingChatId, fromChatId = it.chatId, messageId = it.messageId).contentMessageOrNull() ?: return@mapNotNull null)
            }.sortedBy { it.first }.mapNotNull { (_, forwardedMessage) ->
                forwardedMessage.withContentOrNull<MediaGroupPartContent>() ?: null.also { _ ->
                    targetChatIds.forEach { targetChatId ->
                        bot.copyMessage(targetChatId, forwardedMessage)
                        haveSentMessages = true
                    }
                }
            }
            resultContents.singleOrNull() ?.also {
                targetChatIds.forEach { targetChatId ->
                    bot.copyMessage(targetChatId, it)
                    haveSentMessages = true
                }
                return@forEach
            } ?: resultContents.chunked(mediaCountInMediaGroup.last).forEach {
                targetChatIds.forEach { targetChatId ->
                    bot.send(
                        targetChatId,
                        it.map { it.content.toMediaGroupMemberTelegramMedia() }
                    )
                    haveSentMessages = true
                }
            }
        }

        if (deleteAfterPosting) {
            postsRepo.deleteById(postId)
        }

        return haveSentMessages
    }
}
