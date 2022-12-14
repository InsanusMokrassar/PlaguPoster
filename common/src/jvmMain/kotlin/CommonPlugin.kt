package dev.inmo.plaguposter.common

import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.iS
import dev.inmo.kslog.common.logger
import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object CommonPlugin : Plugin {
    private val Log = logger
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val config = koin.get<ChatConfig>()

        Log.iS { "Target chat info: ${getChat(config.targetChatId)}" }
        Log.iS { "Source chat info: ${getChat(config.sourceChatId)}" }
        Log.iS { "Cache chat info: ${getChat(config.cacheChatId)}" }
    }
}
