package dev.inmo.plaguposter.triggers.timer.repo

import korlibs.time.DateTime
import dev.inmo.micro_utils.common.firstNotNull
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.pagination.paginate
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.micro_utils.repos.exposed.keyvalue.AbstractExposedKeyValueRepo
import dev.inmo.micro_utils.repos.unset
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.triggers.timer.TimersRepo
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ISqlExpressionBuilder
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedTimersRepo(
    database: Database,
    postsRepo: PostsRepo,
    scope: CoroutineScope
) : TimersRepo, AbstractExposedKeyValueRepo<PostId, DateTime>(
    database,
    "timers"
) {
    override val keyColumn = text("post_id")
    private val dateTimeColumn = long("date_time")
    override val selectById: ISqlExpressionBuilder.(PostId) -> Op<Boolean> = { keyColumn.eq(it.string) }
    override val selectByValue: ISqlExpressionBuilder.(DateTime) -> Op<Boolean> = { dateTimeColumn.eq(it.unixMillisLong) }
    override val ResultRow.asKey: PostId
        get() = PostId(get(keyColumn))
    override val ResultRow.asObject: DateTime
        get() = DateTime(get(dateTimeColumn))

    val postsRepoListeningJob = postsRepo.deletedObjectsIdsFlow.subscribeSafelyWithoutExceptions(scope) {
        unset(it)
    }

    init {
        initTable()
    }

    override fun update(k: PostId, v: DateTime, it: UpdateBuilder<Int>) {
        it[dateTimeColumn] = v.unixMillisLong
    }

    override fun insertKey(k: PostId, v: DateTime, it: UpdateBuilder<Int>) {
        it[keyColumn] = k.string
    }

    override suspend fun getMinimalDateTimePost(): Pair<PostId, DateTime>? = transaction(database) {
        selectAll().orderBy(dateTimeColumn).limit(1).firstOrNull() ?.let {
            PostId(it[keyColumn]) to DateTime(it[dateTimeColumn])
        }
    }
}
