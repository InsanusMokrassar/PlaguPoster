package dev.inmo.plaguposter.ratings.selector

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.selector.models.SelectorConfig
import korlibs.time.DateTime
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.core.qualifier.qualifier

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { get<Json>().decodeFromJsonElement(SelectorConfig.serializer(), params["selector"] ?: return@single null) }
        single<KeyValueRepo<PostId, DateTime>>(qualifier("latestChosenRepo")) {
            ExposedKeyValueRepo(
                get(),
                { text("post_id") },
                { double("date_time") },
                "LatestChosenRepo"
            ).withMapper(
                { string },
                { unixMillis },
                { PostId(this) },
                { DateTime(this) }
            )
        }
        single<Selector> { DefaultSelector(get(), get(), get(), get(qualifier("latestChosenRepo"))) }
    }
}
