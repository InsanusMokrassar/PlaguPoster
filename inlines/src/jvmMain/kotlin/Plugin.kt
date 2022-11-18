package dev.inmo.plaguposter.inlines

import dev.inmo.kslog.common.TagLogger
import dev.inmo.kslog.common.w
import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

private val actualPlugin = dev.inmo.plagubot.plugins.inline.queries.Plugin

object Plugin : Plugin by actualPlugin {
    private val log = TagLogger("InlinePlugin")

    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { actualPlugin }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        log.w {
            "Built-in inline plugin has been deprecated. Use \"${actualPlugin::class.qualifiedName}\" instead"
        }
        with(actualPlugin) {
            setupBotPlugin(koin)
        }
    }
}
