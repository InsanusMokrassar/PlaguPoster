package dev.inmo.plaguposter.ratings

import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.repos.unset
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.exposed.ExposedPostsRepo
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.ratings.exposed.ExposedRatingsRepo
import dev.inmo.plaguposter.ratings.repo.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.binds

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { ExposedRatingsRepo(database) } binds arrayOf(
            RatingsRepo::class,
            ReadRatingsRepo::class,
            WriteRatingsRepo::class,
        )
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val ratingsRepo = koin.get<RatingsRepo>()
        koin.get<PostsRepo>().deletedObjectsIdsFlow.subscribeSafelyWithoutExceptions(this) {
            ratingsRepo.unset(it)
        }
    }
}
