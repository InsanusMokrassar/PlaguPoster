package dev.inmo.plaguposter.ratings.selector.models

import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.random.Random

@Serializable
data class RatingConfig(
    val min: Rating?,
    val max: Rating?,
    val prefer: Prefer
) {
    suspend fun select(repo: RatingsRepo, exclude: List<PostId>): PostId? {
        var reversed: Boolean = false
        var count: Int? = null

        when (prefer) {
            Prefer.Max -> {
                reversed = true
                count = 1
            }
            Prefer.Min -> {
                reversed = false
                count = 1
            }
            Prefer.Random -> {
                reversed = false
                count = null
            }
        }

        val posts = when(min) {
            null -> {
                when (max) {
                    null -> {
                        repo.keys(
                            count ?.let { Pagination(0, it) } ?: FirstPagePagination(repo.count().toInt()),
                            reversed
                        ).results.filterNot {
                            it in exclude
                        }
                    }
                    else -> {
                        repo.getPostsWithRatingLessEq(max, exclude = exclude).keys
                    }
                }
            }
            else -> {
                when (max) {
                    null -> {
                        repo.getPostsWithRatingGreaterEq(min, exclude = exclude).keys
                    }
                    else -> {
                        repo.getPosts(min .. max, reversed, count, exclude = exclude).keys
                    }
                }
            }
        }

        return when (prefer) {
            Prefer.Max,
            Prefer.Min -> posts.firstOrNull()
            Prefer.Random -> posts.randomOrNull()
        }
    }

    @Serializable(Prefer.Serializer::class)
    sealed interface Prefer {
        val type: String
        @Serializable(Serializer::class)
        object Max : Prefer { override val type: String = "max" }
        @Serializable(Serializer::class)
        object Min : Prefer { override val type: String = "min" }
        @Serializable(Serializer::class)
        object Random : Prefer { override val type: String = "random" }

        object Serializer : KSerializer<Prefer> {
            override val descriptor: SerialDescriptor = String.serializer().descriptor

            override fun deserialize(decoder: Decoder): Prefer {
                val identifier = decoder.decodeString().lowercase()
                return values.first { it.type.lowercase() == identifier }
            }

            override fun serialize(encoder: Encoder, value: Prefer) {
                encoder.encodeString(value.type.lowercase())
            }

        }

        companion object {
            val values = arrayOf(Max, Min, Random)
        }
    }
}
