package dev.inmo.plaguposter.posts.registrar.state

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.plaguposter.posts.models.PostContentInfo
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.Serializable

interface RegistrationState : State {
    override val context: ChatId

    @Serializable
    data class InProcess(
        override val context: ChatId,
        val messages: List<PostContentInfo>
    ) : RegistrationState

    @Serializable
    data class Finish(
        override val context: ChatId,
        val messages: List<PostContentInfo>
    ) : RegistrationState
}
