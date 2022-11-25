package dev.inmo.plaguposter.posts.registrar.state

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.plaguposter.posts.models.PostContentInfo
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.FullChatIdentifierSerializer
import dev.inmo.tgbotapi.types.IdChatIdentifier
import kotlinx.serialization.Serializable

interface RegistrationState : State {
    override val context: IdChatIdentifier

    @Serializable
    data class InProcess(
        @Serializable(FullChatIdentifierSerializer::class)
        override val context: IdChatIdentifier,
        val messages: List<PostContentInfo>
    ) : RegistrationState

    @Serializable
    data class Finish(
        @Serializable(FullChatIdentifierSerializer::class)
        override val context: IdChatIdentifier,
        val messages: List<PostContentInfo>
    ) : RegistrationState
}
