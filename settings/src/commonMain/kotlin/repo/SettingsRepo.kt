package dev.inmo.plaguposter.settings.repo

import dev.inmo.micro_utils.repos.*
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonObject

interface SettingsRepo<T> : KeyValueRepo<ChatId, T>, ReadSettingsRepo<T>, WriteSettingsRepo<T>
