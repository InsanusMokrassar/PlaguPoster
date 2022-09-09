package dev.inmo.plaguposter.posts.exposed

import com.benasher44.uuid.uuid4
import com.soywiz.klock.DateTime
import dev.inmo.micro_utils.repos.KeyValuesRepo
import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageIdentifier
import kotlinx.coroutines.flow.*
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
    val createdColumn = double("datetime").default(0.0)

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
                DateTime(get(createdColumn)),
                with(contentRepo) {
                    select { postIdColumn.eq(id.string) }.map {
                        it.asObject
                    }
                }
            )
        }

    private val _removedPostsFlow = MutableSharedFlow<RegisteredPost>()
    override val removedPostsFlow: Flow<RegisteredPost> = _removedPostsFlow.asSharedFlow()

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
            DateTime(get(createdColumn)),
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

    override fun insert(value: NewPost, it: InsertStatement<Number>) {
        it[createdColumn] = DateTime.now().unixMillis
    }

    override suspend fun deleteById(ids: List<PostId>) {
        onBeforeDelete(ids)
        val posts = ids.mapNotNull {
            getById(it)
        }.associateBy { it.id }
        val existsIds = posts.keys.toList()
        transaction(db = database) {
            val deleted = deleteWhere(null, null) {
                selectByIds(existsIds)
            }
            with(contentRepo) {
                deleteWhere {
                    postIdColumn.inList(existsIds.map { it.string })
                }
            }
            if (deleted == existsIds.size) {
                existsIds
            } else {
                existsIds.filter {
                    select { selectById(it) }.limit(1).none()
                }
            }
        }.forEach {
            _deletedObjectsIdsFlow.emit(it)
            _removedPostsFlow.emit(posts[it] ?: return@forEach)
        }
    }

    override suspend fun getIdByChatAndMessage(chatId: ChatId, messageId: MessageIdentifier): PostId? {
        return transaction(database) {
            with(contentRepo) {
                select { chatIdColumn.eq(chatId.chatId).and(messageIdColumn.eq(messageId)) }.limit(1).firstOrNull() ?.get(postIdColumn)
            } ?.let(::PostId)
        }
    }

    override suspend fun getPostCreationTime(postId: PostId): DateTime? = transaction(database) {
        select { selectById(postId) }.limit(1).firstOrNull() ?.get(createdColumn) ?.let(::DateTime)
    }
}
