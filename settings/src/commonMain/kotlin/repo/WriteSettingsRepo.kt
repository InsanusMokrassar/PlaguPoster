package dev.inmo.plaguposter.settings.repo

import dev.inmo.micro_utils.repos.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.serialization.json.JsonObject

interface WriteSettingsRepo<T> : WriteKeyValueRepo<ChatId, T>
