package dev.inmo.plaguposter.posts.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier

interface ReadPostsRepo : ReadCRUDRepo<RegisteredPost, PostId> {
    suspend fun getIdByChatAndMessage(chatId: ChatId, messageId: MessageIdentifier): PostId?
}
