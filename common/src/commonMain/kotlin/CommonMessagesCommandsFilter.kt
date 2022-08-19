package dev.inmo.plaguposter.common

import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterExcludeMediaGroups
import dev.inmo.tgbotapi.extensions.behaviour_builder.utils.SimpleFilter
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.message.abstracts.*
import dev.inmo.tgbotapi.types.message.textsources.BotCommandTextSource

val FirstSourceIsCommandsFilter = SimpleFilter<Message> {
    it is ContentMessage<*> && it.content.textContentOrNull() ?.textSources ?.firstOrNull {
        it is BotCommandTextSource
    } != null
}
