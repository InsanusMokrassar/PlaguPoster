package dev.inmo.plaguposter.posts.gc

import com.benasher44.uuid.uuid4
import dev.inmo.krontab.KrontabTemplate
import dev.inmo.krontab.toKronScheduler
import dev.inmo.krontab.utils.asFlowWithDelays
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.iS
import dev.inmo.micro_utils.coroutines.actor
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.plagubot.Plugin
import dev.inmo.plagubot.plugins.inline.queries.models.Format
import dev.inmo.plagubot.plugins.inline.queries.models.OfferTemplate
import dev.inmo.plagubot.plugins.inline.queries.repos.InlineTemplatesRepo
import dev.inmo.plaguposter.common.ChatConfig
import dev.inmo.plaguposter.posts.models.NewPost
import dev.inmo.plaguposter.posts.models.PostContentInfo
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.forwardMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.sameMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.types.MilliSeconds
import dev.inmo.tgbotapi.utils.bold
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    internal data class Config (
        val krontab: KrontabTemplate? = null,
        val throttlingMillis: MilliSeconds = 1000,
        val doFullCheck: Boolean = false
    )
    override fun Module.setupDI(database: Database, params: JsonObject) {
        params["messagesChecker"] ?.let { element ->
            single { get<Json>().decodeFromJsonElement(Config.serializer(), element) }
        }
    }

    private val gcLogger = KSLog("GarbageCollector")
    private suspend fun BehaviourContext.doRecheck(
        throttlingMillis: MilliSeconds,
        doFullCheck: Boolean,
        postsRepo: PostsRepo,
        chatsConfig: ChatConfig
    ) {
        val posts = postsRepo.getAll()
        gcLogger.i {
            "Start garbage collecting of posts. Initial posts count: ${posts.size}"
        }
        posts.forEach { (postId, post) ->
            val surelyAbsentMessages = mutableListOf<PostContentInfo>()
            for (content in post.content) {
                try {
                    forwardMessage(
                        toChatId = chatsConfig.cacheChatId,
                        fromChatId = content.chatId,
                        messageId = content.messageId
                    )

                    if (!doFullCheck) {
                        break
                    }
                } catch (e: Throwable) {
                    if (e.message ?.contains("message to forward not found") == true) {
                        surelyAbsentMessages.add(content)
                    }
                }
                delay(throttlingMillis)
            }
            val existsPostMessages = post.content.filter {
                it !in surelyAbsentMessages
            }
            if (existsPostMessages.isNotEmpty() && surelyAbsentMessages.isNotEmpty()) {
                runCatching {
                    postsRepo.update(
                        postId,
                        NewPost(
                            content = existsPostMessages
                        )
                    )
                }
            }

            if (existsPostMessages.isNotEmpty()) {
                return@forEach
            }

            send(
                chatsConfig.sourceChatId,
                "Can't find any messages for post $postId. So, deleting it"
            )
            runCatching {
                postsRepo.deleteById(postId)
            }
        }
        gcLogger.iS {
            "Complete garbage collecting of posts. Result posts count: ${postsRepo.count()}"
        }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val postsRepo = koin.get<PostsRepo>()
        val chatsConfig = koin.get<ChatConfig>()
        val config = koin.getOrNull<Config>() ?: Config()

        val scope = koin.get<CoroutineScope>()

        val recheckActor = scope.actor<Unit>(0) {
            runCatching {
                doRecheck(
                    config.throttlingMillis,
                    config.doFullCheck,
                    postsRepo,
                    chatsConfig
                )
            }
        }

        config.krontab ?.toKronScheduler() ?.asFlowWithDelays() ?.subscribeSafelyWithoutExceptions(koin.get()) {
            recheckActor.trySend(Unit)
        }

        onCommand("force_garbage_collection") { message ->
            launch {
                val prefix = uuid4().toString()
                val yesData = "${prefix}yes"
                val noData = "${prefix}no"
                edit(
                    message,
                    text = "Are you sure want to trigger posts garbage collecting?",
                    replyMarkup = flatInlineKeyboard {
                        dataButton("Sure", yesData)
                        dataButton("No", noData)
                    }
                )

                val answer = waitMessageDataCallbackQuery().filter {
                    it.message.sameMessage(message)
                }.first()

                if (answer.data == yesData) {
                    if (recheckActor.trySend(Unit).isSuccess) {
                        edit(message, "Checking of posts without exists messages triggered")
                    } else {
                        edit(message) {
                            +"Checking of posts without exists messages has been triggered " + bold("earlier")
                        }
                    }
                } else {
                    delete(message)
                }
            }
        }
        koin.getOrNull<InlineTemplatesRepo>() ?.addTemplate(
            OfferTemplate(
                "Force posts check",
                listOf(
                    Format("/force_garbage_collection")
                ),
                "Force check posts without exists messages"
            )
        )
    }
}
