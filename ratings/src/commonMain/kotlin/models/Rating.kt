package dev.inmo.plaguposter.ratings.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Rating(
    val double: Double
)
