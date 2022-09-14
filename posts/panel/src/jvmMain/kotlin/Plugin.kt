package dev.inmo.plaguposter.posts.panel

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.micro_utils.repos.set
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.ChatConfig
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.panel.repos.PostsMessages
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.extensions.sameMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.ParseMode
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    internal data class Config (
        val text: String = "Post settings:",
        val parseMode: ParseMode? = null,
        val buttonsPerRow: Int = 4,
        val deleteButtonText: String? = null,
        val rootButtonText: String = "Return to panel"
    )
    override fun Module.setupDI(database: Database, params: JsonObject) {
        params["panel"] ?.let { element ->
            single { get<Json>().decodeFromJsonElement(Config.serializer(), element) }
        }
        single {
            val config = getOrNull<Config>() ?: Config()
            val builtInButtons = listOfNotNull(
                config.deleteButtonText ?.let { text ->
                    PanelButtonBuilder {
                        CallbackDataInlineKeyboardButton(
                            text,
                            "delete ${it.id.string}"
                        )
                    }
                }
            )
            PanelButtonsAPI(
                getAllDistinct<PanelButtonBuilder>() + builtInButtons,
                config.rootButtonText
            )
        }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val postsRepo = koin.get<PostsRepo>()
        val chatsConfig = koin.get<ChatConfig>()
        val config = koin.getOrNull<Config>() ?: Config()
        val api = koin.get<PanelButtonsAPI>()
        val postsMessages = PostsMessages(koin.get(), koin.get())

        postsRepo.newObjectsFlow.subscribeSafelyWithoutExceptions(this) {
            val firstContent = it.content.first()
            val buttons = api.buttonsBuilders.chunked(config.buttonsPerRow).mapNotNull { row ->
                row.mapNotNull { builder ->
                    builder.buildButton(it)
                }.takeIf { it.isNotEmpty() }
            }
            send(
                firstContent.chatId,
                text = config.text,
                parseMode = config.parseMode,
                replyToMessageId = firstContent.messageId,
                replyMarkup = InlineKeyboardMarkup(buttons),
                disableNotification = true
            ).also { sentMessage ->
                postsMessages.set(it.id, sentMessage.chat.id to sentMessage.messageId)
            }
        }
        postsRepo.deletedObjectsIdsFlow.subscribeSafelyWithoutExceptions(this) {
            val (chatId, messageId) = postsMessages.get(it) ?: return@subscribeSafelyWithoutExceptions

            delete(chatId, messageId)
        }

        suspend fun updatePost(
            postId: PostId,
            chatId: ChatId,
            messageId: MessageIdentifier
        ) {
            val post = postsRepo.getById(postId) ?: return
            val buttons = api.buttonsBuilders.chunked(config.buttonsPerRow).mapNotNull { row ->
                row.mapNotNull { builder ->
                    builder.buildButton(post)
                }.takeIf { it.isNotEmpty() }
            }
            edit(
                chatId,
                messageId,
                replyMarkup = InlineKeyboardMarkup(buttons)
            )
        }

        onMessageDataCallbackQuery (
            initialFilter = {
                it.data.startsWith(PanelButtonsAPI.openGlobalMenuDataPrefix) && it.message.chat.id == chatsConfig.sourceChatId
            }
        ) {
            val postId = it.data.removePrefix(PanelButtonsAPI.openGlobalMenuDataPrefix).let(::PostId)
            updatePost(postId, it.message.chat.id, it.message.messageId)
        }
        onMessageDataCallbackQuery(
            initialFilter = {
                it.data.startsWith("delete ") && it.message.chat.id == chatsConfig.sourceChatId
            }
        ) { query ->
            val postId = query.data.removePrefix("delete ").let(::PostId)
            val post = postsRepo.getById(postId) ?: return@onMessageDataCallbackQuery

            val approveData = uuid4().toString()

            edit(
                query.message,
                replyMarkup = flatInlineKeyboard {
                    dataButton("\uD83D\uDDD1", approveData)
                    api.RootPanelButtonBuilder.buildButton(post) ?.let(::add)
                }
            )

            val pushedButton = waitMessageDataCallbackQuery().first {
                it.message.sameMessage(query.message)
            }

            if (pushedButton.data == approveData) {
                postsRepo.deleteById(postId)
            }
        }

        api.forceRefreshFlow.subscribeSafelyWithoutExceptions(this) {
            val (chatId, messageId) = postsMessages.get(it) ?: return@subscribeSafelyWithoutExceptions
            updatePost(it, chatId, messageId)
        }
    }
}
