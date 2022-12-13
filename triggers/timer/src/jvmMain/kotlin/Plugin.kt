package dev.inmo.plaguposter.triggers.timer

import com.soywiz.klock.DateTime
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.repo.ReadPostsRepo
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val postsRepo = koin.get<ReadPostsRepo>()
        with(ButtonsBuilder) {
            includeKeyboardHandling()
        }
        onCommand("test") {
            val reply = it.replyTo ?: return@onCommand
            val postId = postsRepo.getIdByChatAndMessage(
                reply.chat.id,
                reply.messageId
            ) ?: return@onCommand
            val buttons = ButtonsBuilder.buildTimerButtons(
                postId,
                DateTime.nowLocal()
            )
            runCatchingSafely {
                edit(
                    it,
                    buttons
                )
            }.onFailure { _ ->
                send(
                    it.chat,
                    "Buttons",
                    replyMarkup = buttons
                )
            }
        }
    }
}
