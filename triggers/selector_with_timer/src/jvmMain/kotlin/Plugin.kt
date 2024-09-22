package dev.inmo.plaguposter.triggers.selector_with_timer

import korlibs.time.DateFormat
import dev.inmo.krontab.KrontabTemplate
import dev.inmo.krontab.toSchedule
import dev.inmo.krontab.utils.asFlowWithDelays
import dev.inmo.krontab.utils.asFlowWithoutDelays
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.firstIndex
import dev.inmo.micro_utils.pagination.lastIndexExclusive
import dev.inmo.plagubot.Plugin
import dev.inmo.plagubot.plugins.inline.queries.models.Format
import dev.inmo.plagubot.plugins.inline.queries.models.OfferTemplate
import dev.inmo.plagubot.plugins.inline.queries.repos.InlineTemplatesRepo
import dev.inmo.plagubot.registerConfig
import dev.inmo.plaguposter.common.ChatConfig
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.repo.ReadPostsRepo
import dev.inmo.plaguposter.posts.sending.PostPublisher
import dev.inmo.plaguposter.ratings.selector.Selector
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.formatting.makeLinkToMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.take
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    private const val pageCallbackDataQueryPrefix = "publishing_autoschedule page"
    private const val pageCallbackDataQuerySize = 5
        @Serializable
    internal data class Config(
        @SerialName("krontab")
        val krontabTemplate: KrontabTemplate,
        val dateTimeFormat: String = "HH:mm:ss, dd.MM.yyyy",
        val retryOnPostFailureTimes: Int = 0
    ) {
        @Transient
        val krontab by lazy {
            krontabTemplate.toSchedule()
        }

        @Transient
        val format: DateFormat = DateFormat(dateTimeFormat)
    }
    override fun Module.setupDI(params: JsonObject) {
        registerConfig<Config>("timer_trigger") { null }
    }

    @OptIn(FlowPreview::class)
    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val publisher = koin.get<PostPublisher>()
        val selector = koin.get<Selector>()
        val filters = koin.getAll<AutopostFilter>().distinct()
        val chatConfig = koin.get<ChatConfig>()
        val postsRepo = koin.get<ReadPostsRepo>()

        koin.getOrNull<InlineTemplatesRepo>() ?.apply {
            addTemplate(
                OfferTemplate(
                    "Autoschedule buttons",
                    listOf(
                        Format(
                            "/autoschedule_panel"
                        )
                    ),
                    "Show autoscheduling publishing info"
                )
            )
        }

        val krontab = koin.get<Config>().krontab
        val retryOnPostFailureTimes = koin.get<Config>().retryOnPostFailureTimes
        val dateTimeFormat = koin.get<Config>().format
        krontab.asFlowWithDelays().subscribeSafelyWithoutExceptions(this) { dateTime ->
            var leftRetries = retryOnPostFailureTimes
            do {
                val success = runCatching {
                    selector.takeOneOrNull(now = dateTime) ?.let { postId ->
                        if (filters.all { it.check(postId, dateTime) }) {
                            publisher.publish(postId)
                        } else {
                            false
                        }
                    } ?: false
                }.getOrElse {
                    false
                }
                if (success) {
                    break;
                }
                leftRetries--;
            } while (leftRetries > 0)
        }

        suspend fun buildPage(pagination: Pagination = FirstPagePagination(size = pageCallbackDataQuerySize)): InlineKeyboardMarkup {
            return inlineKeyboard {
                row {
                    if (pagination.page > 1) {
                        dataButton("⬅️", "${pageCallbackDataQueryPrefix}0")
                    }
                    if (pagination.page > 0) {
                        dataButton("◀️", "${pageCallbackDataQueryPrefix}${pagination.page - 1}")
                    }

                    dataButton("\uD83D\uDD04 ${pagination.page}", "${pageCallbackDataQueryPrefix}${pagination.page}")
                    dataButton("▶️", "${pageCallbackDataQueryPrefix}${pagination.page + 1}")
                }

                val selected = mutableListOf<PostId>()
                krontab.asFlowWithoutDelays().take(pagination.lastIndexExclusive).collectIndexed { i, dateTime ->
                    val postId = selector.takeOneOrNull(now = dateTime, exclude = selected) ?.also { postId ->
                        if (filters.all { it.check(postId, dateTime) }) {
                            selected.add(postId)
                        } else {
                            return@collectIndexed
                        }
                    }

                    val post = postsRepo.getFirstMessageInfo(postId ?: return@collectIndexed)
                    if (i < pagination.firstIndex || post == null) {
                        return@collectIndexed
                    }

                    row {
                        urlButton(
                            dateTime.local.format(dateTimeFormat),
                            makeLinkToMessage(post.chatId, post.messageId)
                        )
                    }
                }
            }
        }

        onCommand("autoschedule_panel", initialFilter = { chatConfig.allSourceChatIds.any { chatId -> it.sameChat(chatId) } }) {
            val keyboard = buildPage()

            runCatchingSafely {
                edit(it, replyMarkup = keyboard) {
                    +"Your schedule:"
                }
            }.onFailure { _ ->
                send(it.chat, replyMarkup = keyboard) {
                    +"Your schedule:"
                }
            }
        }

        onMessageDataCallbackQuery(
            Regex("^$pageCallbackDataQueryPrefix\\d+"),
            initialFilter = { chatConfig.allSourceChatIds.any { sourceChatId -> it.message.sameChat(sourceChatId) } }
        ) {
            val page = it.data.removePrefix(pageCallbackDataQueryPrefix).toIntOrNull() ?: let { _ ->
                answer(it)
                return@onMessageDataCallbackQuery
            }

            runCatchingSafely {
                edit(
                    it.message,
                    replyMarkup = buildPage(Pagination(page, size = pageCallbackDataQuerySize))
                )
            }.onFailure { _ ->
                answer(it)
            }
        }
    }
}
