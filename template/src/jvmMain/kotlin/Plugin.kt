package dev.inmo.plaguposter.template

import dev.inmo.plagubot.Plugin
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
    }
}
