package dev.inmo.plaguposter.triggers.timer

import korlibs.time.DateTime
import dev.inmo.micro_utils.coroutines.launchSafelyWithoutExceptions
import dev.inmo.micro_utils.coroutines.plus
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.repos.unset
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.sending.PostPublisher
import korlibs.time.millisecondsLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TimersHandler(
    private val timersRepo: TimersRepo,
    private val publisher: PostPublisher,
    private val scope: CoroutineScope
) {
    private var currentPostAndJob: Pair<PostId, Job>? = null
    private val currentJobMutex = Mutex()

    init {
        (flowOf(Unit) + timersRepo.onNewValue + timersRepo.onValueRemoved).subscribeSafelyWithoutExceptions(scope) {
            refreshPublishingJob()
        }
    }

    private suspend fun refreshPublishingJob() {
        val minimal = timersRepo.getMinimalDateTimePost()

        currentJobMutex.withLock {
            if (minimal ?.first == currentPostAndJob ?.first) {
                return@withLock
            }

            currentPostAndJob ?.second ?.cancel()

            currentPostAndJob = minimal ?.let { (postId, dateTime) ->
                postId to scope.launchSafelyWithoutExceptions {
                    val now = DateTime.now()
                    val span = dateTime - now

                    delay(span.millisecondsLong)

                    publisher.publish(postId)

                    timersRepo.unset(postId)

                    refreshPublishingJob()
                }
            }
        }
    }
}
