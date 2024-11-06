package dev.inmo.plaguposter.triggers.command

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.SuccessfulSymbol
import dev.inmo.plagubot.plugins.inline.queries.models.Format
import dev.inmo.plagubot.plugins.inline.queries.models.OfferTemplate
import dev.inmo.plagubot.plugins.inline.queries.repos.InlineTemplatesRepo
import dev.inmo.plagubot.registerConfig
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.panel.PanelButtonBuilder
import dev.inmo.plaguposter.posts.panel.PanelButtonsAPI
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.posts.sending.PostPublisher
import dev.inmo.plaguposter.ratings.selector.Selector
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.extensions.sameMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.textsources.regular
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    private data class PublishState(
        override val context: ChatId,
        val sourceMessageId: MessageId,
        val messageInReply: MessageId
    ) : State
    @Serializable
    internal data class Config(
        val panelButtonText: String? = "Publish"
    )
    override fun Module.setupDI(params: JsonObject) {
        registerConfig<Config>("publish_command") { null }
    }

    override suspend fun BehaviourContextWithFSM<State>.setupBotPlugin(koin: Koin) {
        val postsRepo = koin.get<PostsRepo>()
        val publisher = koin.get<PostPublisher>()
        val selector = koin.getOrNull<Selector>()
        val config = koin.getOrNull<Config>()
        val panelApi = koin.getOrNull<PanelButtonsAPI>()

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
                postsRepo.getIdByChatAndMessage(messageInReply.chat.id, messageInReply.messageId) ?: let { _ ->
                    reply(
                        it,
                        "Unable to find any post related to the message in reply"
                    )

                    return@onCommand
                }
            } ?: selector ?.takeOneOrNull()
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

        panelApi ?.apply {
            config ?.panelButtonText ?.let { text ->
                add(
                    PanelButtonBuilder {
                        CallbackDataInlineKeyboardButton(
                            text,
                            "publish ${it.id.string}"
                        )
                    }
                )
                onMessageDataCallbackQuery(
                    initialFilter = {
                        it.data.startsWith("publish ")
                    }
                ) {
                    val postId = it.data.removePrefix("publish ").let(::PostId)
                    val post = postsRepo.getById(postId) ?: return@onMessageDataCallbackQuery

                    val publishData = uuid4().toString()

                    val edited = edit(
                        it.message,
                        replyMarkup = flatInlineKeyboard {
                            dataButton(SuccessfulSymbol, publishData)
                            RootPanelButtonBuilder.buildButton(post) ?.let(::add)
                        }
                    )

                    val pushedButton = waitMessageDataCallbackQuery().first {
                        it.message.sameMessage(edited)
                    }

                    if (pushedButton.data == publishData) {
                        publisher.publish(postId)
                    }
                }
            }
        }
    }
}
