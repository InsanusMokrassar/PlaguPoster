package dev.inmo.plaguposter.settings.repo

import dev.inmo.micro_utils.repos.ReadKeyValueRepo
import dev.inmo.tgbotapi.types.ChatId

interface ReadSettingsRepo<T> : ReadKeyValueRepo<ChatId, T>
