package dev.inmo.plaguposter.ratings.exposed

import dev.inmo.micro_utils.pagination.utils.optionallyReverse
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.micro_utils.repos.exposed.keyvalue.AbstractExposedKeyValueRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.*
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedRatingsRepo (
    database: Database
) : RatingsRepo, AbstractExposedKeyValueRepo<PostId, Rating>(
    database,
    "ratings"
) {
    override val keyColumn = text("post_id")
    val ratingsColumn = double("rating")
    override val selectById: ISqlExpressionBuilder.(PostId) -> Op<Boolean> = { keyColumn.eq(it.string) }
    override val selectByValue: ISqlExpressionBuilder.(Rating) -> Op<Boolean> = { ratingsColumn.eq(it.double) }
    override val ResultRow.asKey: PostId
        get() = get(keyColumn).let(::PostId)
    override val ResultRow.asObject: Rating
        get() = get(ratingsColumn).let(::Rating)

    init {
        initTable()
    }

    override fun update(k: PostId, v: Rating, it: UpdateBuilder<Int>) {
        it[ratingsColumn] = v.double
    }

    override fun insertKey(k: PostId, v: Rating, it: InsertStatement<Number>) {
        it[keyColumn] = k.string
    }

    private fun Query.optionallyLimit(limit: Int?) = if (limit == null) {
        this
    } else {
        limit(limit)
    }

    override suspend fun getPosts(
        range: ClosedRange<Rating>,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ): Map<PostId, Rating> = transaction(database) {
        selectAll().where {
            ratingsColumn.greaterEq(range.start.double).and(
                ratingsColumn.lessEq(range.endInclusive.double)
            ).and(
                keyColumn.notInList(exclude.map { it.string })
            )
        }.optionallyLimit(count).optionallyReverse(reversed).map {
            it.asKey to it.asObject
        }
    }.toMap()

    override suspend fun getPostsWithRatingGreaterEq(
        then: Rating,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ) = transaction(database) {
        selectAll().where { ratingsColumn.greaterEq(then.double).and(keyColumn.notInList(exclude.map { it.string })) }.optionallyLimit(count).optionallyReverse(reversed).map {
            it.asKey to it.asObject
        }
    }.toMap()

    override suspend fun getPostsWithRatingLessEq(
        then: Rating,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ): Map<PostId, Rating> = transaction(database) {
        selectAll().where { ratingsColumn.lessEq(then.double).and(keyColumn.notInList(exclude.map { it.string })) }.optionallyLimit(count).optionallyReverse(reversed).map {
            it.asKey to it.asObject
        }
    }.toMap()
}
