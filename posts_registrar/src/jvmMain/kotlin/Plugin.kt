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
import dev.inmo.tgbotapi.extensions.utils.extensions.sameChat
import dev.inmo.tgbotapi.extensions.utils.extensions.sameMessage
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.utils.regular
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import org.koin.core.Koin

@Serializable
object Plugin : Plugin {
    @Serializable
    data class Config(
        val useInlineFinishingOpportunity: Boolean = true
    )

    override suspend fun BehaviourContextWithFSM<State>.setupBotPlugin(koin: Koin) {
        val config = koin.get<ChatConfig>()
        val postsRepo = koin.get<PostsRepo>()

        strictlyOn { state: RegistrationState.InProcess ->
            val buttonUuid = "finish"

            val suggestionMessageDeferred = async {
                send(
                    state.context,
                    dev.inmo.tgbotapi.utils.buildEntities {
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
            }

            firstOf {
                add {
                    val receivedMessage = waitAnyContentMessage().filter {
                        it.sameChat(state.context)
                    }.first()

                    when  {
                        receivedMessage.content.textContentOrNull() ?.text == "/finish_post" -> {
                            val messageToDelete = suggestionMessageDeferred.await()
                            edit(messageToDelete, "Ok, finishing your request")
                            RegistrationState.Finish(
                                state.context,
                                state.messages
                            )
                        }
                        else -> {
                            RegistrationState.InProcess(
                                state.context,
                                state.messages + PostContentInfo.fromMessage(receivedMessage)
                            ).also {
                                runCatchingSafely {
                                    suggestionMessageDeferred.cancel()
                                }
                                runCatchingSafely {
                                    delete(suggestionMessageDeferred.await())
                                }
                            }
                        }
                    }
                }
                add {
                    val messageToDelete = suggestionMessageDeferred.await()
                    val finishPressed = waitMessageDataCallbackQuery().filter {
                        it.message.sameMessage(messageToDelete) && it.data == buttonUuid
                    }.first()

                    edit(messageToDelete, "Ok, finishing your request")
                    RegistrationState.Finish(
                        state.context,
                        state.messages
                    )
                }
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

        onCommand("start_post", initialFilter = { it.sameChat(config.sourceChatId) }) {
            startChain(RegistrationState.InProcess(it.chat.id, emptyList()))
        }

        onContentMessage(
            initialFilter = { it.sameChat(config.sourceChatId) && !FirstSourceIsCommandsFilter(it) }
        ) {
            startChain(RegistrationState.Finish(it.chat.id, PostContentInfo.fromMessage(it)))
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
