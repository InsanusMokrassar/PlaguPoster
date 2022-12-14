package dev.inmo.plaguposter.triggers.timer

import com.soywiz.klock.DateTime
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.plaguposter.posts.models.PostId

interface TimersRepo : KeyValueRepo<PostId, DateTime> {
    suspend fun getMinimalDateTimePost(): Pair<PostId, DateTime>?
}
