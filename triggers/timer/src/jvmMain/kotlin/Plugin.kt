package dev.inmo.plaguposter.triggers.timer

import com.soywiz.klock.DateTime
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.singleWithRandomQualifierAndBinds
import dev.inmo.micro_utils.repos.set
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.ChatConfig
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.panel.PanelButtonsAPI
import dev.inmo.plaguposter.posts.repo.ReadPostsRepo
import dev.inmo.plaguposter.triggers.timer.repo.ExposedTimersRepo
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.binds

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        single { ExposedTimersRepo(get(), get(), get()) } binds arrayOf(TimersRepo::class)
        single(createdAtStart = true) { TimersHandler(get(), get(), get()) }
        singleWithRandomQualifierAndBinds { TimerPanelButton(get()) }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val timersRepo = koin.get<TimersRepo>()
        val chatsConfig = koin.get<ChatConfig>()
        val panelApi = koin.get<PanelButtonsAPI>()
        val scope = koin.get<CoroutineScope>()
        with(ButtonsBuilder) {
            includeKeyboardHandling(timersRepo) { postId, dateTime ->
                timersRepo.set(postId, dateTime)
                true
            }
        }

        timersRepo.onNewValue.subscribeSafelyWithoutExceptions(scope) {
            panelApi.forceRefresh(it.first)
        }

        timersRepo.onValueRemoved.subscribeSafelyWithoutExceptions(scope) {
            panelApi.forceRefresh(it)
        }

        onMessageDataCallbackQuery(
            Regex("${TimerPanelButton.timerSetPrefix} [^\\s]+"),
            initialFilter = {
                chatsConfig.check(it.message.chat.id)
            }
        ) {
            val (_, postIdRaw) = it.data.split(" ")
            val postId = PostId(postIdRaw)
            val now = nearestAvailableTimerTime()
            val exists = timersRepo.get(postId)
            val textSources = ButtonsBuilder.buildTimerTextSources(now, exists)
            val buttons = ButtonsBuilder.buildTimerButtons(
                postId,
                now.local,
                exists != null
            )
            reply(
                it.message,
                textSources,
                replyMarkup = buttons
            )

            answer(it)
        }
    }
}
