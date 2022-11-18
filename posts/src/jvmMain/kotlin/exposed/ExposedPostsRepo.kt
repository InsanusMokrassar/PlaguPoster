package dev.inmo.plaguposter.posts.exposed

import com.benasher44.uuid.uuid4
import com.soywiz.klock.DateTime
import dev.inmo.micro_utils.repos.KeyValuesRepo
import dev.inmo.micro_utils.repos.UpdatedValuePair
import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import dev.inmo.plaguposter.posts.models.*
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.MessageIdentifier
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.statements.*
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedPostsRepo(
    override val database: Database
) : PostsRepo, AbstractExposedCRUDRepo<RegisteredPost, PostId, NewPost>(
    tableName = "posts"
) {
    val idColumn = text("id")
    val createdColumn = double("datetime").default(0.0)

    private val contentRepo by lazy {
        ExposedContentInfoRepo(
            database,
            idColumn
        )
    }

    override val primaryKey: PrimaryKey = PrimaryKey(idColumn)

    override val selectById: ISqlExpressionBuilder.(PostId) -> Op<Boolean> = { idColumn.eq(it.string) }
    override val selectByIds: ISqlExpressionBuilder.(List<PostId>) -> Op<Boolean> = { idColumn.inList(it.map { it.string }) }
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

    override fun createAndInsertId(value: NewPost, it: InsertStatement<Number>): PostId {
        val id = PostId(uuid4().toString())
        it[idColumn] = id.string
        return id
    }

    override fun update(id: PostId?, value: NewPost, it: UpdateBuilder<Int>) {}

    private fun updateContent(post: RegisteredPost) {
        transaction(database) {
            with(contentRepo) {
                deleteWhere { postIdColumn.eq(post.id.string) }
                post.content.forEach { contentInfo ->
                    insert {
                        it[postIdColumn] = post.id.string
                        it[chatIdColumn] = contentInfo.chatId.chatId
                        it[threadIdColumn] = contentInfo.chatId.threadId
                        it[messageIdColumn] = contentInfo.messageId
                        it[groupColumn] = contentInfo.group
                        it[orderColumn] = contentInfo.order
                    }
                }
            }
        }
    }

    override fun insert(value: NewPost, it: InsertStatement<Number>) {
        super.insert(value, it)
        it[createdColumn] = DateTime.now().unixMillis
    }

    override suspend fun onAfterCreate(values: List<Pair<NewPost, RegisteredPost>>): List<RegisteredPost> {
        return values.map {
            val actual = it.second.copy(content = it.first.content)
            updateContent(actual)
            actual
        }
    }

    override suspend fun onAfterUpdate(value: List<UpdatedValuePair<NewPost, RegisteredPost>>): List<RegisteredPost> {
        return value.map {
            val actual = it.second.copy(content = it.first.content)
            updateContent(actual)
            actual
        }
    }

    override suspend fun deleteById(ids: List<PostId>) {
        onBeforeDelete(ids)
        val posts = ids.mapNotNull {
            getById(it)
        }.associateBy { it.id }
        val existsIds = posts.keys.toList()
        transaction(db = database) {
            val deleted = deleteWhere(null, null) {
                selectByIds(it, existsIds)
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

    override suspend fun getIdByChatAndMessage(chatId: IdChatIdentifier, messageId: MessageIdentifier): PostId? {
        return transaction(database) {
            with(contentRepo) {
                select { chatIdColumn.eq(chatId.chatId).and(threadIdColumn.eq(chatId.threadId)).and(messageIdColumn.eq(messageId)) }.limit(1).firstOrNull() ?.get(postIdColumn)
            } ?.let(::PostId)
        }
    }

    override suspend fun getPostCreationTime(postId: PostId): DateTime? = transaction(database) {
        select { selectById(postId) }.limit(1).firstOrNull() ?.get(createdColumn) ?.let(::DateTime)
    }
}
