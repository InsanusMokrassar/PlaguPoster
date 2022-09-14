package dev.inmo.plaguposter.posts.panel

import dev.inmo.plaguposter.posts.models.RegisteredPost
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton

fun interface PanelButtonBuilder {
    suspend fun buildButton(post: RegisteredPost): InlineKeyboardButton?
}
