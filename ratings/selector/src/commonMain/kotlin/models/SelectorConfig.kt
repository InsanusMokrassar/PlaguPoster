package dev.inmo.plaguposter.ratings.selector.models

import korlibs.time.DateTime
import korlibs.time.Time
import kotlinx.serialization.Serializable

@Serializable
data class SelectorConfig(
    val items: List<SelectorConfigItem>
) {
    fun active(now: Time = DateTime.now().time) = items.firstOrNull { it.time.isActive(now) }
}
