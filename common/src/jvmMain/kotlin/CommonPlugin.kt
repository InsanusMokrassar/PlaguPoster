package dev.inmo.plaguposter.common

import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.iS
import dev.inmo.kslog.common.logger
import dev.inmo.plagubot.Plugin
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import org.koin.core.Koin

object CommonPlugin : Plugin {
    private val Log = logger
    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val config = koin.get<ChatConfig>()

        Log.iS { "Target chat info: ${getChat(config.targetChatId)}" }
        Log.iS { "Source chat info: ${getChat(config.sourceChatId)}" }
        Log.iS { "Cache chat info: ${getChat(config.cacheChatId)}" }
    }
}
