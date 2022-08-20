package dev.inmo.plaguposter.posts

import dev.inmo.kslog.common.logger
import dev.inmo.kslog.common.w
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.exposed.ExposedPostsRepo
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.posts.sending.PostPublisher
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    private data class Config(
        @SerialName("targetChat")
        val targetChatId: ChatId,
        @SerialName("cacheChat")
        val cacheChatId: ChatId
    )

    override fun Module.setupDI(database: Database, params: JsonObject) {
        val configJson = params["posts"] ?: this@Plugin.let {
            it.logger.w {
                "Unable to load posts plugin due to absence of `posts` key in config"
            }
            return
        }
        single { get<Json>().decodeFromJsonElement(Config.serializer(), configJson) }
        single<PostsRepo> { ExposedPostsRepo(database) }
        single {
            val config = get<Config>()
            PostPublisher(get(), get(), config.cacheChatId, config.targetChatId)
        }
    }
}
