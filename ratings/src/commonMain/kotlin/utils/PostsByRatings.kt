package dev.inmo.plaguposter.ratings.utils

import dev.inmo.micro_utils.pagination.utils.getAll
import dev.inmo.plaguposter.ratings.repo.RatingsRepo

suspend fun RatingsRepo.postsByRatings() {
    getAll()
}
