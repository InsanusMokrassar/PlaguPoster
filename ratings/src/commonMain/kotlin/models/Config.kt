package dev.inmo.plaguposter.ratings.models

import dev.inmo.krontab.EveryHourScheduler
import dev.inmo.krontab.KrontabTemplate
import dev.inmo.krontab.buildSchedule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class RatingsConfig(
    @SerialName("manualRecheckKrontab")
    val manualRecheckKrontabTemplate: KrontabTemplate = "0 /30 *"
) {
    @Transient
    val manualRecheckKrontab
        get() = buildSchedule(manualRecheckKrontabTemplate)
}
