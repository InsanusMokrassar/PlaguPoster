package dev.inmo.plaguposter.triggers.timer

import dev.inmo.plaguposter.common.SuccessfulSymbol
import dev.inmo.plaguposter.common.UnsuccessfulSymbol
import dev.inmo.plaguposter.posts.models.RegisteredPost
import dev.inmo.plaguposter.posts.panel.PanelButtonBuilder
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.KeyboardButtonStyle

class TimerPanelButton(
    private val timersRepo: TimersRepo
) : PanelButtonBuilder {
    override val weight: Int
        get() = 0

    override suspend fun buildButton(post: RegisteredPost): InlineKeyboardButton? {
        val publishingTime = timersRepo.get(post.id)

        return CallbackDataInlineKeyboardButton(
            "⏰ ${ if (publishingTime == null) UnsuccessfulSymbol else SuccessfulSymbol }",
            "$timerSetPrefix ${post.id}",
            style = if (publishingTime == null) {
                null
            } else {
                KeyboardButtonStyle.Success
            }
        )
    }

    companion object {
        const val timerSetPrefix = "timer_set_init"
    }
}
