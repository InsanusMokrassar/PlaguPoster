package dev.inmo.plaguposter.posts.panel

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import kotlinx.serialization.Serializable

@Serializable
data class PanelButtonSettings(
    val button: InlineKeyboardButton
)
