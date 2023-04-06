package dev.inmo.plaguposter.common

import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterExcludeMediaGroups
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.SimpleFilter
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.message.abstracts.*
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.BotCommandTextSource

val FirstSourceIsCommandsFilter = SimpleFilter<Message> {
    it.contentMessageOrNull() ?.withContentOrNull<TextContent>() ?.content ?.textSources ?.firstOrNull() is BotCommandTextSource
}
