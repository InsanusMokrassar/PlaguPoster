package dev.inmo.plaguposter.ratings.repo

import dev.inmo.micro_utils.repos.ReadKeyValueRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating

interface ReadRatingsRepo : ReadKeyValueRepo<PostId, Rating> {
    suspend fun getPosts(
        range: ClosedRange<Rating>,
        reversed: Boolean = false,
        count: Int? = null,
        exclude: List<PostId> = emptyList()
    ): Map<PostId, Rating>

    suspend fun getPostsWithRatingGreaterEq(
        then: Rating,
        reversed: Boolean = false,
        count: Int? = null,
        exclude: List<PostId> = emptyList()
    ): Map<PostId, Rating>
    suspend fun getPostsWithRatingLessEq(
        then: Rating,
        reversed: Boolean = false,
        count: Int? = null,
        exclude: List<PostId> = emptyList()
    ): Map<PostId, Rating>
}
