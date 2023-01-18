package dev.inmo.plaguposter.ratings.repo

import dev.inmo.micro_utils.pagination.utils.doForAllWithNextPaging
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.cache.cache.FullKVCache
import dev.inmo.micro_utils.repos.cache.full.FullKeyValueCacheRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating
import kotlinx.coroutines.CoroutineScope

class CachedRatingsRepo(
    private val base: RatingsRepo,
    private val scope: CoroutineScope,
    private val kvCache: FullKVCache<PostId, Rating> = FullKVCache()
) : RatingsRepo, KeyValueRepo<PostId, Rating> by FullKeyValueCacheRepo(base, kvCache, scope) {
    override suspend fun getPosts(
        range: ClosedRange<Rating>,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ): Map<PostId, Rating> {
        val result = mutableMapOf<PostId, Rating>()

        doForAllWithNextPaging {
            kvCache.keys(it).also {
                it.results.forEach {
                    val rating = kvCache.get(it) ?: return@forEach
                    if (it !in exclude && rating in range) {
                        result[it] = rating
                    }
                }
            }
        }

        return result.toMap()
    }

    override suspend fun getPostsWithRatingGreaterEq(
        then: Rating,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ): Map<PostId, Rating> = getPosts(
        then .. Rating(Double.MAX_VALUE),
        reversed,
        count,
        exclude
    )

    override suspend fun getPostsWithRatingLessEq(
        then: Rating,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ): Map<PostId, Rating> = getPosts(
        Rating(Double.MIN_VALUE) .. then,
        reversed,
        count,
        exclude
    )
}
