package dev.inmo.plaguposter.ratings.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Rating(
    val double: Double
) : Comparable<Rating> {
    override fun compareTo(other: Rating): Int = double.compareTo(other.double)
}
