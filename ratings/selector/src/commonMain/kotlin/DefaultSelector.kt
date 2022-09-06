package dev.inmo.plaguposter.ratings.selector

import com.soywiz.klock.DateTime
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.ratings.selector.models.SelectorConfig

class DefaultSelector (
    private val config: SelectorConfig,
    private val repo: RatingsRepo
) : Selector {
    override suspend fun take(n: Int, now: DateTime): List<PostId> {
        val result = mutableListOf<PostId>()

        do {
            val selected = config.active(now.time) ?.ratings ?.select(repo, result) ?: break
            result.add(selected)
        } while (result.size < n)

        return result.toList()
    }
}
