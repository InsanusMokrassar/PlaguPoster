package dev.inmo.plaguposter.ratings.source.models

import dev.inmo.plaguposter.ratings.models.Rating

fun interface VariantTransformer {
    operator fun invoke(from: String): Rating?
}
