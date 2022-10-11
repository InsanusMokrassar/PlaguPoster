package dev.inmo.plaguposter.posts.registrar

import dev.inmo.micro_utils.coroutines.*
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.micro_utils.repos.create
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.*
import dev.inmo.plagubot.plugins.inline.queries.models.Format
import dev.inmo.plagubot.plugins.inline.queries.models.OfferTemplate
import dev.inmo.plagubot.plugins.inline.queries.repos.InlineTemplatesRepo
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
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.extensions.sameMessage
import dev.inmo.tgbotapi.extensions.utils.formatting.buildEntities
import dev.inmo.tgbotapi.extensions.utils.formatting.regular
import dev.inmo.tgbotapi.extensions.utils.mediaGroupMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import org.koin.core.Koin

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
                            it.chat.id == state.context && it.content.textContentOrNull() ?.text != "/finish_post"
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
                add {
                    val finishPressed = waitTextMessage().filter {
                        it.sameChat(messageToDelete) && it.content.text == "/finish_post"
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
        koin.getOrNull<InlineTemplatesRepo>() ?.apply {
            addTemplate(
                OfferTemplate(
                    "Start post creating",
                    listOf(Format("/start_post")),
                    "Use this command to start creating of complex post with several messages"
                )
            )
            addTemplate(
                OfferTemplate(
                    "Finish post creating",
                    listOf(Format("/finish_post")),
                    "Finish creating of complex post"
                )
            )
        }
    }
}
