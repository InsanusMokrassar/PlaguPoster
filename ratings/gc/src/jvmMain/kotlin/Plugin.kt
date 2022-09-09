package dev.inmo.plaguposter.ratings.gc

import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import dev.inmo.krontab.KrontabTemplate
import dev.inmo.krontab.toSchedule
import dev.inmo.krontab.utils.asFlow
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.repos.deleteById
import dev.inmo.micro_utils.repos.id
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.MilliSeconds
import dev.inmo.tgbotapi.types.Seconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    internal data class Config(
        val autoclear: AutoClearOptions? = null,
        val immediateDrop: Rating? = null,
    ) {
        @Serializable
        data class AutoClearOptions(
            val rating: Rating,
            val autoClearKrontab: KrontabTemplate,
            val skipPostAge: Seconds? = null
        )
    }
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { get<Json>().decodeFromJsonElement(Config.serializer(), params["gc"] ?: return@single null) }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val ratingsRepo = koin.get<RatingsRepo>()
        val postsRepo = koin.get<PostsRepo>()
        val config = koin.get<Config>()

        config.immediateDrop ?.let { toDrop ->
            ratingsRepo.onNewValue.subscribeSafelyWithoutExceptions(this) {
                postsRepo.deleteById(it.id)
            }
        }
        config.autoclear ?.let { autoclear ->
            autoclear.autoClearKrontab.toSchedule().asFlow().subscribeSafelyWithoutExceptions(scope) {
                val dropCreatedBefore = it - (autoclear.skipPostAge ?: 0).seconds
                ratingsRepo.getPostsWithRatingLessEq(autoclear.rating).keys.forEach {
                    if ((postsRepo.getPostCreationTime(it) ?: return@forEach) < dropCreatedBefore) {
                        postsRepo.deleteById(it)
                    }
                }
            }
        }
    }
}
