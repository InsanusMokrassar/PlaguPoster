package dev.inmo.plaguposter.inlines.models

import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.message.MarkdownV2
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Format(
    val template: String,
    val regexTemplate: String = "^$",
    val splitBy: String? = null,
    val enableMarkdownSupport: Boolean = false
) {
    @Transient
    val queryRegex = Regex(regexTemplate, RegexOption.DOT_MATCHES_ALL)

    init {
        println(queryRegex)
    }

    fun formatByRegex(with: String): String? {
        return if (queryRegex.matches(with)) {
            template.format(*(splitBy ?.let { with.split(it).toTypedArray() } ?: arrayOf(with)))
        } else {
            null
        }
    }

    fun createContent(with: String): InputTextMessageContent? {
        return if (queryRegex.matches(with)) {
            InputTextMessageContent(
                template.format(*(splitBy ?.let { with.split(it).toTypedArray() } ?: arrayOf(with))),
                if (enableMarkdownSupport) MarkdownV2 else null
            )
        } else {
            null
        }
    }
}
