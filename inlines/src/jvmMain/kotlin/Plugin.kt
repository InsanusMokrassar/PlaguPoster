package dev.inmo.plaguposter.inlines

import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.utils.paginate
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.ChatConfig
import dev.inmo.plaguposter.inlines.models.OfferTemplate
import dev.inmo.plaguposter.inlines.repos.InlineTemplatesRepo
import dev.inmo.tgbotapi.bot.exceptions.RequestException
import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.libraries.cache.admins.AdminsCacheAPI
import dev.inmo.tgbotapi.libraries.cache.admins.adminsPlugin
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
        val adminsApi = koin.get<AdminsCacheAPI>()
        val chatConfig = koin.get<ChatConfig>()
        val templatesRepo = koin.get<InlineTemplatesRepo>()
        onBaseInlineQuery { query ->
            if (!adminsApi.isAdmin(chatConfig.sourceChatId, query.from.id)) {
                answerInlineQuery(query, cachedTime = 0)
                return@onBaseInlineQuery
            }
            val page = query.offset.toIntOrNull() ?: 0
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
                            query.query
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
    }
}
