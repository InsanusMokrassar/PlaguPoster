package dev.inmo.plaguposter.posts.panel

import dev.inmo.plaguposter.posts.models.RegisteredPost
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton

interface PanelButtonBuilder {
    val weight: Int
    suspend fun buildButton(post: RegisteredPost): InlineKeyboardButton?

    class Default(override val weight: Int = 0, private val block: suspend (RegisteredPost) -> InlineKeyboardButton?) : PanelButtonBuilder {
        override suspend fun buildButton(post: RegisteredPost): InlineKeyboardButton? = block(post)
    }

    companion object {
        operator fun invoke(block: suspend (RegisteredPost) -> InlineKeyboardButton?) = Default(
            block = block
        )
        operator fun invoke(weight: Int, block: suspend (RegisteredPost) -> InlineKeyboardButton?) = Default(
            weight,
            block
        )
    }
}
