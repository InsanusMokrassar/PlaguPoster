package dev.inmo.plaguposter.settings.repo

import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.json.buildJsonObject

suspend inline fun <reified T : Any> SettingsRepo.set(
    chatId: ChatId,
    settings: T
) {
    val oldSettings = get(chatId) ?: buildJsonObject {  }


}
