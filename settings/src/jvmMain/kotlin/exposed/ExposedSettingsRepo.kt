package dev.inmo.plaguposter.settings.exposed

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.plaguposter.settings.repo.SettingsRepo
import dev.inmo.tgbotapi.types.ChatId
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

class ExposedSettingsRepo<T>(
    parent: KeyValueRepo<ChatId, T>
) : SettingsRepo<T>, KeyValueRepo<ChatId, T> by parent

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> ExposedSettingsRepo(
    parent: KeyValueRepo<Long, String>,
    json: Json,
    serializer: KSerializer<T> = T::class.serializer()
) = ExposedSettingsRepo<T>(
    parent.withMapper<ChatId, T, Long, String>(
        { chatId },
        { json.encodeToString(serializer, this) },
        { ChatId(this) },
        { json.decodeFromString(serializer, this) }
    )
)

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> ExposedSettingsRepo(
    database: Database,
    json: Json,
    serializer: KSerializer<T> = T::class.serializer(),
    tableName: String = "settings_${T::class.simpleName!!}"
) = ExposedSettingsRepo<T>(
    ExposedKeyValueRepo(
        database,
        { long("chat_id") },
        { text("settings") },
        tableName
    ),
    json,
    serializer
)

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> SettingsRepo(
    parent: KeyValueRepo<Long, String>,
    json: Json,
    serializer: KSerializer<T> = T::class.serializer()
) = ExposedSettingsRepo<T>(parent, json, serializer)

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> SettingsRepo(
    database: Database,
    json: Json,
    serializer: KSerializer<T> = T::class.serializer(),
    tableName: String = "settings_${T::class.simpleName!!}"
) = ExposedSettingsRepo(database, json, serializer, tableName)
