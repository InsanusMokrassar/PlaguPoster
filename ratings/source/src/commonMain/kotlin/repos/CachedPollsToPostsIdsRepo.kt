package dev.inmo.plaguposter.ratings.source.repos

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.cache.FullKVCache
import dev.inmo.micro_utils.repos.cache.full.fullyCached
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.types.PollId
import kotlinx.coroutines.CoroutineScope

class CachedPollsToPostsIdsRepo(
    private val repo: PollsToPostsIdsRepo,
    private val scope: CoroutineScope,
    private val kvCache: KeyValueRepo<PollId, PostId> = MapKeyValueRepo()
) : PollsToPostsIdsRepo, KeyValueRepo<PollId, PostId> by repo.fullyCached(kvCache, scope)
