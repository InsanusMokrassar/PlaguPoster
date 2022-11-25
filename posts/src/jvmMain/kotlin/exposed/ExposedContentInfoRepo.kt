package dev.inmo.plaguposter.posts.exposed

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.repos.KeyValuesRepo
import dev.inmo.micro_utils.repos.exposed.*
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import org.jetbrains.exposed.sql.*

internal class ExposedContentInfoRepo(
    override val database: Database,
    postIdColumnReference: Column<String>
) : ExposedRepo, Table(name = "posts_content") {
    val postIdColumn = text("post_id").references(postIdColumnReference, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val chatIdColumn = long("chat_id")
    val threadIdColumn = long("thread_id").nullable().default(null)
    val messageIdColumn = long("message_id")
    val groupColumn = text("group").nullable()
    val orderColumn = integer("order")

    val ResultRow.asObject
        get() = PostContentInfo(
            IdChatIdentifier(get(chatIdColumn), get(threadIdColumn)),
            get(messageIdColumn),
            get(groupColumn),
            get(orderColumn)
        )

    init {
        initTable()
    }
}
