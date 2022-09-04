package dev.inmo.plaguposter.posts

import dev.inmo.kslog.common.logger
import dev.inmo.kslog.common.w
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.exposed.ExposedPostsRepo
import dev.inmo.plaguposter.posts.models.ChatConfig
import dev.inmo.plaguposter.posts.repo.*
import dev.inmo.plaguposter.posts.sending.PostPublisher
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.binds

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        val configJson = params["posts"] ?: this@Plugin.let {
            it.logger.w {
                "Unable to load posts plugin due to absence of `posts` key in config"
            }
            return
        }
        single { get<Json>().decodeFromJsonElement(ChatConfig.serializer(), configJson) }
        single { ExposedPostsRepo(database) } binds arrayOf(
            PostsRepo::class,
            ReadPostsRepo::class,
            WritePostsRepo::class,
        )
        single {
            val config = get<ChatConfig>()
            PostPublisher(get(), get(), config.cacheChatId, config.targetChatId)
        }
    }
}
