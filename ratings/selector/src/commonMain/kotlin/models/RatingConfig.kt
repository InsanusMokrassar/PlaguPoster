package dev.inmo.plaguposter.ratings.selector.models

import korlibs.time.DateTime
import korlibs.time.seconds
import dev.inmo.micro_utils.pagination.utils.getAllByWithNextPaging
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.unset
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
    val min: Rating? = null,
    val max: Rating? = null,
    val prefer: Prefer = Prefer.Random,
    val otherwise: RatingConfig? = null,
    val postAge: Seconds? = null,
    val uniqueCount: Int? = null
) {
    suspend fun select(
        ratingsRepo: RatingsRepo,
        postsRepo: PostsRepo,
        exclude: List<PostId>,
        now: DateTime,
        latestChosenRepo: KeyValueRepo<PostId, DateTime>
    ): PostId? {
        var reversed: Boolean = false
        var count: Int? = null
        val allowedCreationTime = now - (postAge ?: 0).seconds
        val excludedByRepo = uniqueCount ?.let {
            latestChosenRepo.getAll().toList().sortedBy { it.second }.takeLast(uniqueCount).map { it.first }
        } ?: emptyList()
        val resultExcluded = exclude + excludedByRepo

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
                        ratingsRepo.getPostsWithRatingLessEq(max, exclude = resultExcluded).keys
                    }
                }
            }
            else -> {
                when (max) {
                    null -> {
                        ratingsRepo.getPostsWithRatingGreaterEq(min, exclude = resultExcluded).keys
                    }
                    else -> {
                        ratingsRepo.getPosts(min .. max, reversed, count, exclude = resultExcluded).keys
                    }
                }
            }
        }.filter {
            it !in resultExcluded && (postsRepo.getPostCreationTime(it) ?.let { it < allowedCreationTime } ?: true)
        }

        val resultPosts: PostId = when (prefer) {
            Prefer.Max,
            Prefer.Min -> posts.firstOrNull()
            Prefer.Random -> posts.randomOrNull()
        } ?: otherwise ?.select(ratingsRepo, postsRepo, resultExcluded, now, latestChosenRepo) ?: return null

        val postsToKeep = uniqueCount ?.let {
            (excludedByRepo + resultPosts).takeLast(it)
        } ?: return resultPosts

        val postsToRemoveFromKeep = excludedByRepo.filter { it !in postsToKeep }
        latestChosenRepo.unset(postsToRemoveFromKeep)
        val postsToAdd = postsToKeep.filter { it !in excludedByRepo }
        latestChosenRepo.set(
            postsToAdd.associateWith { DateTime.now() }
        )

        return resultPosts
    }

    @Serializable(Prefer.Serializer::class)
    sealed interface Prefer {
        val type: String
        @Serializable(Serializer::class)
        data object Max : Prefer { override val type: String = "max" }
        @Serializable(Serializer::class)
        data object Min : Prefer { override val type: String = "min" }
        @Serializable(Serializer::class)
        data object Random : Prefer { override val type: String = "random" }

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
            val values: Array<Prefer> = arrayOf(Max, Min, Random)
        }
    }
}
