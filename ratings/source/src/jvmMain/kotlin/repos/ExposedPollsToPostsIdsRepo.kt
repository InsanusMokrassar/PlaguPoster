package dev.inmo.plaguposter.ratings.source.repos

import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.micro_utils.repos.exposed.keyvalue.AbstractExposedKeyValueRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.types.PollIdentifier
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement

class ExposedPollsToPostsIdsRepo(
    database: Database
) : PollsToPostsIdsRepo, AbstractExposedKeyValueRepo<PollIdentifier, PostId>(database, "polls_to_posts") {
    override val keyColumn = text("poll_id")
    val postIdColumn = text("postId")
    override val selectById: SqlExpressionBuilder.(PollIdentifier) -> Op<Boolean> = { keyColumn.eq(it) }
    override val selectByValue: SqlExpressionBuilder.(PostId) -> Op<Boolean> = { postIdColumn.eq(it.string) }
    override val ResultRow.asKey: PollIdentifier
        get() = get(keyColumn)
    override val ResultRow.asObject: PostId
        get() = get(postIdColumn).let(::PostId)

    init {
        initTable()
    }

    override fun update(k: PollIdentifier, v: PostId, it: UpdateStatement) {
        it[postIdColumn] = v.string
    }

    override fun insert(k: PollIdentifier, v: PostId, it: InsertStatement<Number>) {
        it[keyColumn] = k
        it[postIdColumn] = v.string
    }
}
