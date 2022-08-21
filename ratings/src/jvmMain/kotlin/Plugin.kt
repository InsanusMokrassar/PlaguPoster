package dev.inmo.plaguposter.ratings

import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.exposed.ExposedPostsRepo
import dev.inmo.plaguposter.ratings.exposed.ExposedRatingsRepo
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { ExposedRatingsRepo(database, get<ExposedPostsRepo>().idColumn) }
    }
}
