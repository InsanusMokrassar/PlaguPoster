package dev.inmo.plaguposter.ratings

import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.exposed.ExposedPostsRepo
import dev.inmo.plaguposter.ratings.exposed.ExposedRatingsRepo
import dev.inmo.plaguposter.ratings.repo.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.binds

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { ExposedRatingsRepo(database, get<ExposedPostsRepo>().idColumn) } binds arrayOf(
            RatingsRepo::class,
            ReadRatingsRepo::class,
            WriteRatingsRepo::class,
        )
    }
}
