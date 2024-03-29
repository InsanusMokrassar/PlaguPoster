package dev.inmo.plaguposter.ratings.selector

import dev.inmo.micro_utils.repos.KeyValueRepo
import korlibs.time.DateTime
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.ratings.selector.models.SelectorConfig

class DefaultSelector (
    private val config: SelectorConfig,
    private val ratingsRepo: RatingsRepo,
    private val postsRepo: PostsRepo,
    private val latestChosenRepo: KeyValueRepo<PostId, DateTime>
) : Selector {
    override suspend fun take(n: Int, now: DateTime, exclude: List<PostId>): List<PostId> {
        val result = mutableListOf<PostId>()

        do {
            val selected = config.active(now.time) ?.rating ?.select(ratingsRepo, postsRepo, result + exclude, now, latestChosenRepo) ?: break
            result.add(selected)
        } while (result.size < n)

        return result.toList()
    }
}
