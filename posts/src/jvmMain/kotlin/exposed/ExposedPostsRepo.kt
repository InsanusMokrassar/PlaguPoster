package dev.inmo.plaguposter.posts.exposed

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.repos.KeyValuesRepo
import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.plaguposter.posts.repo.PostsRepo
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement

class ExposedPostsRepo(
    override val database: Database,
    json: Json
) : PostsRepo, AbstractExposedCRUDRepo<RegisteredPost, PostId, NewPost>(
    tableName = "posts"
) {
    val idColumn = text("id").clientDefault { uuid4().toString() }

    private val contentRepo by lazy {
        ExposedContentInfoRepo(
            database,
            idColumn
        )
    }

    override val primaryKey: PrimaryKey = PrimaryKey(idColumn)

    override val selectById: SqlExpressionBuilder.(PostId) -> Op<Boolean> = { idColumn.eq(it.string) }
    override val selectByIds: SqlExpressionBuilder.(List<PostId>) -> Op<Boolean> = { idColumn.inList(it.map { it.string }) }
    override val ResultRow.asObject: RegisteredPost
        get() {
            val id = PostId(get(idColumn))
            return RegisteredPost(
                id,
                with(contentRepo) {
                    select { postIdColumn.eq(id.string) }.map {
                        it.asObject
                    }
                }
            )
        }

    init {
        initTable()
    }

    override fun InsertStatement<Number>.asObject(value: NewPost): RegisteredPost {
        val id = PostId(get(idColumn))

        with(contentRepo) {
            value.content.forEach { contentInfo ->
                insert {
                    it[postIdColumn] = id.string
                    it[chatIdColumn] = contentInfo.chatId.chatId
                    it[messageIdColumn] = contentInfo.messageId
                    it[groupColumn] = contentInfo.group
                    it[orderColumn] = contentInfo.order
                }
            }
        }

        return RegisteredPost(
            id,
            with(contentRepo) {
                select { postIdColumn.eq(id.string) }.map {
                    it.asObject
                }
            }
        )
    }

    override fun update(id: PostId, value: NewPost, it: UpdateStatement) {
        with(contentRepo) {
            deleteWhere { postIdColumn.eq(id.string) }
            value.content.forEach { contentInfo ->
                insert {
                    it[postIdColumn] = id.string
                    it[chatIdColumn] = contentInfo.chatId.chatId
                    it[messageIdColumn] = contentInfo.messageId
                    it[groupColumn] = contentInfo.group
                    it[orderColumn] = contentInfo.order
                }
            }
        }
    }

    override fun insert(value: NewPost, it: InsertStatement<Number>) {}
}
