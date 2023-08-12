package dev.inmo.plaguposter.posts.cached

import korlibs.time.DateTime
import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.micro_utils.pagination.firstPageWithOneElementPagination
import dev.inmo.micro_utils.pagination.utils.doForAllWithNextPaging
import dev.inmo.micro_utils.repos.CRUDRepo
import dev.inmo.micro_utils.repos.cache.cache.FullKVCache
import dev.inmo.micro_utils.repos.cache.full.FullCRUDCacheRepo
import dev.inmo.plaguposter.posts.models.NewPost
import dev.inmo.plaguposter.posts.models.PostContentInfo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.models.RegisteredPost
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.MessageIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class CachedPostsRepo(
    private val parentRepo: PostsRepo,
    private val scope: CoroutineScope,
    private val kvCache: FullKVCache<PostId, RegisteredPost> = FullKVCache()
) : PostsRepo, CRUDRepo<RegisteredPost, PostId, NewPost> by FullCRUDCacheRepo(
    parentRepo,
    kvCache,
    scope,
    skipStartInvalidate = false,
    { it.id }
) {
    override val removedPostsFlow: Flow<RegisteredPost> by parentRepo::removedPostsFlow

    override suspend fun getIdByChatAndMessage(chatId: IdChatIdentifier, messageId: MessageIdentifier): PostId? {
        doForAllWithNextPaging(firstPageWithOneElementPagination) {
            kvCache.values(it).also {
                it.results.forEach {
                    return it.takeIf {
                        it.content.any { it.chatId == chatId && it.messageId == messageId }
                    } ?.id ?: return@forEach
                }
            }
        }

        return null
    }

    override suspend fun getPostCreationTime(postId: PostId): DateTime? = getById(postId) ?.created

    override suspend fun getFirstMessageInfo(postId: PostId): PostContentInfo? = getById(postId) ?.content ?.firstOrNull()
}
