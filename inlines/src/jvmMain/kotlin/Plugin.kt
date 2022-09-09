package dev.inmo.plaguposter.inlines

import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.utils.paginate
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.ChatConfig
import dev.inmo.plaguposter.inlines.models.Format
import dev.inmo.plaguposter.inlines.models.OfferTemplate
import dev.inmo.plaguposter.inlines.repos.InlineTemplatesRepo
import dev.inmo.tgbotapi.bot.exceptions.RequestException
import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.types.inlineQueryAnswerResultsLimit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    internal data class Config(
        val preset: List<OfferTemplate>
    )
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { get<Json>().decodeFromJsonElement(Config.serializer(), params["inlines"] ?: return@single Config(emptyList())) }
        single { InlineTemplatesRepo(getOrNull<Config>() ?.preset ?.toMutableSet() ?: mutableSetOf()) }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val templatesRepo = koin.get<InlineTemplatesRepo>()
        onBaseInlineQuery { query ->
            val page = query.offset.toIntOrNull() ?: 0
            val queryString = query.query.trim()
            try {
                answerInlineQuery(
                    query,
                    templatesRepo.templates.paginate(
                        Pagination(
                            page,
                            inlineQueryAnswerResultsLimit.last + 1
                        )
                    ).results.mapIndexedNotNull { index, offerTemplate ->
                        offerTemplate.createArticleResult(
                            index.toString(),
                            queryString
                        )
                    },
                    nextOffset = (page + 1).toString(),
                    cachedTime = 0
                )
            } catch (e: RequestException) {
                bot.answerInlineQuery(
                    query,
                    cachedTime = 0
                )
            }
        }
        onCommand("help", requireOnlyCommandInMessage = true) {
            reply(
                it,
                "Push the button above to see available commands",
                replyMarkup = flatInlineKeyboard {
                    inlineQueryInCurrentChatButton("Toggle commands", "")
                }
            )
        }
        koin.getOrNull<InlineTemplatesRepo>() ?.apply {
            addTemplate(
                OfferTemplate(
                    "Trigger help button",
                    listOf(Format("/help"))
                )
            )
        }
    }
}
