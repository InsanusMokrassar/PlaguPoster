package dev.inmo.plaguposter.triggers.timer.disablers.ratings

import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.repos.unset
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.triggers.timer.TimersRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        singleWithRandomQualifier(createdAtStart = true) {
            val timersRepo = get<TimersRepo>()
            val ratingsRepo = get<RatingsRepo>()
            val scope = get<CoroutineScope>()

            timersRepo.onNewValue.subscribeSafelyWithoutExceptions(scope) {
                ratingsRepo.unset(it.first)
            }
        }
    }
}
