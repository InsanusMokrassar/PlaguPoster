package dev.inmo.plaguposter.ratings.selector

import dev.inmo.plaguposter.posts.models.PostId

interface Selector {
    suspend fun take(n: Int = 1): List<PostId>
}
