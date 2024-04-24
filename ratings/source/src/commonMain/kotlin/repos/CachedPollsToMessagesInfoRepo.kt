package dev.inmo.plaguposter.ratings.source.repos

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.micro_utils.repos.cache.cache.FullKVCache
import dev.inmo.micro_utils.repos.cache.full.fullyCached
import dev.inmo.plaguposter.common.ShortMessageInfo
import dev.inmo.tgbotapi.types.PollId
import kotlinx.coroutines.CoroutineScope

class CachedPollsToMessagesInfoRepo(
    private val repo: PollsToMessagesInfoRepo,
    private val scope: CoroutineScope,
    private val kvCache: KeyValueRepo<PollId, ShortMessageInfo> = MapKeyValueRepo()
) : PollsToMessagesInfoRepo, KeyValueRepo<PollId, ShortMessageInfo> by repo.fullyCached(kvCache, scope)
