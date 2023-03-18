package dev.inmo.plaguposter.ratings.selector

import com.soywiz.klock.DateTime
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.ratings.selector.models.SelectorConfig

class DefaultSelector (
    private val config: SelectorConfig,
    private val ratingsRepo: RatingsRepo,
    private val postsRepo: PostsRepo
) : Selector {
    override suspend fun take(n: Int, now: DateTime, exclude: List<PostId>): List<PostId> {
        val result = mutableListOf<PostId>()

        do {
            val selected = config.active(now.time) ?.rating ?.select(ratingsRepo, postsRepo, result + exclude, now) ?: break
            result.add(selected)
        } while (result.size < n)

        return result.toList()
    }
}
