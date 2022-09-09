package dev.inmo.plaguposter.ratings.selector.models

import com.soywiz.klock.DateTime
import com.soywiz.klock.seconds
import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.utils.getAllByWithNextPaging
import dev.inmo.micro_utils.repos.pagination.getAll
import dev.inmo.plaguposter.common.DateTimeSerializer
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.tgbotapi.types.Seconds
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class RatingConfig(
    val min: Rating?,
    val max: Rating?,
    val prefer: Prefer,
    val otherwise: RatingConfig? = null,
    val postAge: Seconds? = null
) {
    suspend fun select(
        ratingsRepo: RatingsRepo,
        postsRepo: PostsRepo,
        exclude: List<PostId>,
        now: DateTime
    ): PostId? {
        var reversed: Boolean = false
        var count: Int? = null
        val allowedCreationTime = now - (postAge ?: 0).seconds

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
                        ratingsRepo.getAllByWithNextPaging { keys(it) }
                    }
                    else -> {
                        ratingsRepo.getPostsWithRatingLessEq(max, exclude = exclude).keys
                    }
                }
            }
            else -> {
                when (max) {
                    null -> {
                        ratingsRepo.getPostsWithRatingGreaterEq(min, exclude = exclude).keys
                    }
                    else -> {
                        ratingsRepo.getPosts(min .. max, reversed, count, exclude = exclude).keys
                    }
                }
            }
        }.filter {
            it !in exclude && (postsRepo.getPostCreationTime(it) ?.let { it < allowedCreationTime } ?: true)
        }

        return when (prefer) {
            Prefer.Max,
            Prefer.Min -> posts.firstOrNull()
            Prefer.Random -> posts.randomOrNull()
        } ?: otherwise ?.select(ratingsRepo, postsRepo, exclude, now)
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
