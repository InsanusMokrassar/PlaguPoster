package dev.inmo.plaguposter.ratings.selector.models

import kotlinx.serialization.Serializable

@Serializable
data class SelectorConfig(
    val items: List<SelectorConfigItem>
) {
    val active: SelectorConfigItem?
        get() = items.firstOrNull { it.time.isActive }
}
