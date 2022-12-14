package dev.inmo.plaguposter.triggers.selector_with_timer

import dev.inmo.krontab.KrontabTemplate
import dev.inmo.krontab.toSchedule
import dev.inmo.krontab.utils.asFlow
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.sending.PostPublisher
import dev.inmo.plaguposter.ratings.selector.Selector
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import kotlinx.coroutines.FlowPreview
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    internal data class Config(
        @SerialName("krontab")
        val krontabTemplate: KrontabTemplate
    ) {
        @Transient
        val krontab by lazy {
            krontabTemplate.toSchedule()
        }
    }
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { get<Json>().decodeFromJsonElement(Config.serializer(), params["timer_trigger"] ?: return@single null) }
    }

    @OptIn(FlowPreview::class)
    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val publisher = koin.get<PostPublisher>()
        val selector = koin.get<Selector>()
        val filters = koin.getAll<AutopostFilter>().distinct()
        koin.get<Config>().krontab.asFlow().subscribeSafelyWithoutExceptions(this) { dateTime ->
            selector.take(now = dateTime).forEach { postId ->
                if (filters.all { it.check(postId, dateTime) }) {
                    publisher.publish(postId)
                }
            }
        }
    }
}
