package dev.inmo.plaguposter.ratings.source.repos

import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.micro_utils.repos.exposed.keyvalue.AbstractExposedKeyValueRepo
import dev.inmo.plaguposter.common.ShortMessageInfo
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.PollIdentifier
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.*

class ExposedPollsToMessagesInfoRepo(
    database: Database
) : PollsToMessagesInfoRepo, AbstractExposedKeyValueRepo<PollIdentifier, ShortMessageInfo>(
    database,
    "polls_to_their_messages_info"
) {
    override val keyColumn = text("poll_id")
    private val chatIdColumn = long("chat_id")
    private val threadIdColumn = long("thread_id").nullable().default(null)
    private val messageIdColumn = long("message_id")
    override val selectById: ISqlExpressionBuilder.(PollIdentifier) -> Op<Boolean> = { keyColumn.eq(it) }
    override val selectByValue: ISqlExpressionBuilder.(ShortMessageInfo) -> Op<Boolean> = {
        chatIdColumn.eq(it.chatId.chatId).and(threadIdColumn.eq(it.chatId.threadId)).and(
            messageIdColumn.eq(it.messageId)
        )
    }
    override val ResultRow.asKey: PollIdentifier
        get() = get(keyColumn)
    override val ResultRow.asObject: ShortMessageInfo
        get() = ShortMessageInfo(
            IdChatIdentifier(get(chatIdColumn), get(threadIdColumn)),
            get(messageIdColumn)
        )

    init {
        initTable()
    }

    override fun update(k: PollIdentifier, v: ShortMessageInfo, it: UpdateBuilder<Int>) {
        it[chatIdColumn] = v.chatId.chatId
        it[threadIdColumn] = v.chatId.threadId
        it[messageIdColumn] = v.messageId
    }

    override fun insertKey(k: PollIdentifier, v: ShortMessageInfo, it: InsertStatement<Number>) {
        it[keyColumn] = k
    }
}
