package dev.inmo.plaguposter.common

import dev.inmo.kslog.common.iS
import dev.inmo.kslog.common.logger
import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.koin.core.Koin
import org.koin.core.module.Module

object CommonPlugin : Plugin {
    private val Log = logger
    override fun Module.setupDI(config: JsonObject) {
        single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
        val useCache = (config["useCache"] as? JsonPrimitive) ?.booleanOrNull ?: true
        useCache(useCache)
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val config = koin.get<ChatConfig>()

        Log.iS { "Target chats info: ${config.allTargetChatIds.map { getChat(it) }.joinToString()}" }
        Log.iS { "Source chats info: ${config.allSourceChatIds.map { getChat(it) }.joinToString()}" }
        Log.iS { "Cache chat info: ${getChat(config.cacheChatId)}" }
    }
}
