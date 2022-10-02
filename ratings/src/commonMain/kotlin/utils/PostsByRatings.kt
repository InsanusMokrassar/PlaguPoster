package dev.inmo.plaguposter.ratings.utils

import dev.inmo.micro_utils.pagination.utils.getAll
import dev.inmo.micro_utils.repos.pagination.getAll
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo

suspend fun RatingsRepo.postsByRatings(): Map<Rating, List<PostId>> {
    return getAll { keys(it) }.groupBy {
        it.second
    }.map {
        it.key to it.value.map { it.first }
    }.toMap()
}
