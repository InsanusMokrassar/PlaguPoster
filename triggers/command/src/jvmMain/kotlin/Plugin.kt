package dev.inmo.plaguposter.triggers.command

import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.SuccessfulSymbol
import dev.inmo.plaguposter.inlines.models.Format
import dev.inmo.plaguposter.inlines.models.OfferTemplate
import dev.inmo.plaguposter.inlines.repos.InlineTemplatesRepo
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.posts.sending.PostPublisher
import dev.inmo.plaguposter.ratings.selector.Selector
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.strictlyOn
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.botCommandTextSourceOrNull
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.message.textsources.regular
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    private data class PublishState(
        override val context: ChatId,
        val sourceMessageId: MessageIdentifier,
        val messageInReply: MessageIdentifier
    ) : State
    override fun Module.setupDI(database: Database, params: JsonObject) {
    }

    override suspend fun BehaviourContextWithFSM<State>.setupBotPlugin(koin: Koin) {
        val postsRepo = koin.get<PostsRepo>()
        val publisher = koin.get<PostPublisher>()
        val selector = koin.getOrNull<Selector>()

        onCommand("publish_post") {
            val messageInReply = it.replyTo ?.contentMessageOrNull() ?: run {
                if (selector == null) {
                    reply(it, "You should reply some message of post to trigger it for posting")

                    return@onCommand
                } else {
                    null
                }
            }
            val postId = messageInReply ?.let {
                postsRepo.getIdByChatAndMessage(messageInReply.chat.id, messageInReply.messageId)
            } ?: selector ?.take(1) ?.firstOrNull()
            if (postId == null) {
                reply(
                    it,
                    "Unable to find any post related to the message in reply"
                )

                return@onCommand
            }

            publisher.publish(postId)

            edit(
                it,
                it.content.textSources + regular(SuccessfulSymbol)
            )
        }

        koin.getOrNull<InlineTemplatesRepo>() ?.apply {
            addTemplate(
                OfferTemplate(
                    "Publish post",
                    listOf(Format("/publish_post")),
                    if (selector == null) {
                        "Require reply on post message"
                    } else {
                        "Publish post according to selector in system or post with message from reply"
                    }
                )
            )
        }
    }
}
