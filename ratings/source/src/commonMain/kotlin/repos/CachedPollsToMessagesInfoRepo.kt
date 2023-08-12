package dev.inmo.plaguposter.ratings.source.repos

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.cache.cache.FullKVCache
import dev.inmo.micro_utils.repos.cache.full.fullyCached
import dev.inmo.plaguposter.common.ShortMessageInfo
import dev.inmo.tgbotapi.types.PollIdentifier
import kotlinx.coroutines.CoroutineScope

class CachedPollsToMessagesInfoRepo(
    private val repo: PollsToMessagesInfoRepo,
    private val scope: CoroutineScope,
    private val kvCache: FullKVCache<PollIdentifier, ShortMessageInfo> = FullKVCache()
) : PollsToMessagesInfoRepo, KeyValueRepo<PollIdentifier, ShortMessageInfo> by repo.fullyCached(kvCache, scope)
