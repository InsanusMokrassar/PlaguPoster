package dev.inmo.plaguposter.ratings.selector

import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.ratings.selector.models.SelectorConfig

class DefaultSelector (
    private val config: SelectorConfig,
    private val repo: RatingsRepo
) : Selector {
    override suspend fun take(n: Int): List<PostId> {
        val result = mutableListOf<PostId>()

        do {
            val selected = config.active ?.ratings ?.select(repo, result) ?: break
            result.add(selected)
        } while (result.size < n)

        return result.toList()
    }
}
