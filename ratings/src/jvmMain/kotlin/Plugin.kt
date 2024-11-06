package dev.inmo.plaguposter.ratings

import dev.inmo.krontab.utils.asTzFlowWithDelays
import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.i
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.singleWithBinds
import dev.inmo.micro_utils.repos.unset
import dev.inmo.plagubot.Plugin
import dev.inmo.plagubot.config
import dev.inmo.plagubot.database
import dev.inmo.plagubot.registerConfig
import dev.inmo.plaguposter.common.useCache
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.ratings.Plugin.setupBotPlugin
import dev.inmo.plaguposter.ratings.exposed.ExposedRatingsRepo
import dev.inmo.plaguposter.ratings.models.RatingsConfig
import dev.inmo.plaguposter.ratings.repo.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.*
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    private val Log = TagLogger("RatingsPlugin")
    override fun Module.setupDI(config: JsonObject) {
        single { ExposedRatingsRepo(database) }
        singleWithBinds<RatingsRepo> {
            val base = get<ExposedRatingsRepo>()

            if (useCache) {
                CachedRatingsRepo(base, get())
            } else {
                base
            }
        }

        registerConfig(RatingsConfig.serializer(), "ratings") { RatingsConfig() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        val config = koin.config<RatingsConfig>()
        val scope = koin.get<CoroutineScope>()
        val ratingsRepo = koin.get<RatingsRepo>()
        val postsRepo = koin.get<PostsRepo>()
        postsRepo.deletedObjectsIdsFlow.subscribeSafelyWithoutExceptions(scope) {
            ratingsRepo.unset(it)
        }
        config.manualRecheckKrontab.asTzFlowWithDelays().subscribeSafelyWithoutExceptions(scope) {
            Log.i { "Start clearing ratings without registered posts" }
            val postsIdsToRemove = ratingsRepo.getAll().keys - postsRepo.getAll().keys
            Log.i { "Posts to remove: $postsIdsToRemove" }
            ratingsRepo.unset(postsIdsToRemove.toList())
        }
    }
}
