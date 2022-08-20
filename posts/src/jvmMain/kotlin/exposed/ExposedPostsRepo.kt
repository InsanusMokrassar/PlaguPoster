package dev.inmo.plaguposter.posts.exposed

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.repos.KeyValuesRepo
import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedPostsRepo(
    override val database: Database
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

    override suspend fun deleteById(ids: List<PostId>) {
        onBeforeDelete(ids)
        transaction(db = database) {
            val deleted = deleteWhere(null, null) {
                selectByIds(ids)
            }
            with(contentRepo) {
                deleteWhere {
                    postIdColumn.inList(ids.map { it.string })
                }
            }
            if (deleted == ids.size) {
                ids
            } else {
                ids.filter {
                    select { selectById(it) }.limit(1).none()
                }
            }
        }.forEach {
            _deletedObjectsIdsFlow.emit(it)
        }
    }

    override suspend fun getIdByChatAndMessage(chatId: ChatId, messageId: MessageIdentifier): PostId? {
        return transaction(database) {
            with(contentRepo) {
                select { chatIdColumn.eq(chatId.chatId).and(messageIdColumn.eq(messageId)) }.limit(1).firstOrNull() ?.get(postIdColumn)
            } ?.let(::PostId)
        }
    }
}