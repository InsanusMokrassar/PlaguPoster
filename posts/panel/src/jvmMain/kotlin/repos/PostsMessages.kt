package dev.inmo.plaguposter.posts.panel.repos

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database

private val ChatIdToMessageSerializer = PairSerializer(ChatId.serializer(), MessageIdentifier.serializer())

fun PostsMessages(
    database: Database,
    json: Json
): KeyValueRepo<PostId, Pair<ChatId, MessageIdentifier>> = ExposedKeyValueRepo<String, String>(
    database,
    { text("postId") },
    { text("chatToMessage") },
    "panel_messages_info"
).withMapper(
    { string },
    { json.encodeToString(ChatIdToMessageSerializer, this) },
    { PostId(this) },
    { json.decodeFromString(ChatIdToMessageSerializer, this) }
)
