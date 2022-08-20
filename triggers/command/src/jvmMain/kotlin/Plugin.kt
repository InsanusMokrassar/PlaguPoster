package dev.inmo.plaguposter.triggers.command

import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.posts.sending.PostPublisher
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
        strictlyOn { state: PublishState ->

            null
        }

        onCommand("publish_post") {
            val messageInReply = it.replyTo ?.contentMessageOrNull() ?: let { _ ->
                reply(it, "You should reply some message of post to trigger it for posting")

                return@onCommand
            }
            val postId = postsRepo.getIdByChatAndMessage(messageInReply.chat.id, messageInReply.messageId)
            if (postId == null) {
                reply(
                    it,
                    "Unable to find any post related to the message in reply"
                )

                return@onCommand
            }

            publisher.publish(postId)
            reply(
                it,
                "Successfully triggered publishing"
            )
        }
    }
}
