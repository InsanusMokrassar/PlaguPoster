package dev.inmo.plaguposter.ratings.source.repos

import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.micro_utils.repos.exposed.keyvalue.AbstractExposedKeyValueRepo
import dev.inmo.plaguposter.common.ShortMessageInfo
import dev.inmo.tgbotapi.types.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.statements.*

class ExposedPollsToMessagesInfoRepo(
    database: Database
) : PollsToMessagesInfoRepo, AbstractExposedKeyValueRepo<PollId, ShortMessageInfo>(
    database,
    "polls_to_their_messages_info"
) {
    override val keyColumn = text("poll_id")
    private val chatIdColumn = long("chat_id")
    private val threadIdColumn = long("thread_id").nullable().default(null)
    private val messageIdColumn = long("message_id")
    override val selectById: ISqlExpressionBuilder.(PollId) -> Op<Boolean> = { keyColumn.eq(it.string) }
    override val selectByValue: ISqlExpressionBuilder.(ShortMessageInfo) -> Op<Boolean> = {
        chatIdColumn.eq(it.chatId.chatId.long)
            .and(it.chatId.threadId?.let { threadIdColumn.eq(it.long) } ?: threadIdColumn.isNull()).and(
            messageIdColumn.eq(it.messageId.long)
        )
    }
    override val ResultRow.asKey: PollId
        get() = PollId(get(keyColumn))
    override val ResultRow.asObject: ShortMessageInfo
        get() = ShortMessageInfo(
            IdChatIdentifier(RawChatId(get(chatIdColumn)), get(threadIdColumn) ?.let(::MessageThreadId)),
            MessageId(get(messageIdColumn))
        )

    init {
        initTable()
    }

    override fun update(k: PollId, v: ShortMessageInfo, it: UpdateBuilder<Int>) {
        it[chatIdColumn] = v.chatId.chatId.long
        it[threadIdColumn] = v.chatId.threadId ?.long
        it[messageIdColumn] = v.messageId.long
    }

    override fun insertKey(k: PollId, v: ShortMessageInfo, it: UpdateBuilder<Int>) {
        it[keyColumn] = k.string
    }
}
