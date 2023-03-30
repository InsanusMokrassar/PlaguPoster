package dev.inmo.plaguposter.ratings.selector

import com.soywiz.klock.DateTime
import dev.inmo.plaguposter.posts.models.PostId

interface Selector {
    suspend fun take(n: Int = 1, now: DateTime = DateTime.now(), exclude: List<PostId> = emptyList()): List<PostId>
}
