package dev.inmo.plaguposter.ratings.source.repos

import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.micro_utils.repos.exposed.keyvalue.AbstractExposedKeyValueRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.types.PollId
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.*

class ExposedPollsToPostsIdsRepo(
    database: Database
) : PollsToPostsIdsRepo, AbstractExposedKeyValueRepo<PollId, PostId>(database, "polls_to_posts") {
    override val keyColumn = text("poll_id")
    val postIdColumn = text("postId")
    override val selectById: ISqlExpressionBuilder.(PollId) -> Op<Boolean> = { keyColumn.eq(it.string) }
    override val selectByValue: ISqlExpressionBuilder.(PostId) -> Op<Boolean> = { postIdColumn.eq(it.string) }
    override val ResultRow.asKey: PollId
        get() = PollId(get(keyColumn))
    override val ResultRow.asObject: PostId
        get() = get(postIdColumn).let(::PostId)

    init {
        initTable()
    }

    override fun update(k: PollId, v: PostId, it: UpdateBuilder<Int>) {
        it[postIdColumn] = v.string
    }

    override fun insertKey(k: PollId, v: PostId, it: InsertStatement<Number>) {
        it[keyColumn] = k.string
    }
}
