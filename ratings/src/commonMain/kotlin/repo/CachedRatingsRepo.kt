package dev.inmo.plaguposter.ratings.repo

import dev.inmo.micro_utils.pagination.utils.doForAllWithNextPaging
import dev.inmo.micro_utils.pagination.utils.optionallyReverse
import dev.inmo.micro_utils.pagination.utils.paginate
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.full.FullKeyValueCacheRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating
import kotlinx.coroutines.CoroutineScope

class CachedRatingsRepo(
    private val base: RatingsRepo,
    private val scope: CoroutineScope,
    private val kvCache: MapKeyValueRepo<PostId, Rating> = MapKeyValueRepo()
) : RatingsRepo, KeyValueRepo<PostId, Rating> by FullKeyValueCacheRepo(base, kvCache, scope) {
    private suspend fun getPosts(
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>,
        ratingFilter: (Rating) -> Boolean
    ): Map<PostId, Rating> {
        return kvCache.getAll().filter { (it, rating) ->
            it !in exclude && ratingFilter(rating)
        }.let {
            if (count == null) {
                it
            } else {
                val keys = it.keys.optionallyReverse(reversed).take(count)
                keys.associateWith { id -> it.getValue(id) }
            }
        }
    }
    override suspend fun getPosts(
        range: ClosedRange<Rating>,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ): Map<PostId, Rating> = getPosts(reversed, count, exclude) {
        it in range
    }

    override suspend fun getPostsWithRatingGreaterEq(
        then: Rating,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ): Map<PostId, Rating> = getPosts(reversed, count, exclude) {
        it >= then
    }

    override suspend fun getPostsWithRatingLessEq(
        then: Rating,
        reversed: Boolean,
        count: Int?,
        exclude: List<PostId>
    ): Map<PostId, Rating> = getPosts(reversed, count, exclude) {
        it <= then
    }
}
