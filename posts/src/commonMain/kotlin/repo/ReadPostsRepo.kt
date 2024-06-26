package dev.inmo.plaguposter.posts.repo

import korlibs.time.DateTime
import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.MessageId

interface ReadPostsRepo : ReadCRUDRepo<RegisteredPost, PostId> {
    suspend fun getIdByChatAndMessage(chatId: IdChatIdentifier, messageId: MessageId): PostId?
    suspend fun getPostCreationTime(postId: PostId): DateTime?
    suspend fun getFirstMessageInfo(postId: PostId): PostContentInfo?
}
