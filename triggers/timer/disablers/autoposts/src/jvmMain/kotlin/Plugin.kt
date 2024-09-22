package dev.inmo.plaguposter.triggers.timer.disablers.autoposts

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.triggers.selector_with_timer.AutopostFilter
import dev.inmo.plaguposter.triggers.timer.TimersRepo
import kotlinx.serialization.json.*
import org.koin.core.module.Module

object Plugin : Plugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier<AutopostFilter> {
            val timersRepo = get<TimersRepo>()
            AutopostFilter { _, dateTime ->
                val result = timersRepo.keys(dateTime, FirstPagePagination(1))
                result.results.isEmpty()
            }
        }
    }
}
