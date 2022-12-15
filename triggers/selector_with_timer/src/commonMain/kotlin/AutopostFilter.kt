package dev.inmo.plaguposter.triggers.selector_with_timer

import com.soywiz.klock.DateTime
import dev.inmo.plaguposter.posts.models.PostId

fun interface AutopostFilter {
    suspend fun check(postId: PostId, dateTime: DateTime): Boolean
}
