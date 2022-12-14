package dev.inmo.plaguposter.triggers.timer

import com.soywiz.klock.DateTime
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.repos.set
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.posts.repo.ReadPostsRepo
import dev.inmo.plaguposter.triggers.timer.repo.ExposedTimersRepo
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.binds

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { ExposedTimersRepo(get(), get(), get()) } binds arrayOf(TimersRepo::class)
        single(createdAtStart = true) { TimersHandler(get(), get(), get()) }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val postsRepo = koin.get<ReadPostsRepo>()
        val timersRepo = koin.get<TimersRepo>()
        with(ButtonsBuilder) {
            includeKeyboardHandling { postId, dateTime ->
                timersRepo.set(postId, dateTime)
                true
            }
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
