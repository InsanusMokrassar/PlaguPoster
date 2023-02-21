package dev.inmo.plaguposter.posts

import dev.inmo.kslog.common.logger
import dev.inmo.kslog.common.w
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.singleWithBinds
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.SuccessfulSymbol
import dev.inmo.plaguposter.common.UnsuccessfulSymbol
import dev.inmo.plaguposter.posts.exposed.ExposedPostsRepo
import dev.inmo.plaguposter.common.ChatConfig
import dev.inmo.plagubot.plugins.inline.queries.models.Format
import dev.inmo.plagubot.plugins.inline.queries.models.OfferTemplate
import dev.inmo.plagubot.plugins.inline.queries.repos.InlineTemplatesRepo
import dev.inmo.plaguposter.common.useCache
import dev.inmo.plaguposter.posts.cached.CachedPostsRepo
import dev.inmo.plaguposter.posts.repo.*
import dev.inmo.plaguposter.posts.sending.PostPublisher
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.types.message.textsources.regular
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.binds

object Plugin : Plugin {
    @Serializable
    data class Config(
        val chats: ChatConfig,
        val autoRemoveMessages: Boolean = true,
        val deleteAfterPublishing: Boolean = true
    )
    override fun Module.setupDI(database: Database, params: JsonObject) {
        val configJson = params["posts"] ?: this@Plugin.let {
            it.logger.w {
                "Unable to load posts plugin due to absence of `posts` key in config"
            }
            return
        }
        single { get<Json>().decodeFromJsonElement(Config.serializer(), configJson) }
        single { get<Config>().chats }
        single { ExposedPostsRepo(database) }
        singleWithBinds<PostsRepo> {
            val base = get<ExposedPostsRepo>()

            if (useCache) {
                CachedPostsRepo(base, get())
            } else {
                base
            }
        }
        single {
            val config = get<Config>()
            PostPublisher(get(), get(), config.chats.cacheChatId, config.chats.targetChatId, config.deleteAfterPublishing)
        }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val postsRepo = koin.get<PostsRepo>()
        val config = koin.get<Config>()

        if (config.autoRemoveMessages) {
            postsRepo.removedPostsFlow.subscribeSafelyWithoutExceptions(this) {
                it.content.forEach {
                    runCatchingSafely {
                        delete(it.chatId, it.messageId)
                    }
                }
            }
        }

        onCommand("delete_post", requireOnlyCommandInMessage = true) {
            val messageInReply = it.replyTo ?: run {
                reply(it, "Reply some message of post to delete it")
                return@onCommand
            }

            val postId = postsRepo.getIdByChatAndMessage(messageInReply.chat.id, messageInReply.messageId) ?: run {
                reply(it, "Unable to find post id by message")
                return@onCommand
            }

            postsRepo.deleteById(postId)

            if (postsRepo.contains(postId)) {
                edit(it, it.content.textSources + regular(UnsuccessfulSymbol))
            } else {
                edit(it, it.content.textSources + regular(SuccessfulSymbol))
            }
        }

        koin.getOrNull<InlineTemplatesRepo>() ?.addTemplate(
            OfferTemplate(
                "Delete post",
                listOf(
                    Format("/delete_post")
                ),
                "Should be used with a reply on any post message"
            )
        )
    }
}
