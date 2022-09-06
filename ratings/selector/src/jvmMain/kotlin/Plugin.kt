package dev.inmo.plaguposter.ratings.selector

import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.ratings.selector.models.SelectorConfig
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { get<Json>().decodeFromJsonElement(SelectorConfig.serializer(), params["selector"] ?: return@single null) }
        single<Selector> { DefaultSelector(get(), get()) }
    }
}
