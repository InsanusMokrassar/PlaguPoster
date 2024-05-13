package dev.inmo.plaguposter.ratings.gc

import korlibs.time.DateTime
import korlibs.time.seconds
import dev.inmo.krontab.KrontabTemplate
import dev.inmo.krontab.toSchedule
import dev.inmo.krontab.utils.asFlowWithDelays
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.repos.*
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
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
            suspend fun checkAndOptionallyDrop(postId: PostId, rating: Rating) {
                if (rating <= toDrop) {
                    postsRepo.deleteById(postId)
                }
            }
            ratingsRepo.getAll().forEach {
                runCatchingSafely {
                    checkAndOptionallyDrop(it.key, it.value)
                }
            }
            ratingsRepo.onNewValue.subscribeSafelyWithoutExceptions(this) {
                checkAndOptionallyDrop(it.first, it.second)
            }
        }
        config.autoclear ?.let { autoclear ->
            suspend fun doAutoClear() {
                val dropCreatedBefore = DateTime.now() - (autoclear.skipPostAge ?: 0).seconds
                ratingsRepo.getPostsWithRatingLessEq(autoclear.rating).keys.forEach {
                    val postCreationDateTime = postsRepo.getPostCreationTime(it) ?: (dropCreatedBefore - 1.seconds) // do dropping if post creation time is not available
                    if (postCreationDateTime < dropCreatedBefore) {
                        ratingsRepo.unset(it)
                        postsRepo.deleteById(it)
                    }
                }
            }
            runCatchingSafely {
                doAutoClear()
            }
            autoclear.autoClearKrontab.toSchedule().asFlowWithDelays().subscribeSafelyWithoutExceptions(scope) {
                doAutoClear()
            }
        }
    }
}
