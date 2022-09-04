package dev.inmo.plaguposter.posts.registrar

import com.benasher44.uuid.uuid4
import dev.inmo.kslog.common.logger
import dev.inmo.kslog.common.w
import dev.inmo.micro_utils.coroutines.firstOf
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.micro_utils.repos.create
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.FirstSourceIsCommandsFilter
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.plaguposter.posts.registrar.state.RegistrationState
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.strictlyOn
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.extensions.sameMessage
import dev.inmo.tgbotapi.extensions.utils.formatting.buildEntities
import dev.inmo.tgbotapi.extensions.utils.formatting.regular
import dev.inmo.tgbotapi.extensions.utils.mediaGroupMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module

@Serializable
object Plugin : Plugin {
    override suspend fun BehaviourContextWithFSM<State>.setupBotPlugin(koin: Koin) {
        val config = koin.get<ChatConfig>()
        val postsRepo = koin.get<PostsRepo>()

        strictlyOn {state: RegistrationState.InProcess ->
            val buttonUuid = "finish"

            val messageToDelete = send(
                state.context,
                buildEntities {
                    if (state.messages.isNotEmpty()) {
                        regular("Your message(s) has been registered. You may send new ones or push \"Finish\" to finalize your post")
                    } else {
                        regular("Ok, send me your messages for new post")
                    }
                },
                replyMarkup = if (state.messages.isNotEmpty()) {
                    flatInlineKeyboard {
                        dataButton(
                            "Finish",
                            buttonUuid
                        )
                    }
                } else {
                    null
                }
            )

            val newMessagesInfo = firstOf {
                add {
                    listOf(
                        waitContentMessage(
                            includeMediaGroups = false
                        ).filter {
                            it.chat.id == state.context
                        }.take(1).first()
                    )
                }
                add {
                    waitMediaGroupMessages().filter {
                        it.first().chat.id == state.context
                    }.take(1).first()
                }
                add {
                    val finishPressed = waitMessageDataCallbackQuery().filter {
                        it.message.sameMessage(messageToDelete) && it.data == buttonUuid
                    }.first()
                    emptyList<ContentMessage<MessageContent>>()
                }
            }.ifEmpty {
                edit(messageToDelete, "Ok, finishing your request")
                return@strictlyOn RegistrationState.Finish(
                    state.context,
                    state.messages
                )
            }.map {
                PostContentInfo.fromMessage(it, state.messages.size)
            }

            RegistrationState.InProcess(
                state.context,
                state.messages + newMessagesInfo
            ).also {
                delete(messageToDelete)
            }
        }

        strictlyOn { state: RegistrationState.Finish ->
            postsRepo.create(
                NewPost(
                    state.messages
                )
            ).firstOrNull() ?.let {
                send(state.context, "Ok, you have registered ${it.content.size} messages as new post")
            } ?: send(
                state.context,
                "Sorry, for some reason I was unable to register your post"
            )
            null
        }

        onCommand("start_post", initialFilter = { it.chat.id == config.sourceChatId }) {
            startChain(RegistrationState.InProcess(it.chat.id, emptyList()))
        }

        onContentMessage(
            initialFilter = { it.chat.id == config.sourceChatId && it.mediaGroupMessageOrNull() ?.mediaGroupId == null && !FirstSourceIsCommandsFilter(it) }
        ) {
            startChain(RegistrationState.Finish(it.chat.id, listOf(PostContentInfo.fromMessage(it, 0))))
        }

        onMediaGroup(
            initialFilter = { it.first().chat.id == config.sourceChatId }
        ) {
            startChain(
                RegistrationState.Finish(
                    it.first().chat.id,
                    it.map {
                        PostContentInfo.fromMessage(
                            it,
                            0
                        )
                    }
                )
            )
        }
    }
}
