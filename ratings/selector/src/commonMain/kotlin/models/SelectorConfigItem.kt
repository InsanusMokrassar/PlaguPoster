package dev.inmo.plaguposter.ratings.selector.models

import kotlinx.serialization.Serializable

@Serializable
data class SelectorConfigItem(
    val time: TimeConfig,
    val rating: RatingConfig
)
