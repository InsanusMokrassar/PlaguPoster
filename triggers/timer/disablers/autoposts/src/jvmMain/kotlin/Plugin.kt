package dev.inmo.plaguposter.triggers.timer.disablers.autoposts

import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.koin.singleWithRandomQualifierAndBinds
import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.micro_utils.repos.unset
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.triggers.selector_with_timer.AutopostFilter
import dev.inmo.plaguposter.triggers.timer.TimersRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module

object Plugin : Plugin {
    override fun Module.setupDI(database: Database, params: JsonObject) {
        singleWithRandomQualifier<AutopostFilter> {
            val timersRepo = get<TimersRepo>()
            AutopostFilter { _, dateTime ->
                val result = timersRepo.keys(dateTime, FirstPagePagination(1))
                result.results.isEmpty()
            }
        }
    }
}
