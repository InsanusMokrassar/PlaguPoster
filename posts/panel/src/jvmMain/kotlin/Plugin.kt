package dev.inmo.plaguposter.posts.panel

import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    @Serializable
    internal data class Config (
        val text: String = "You have registered new post with %s posts",
        val buttonsPrefix: String = ". Here the buttons available for management of post:",
        val preset: List<List<InlineKeyboardButton>>? = null
    )
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single {  }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        TODO("Not yet implemented")
    }
}
