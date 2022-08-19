package dev.inmo.plaguposter.posts.exposed

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.repos.KeyValuesRepo
import dev.inmo.micro_utils.repos.exposed.*
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.tgbotapi.types.ChatId
import org.jetbrains.exposed.sql.*

internal class ExposedContentInfoRepo(
    override val database: Database,
    postIdColumnReference: Column<String>
) : ExposedRepo, Table(name = "posts_content") {
    val postIdColumn = (text("post_id") references postIdColumnReference).index()
    val chatIdColumn = long("chat_id")
    val messageIdColumn = long("message_id")
    val groupColumn = text("group").nullable()
    val orderColumn = integer("order")

    val ResultRow.asObject
        get() = PostContentInfo(
            ChatId(get(chatIdColumn)),
            get(messageIdColumn),
            get(groupColumn),
            get(orderColumn)
        )

    init {
        initTable()
    }
}
