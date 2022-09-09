package dev.inmo.plaguposter.ratings.source

import dev.inmo.kslog.common.e
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.pagination.firstPageWithOneElementPagination
import dev.inmo.micro_utils.repos.id
import dev.inmo.micro_utils.repos.pagination.getAll
import dev.inmo.micro_utils.repos.set
import dev.inmo.plagubot.Plugin
import dev.inmo.plaguposter.common.*
import dev.inmo.plaguposter.inlines.models.Format
import dev.inmo.plaguposter.inlines.models.OfferTemplate
import dev.inmo.plaguposter.inlines.repos.InlineTemplatesRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.posts.repo.PostsRepo
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.ratings.source.models.*
import dev.inmo.plaguposter.ratings.source.repos.*
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.types.message.textsources.regular
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.qualifier.named

object Plugin : Plugin {
    private val ratingVariantsQualifier = named("ratingsVariants")

    @Serializable
    internal data class Config(
        @Serializable(RatingsVariantsSerializer::class)
        val variants: RatingsVariants,
        val autoAttach: Boolean,
        val ratingOfferText: String
    )

    override fun Module.setupDI(database: Database, params: JsonObject) {
        single {
            get<Json>().decodeFromJsonElement(Config.serializer(), params["ratingsPolls"] ?: error("Unable to load config for rating polls in $params"))
        }
        single<RatingsVariants>(ratingVariantsQualifier) { get<Config>().variants }
        single<PollsToPostsIdsRepo> { ExposedPollsToPostsIdsRepo(database) }
        single<PollsToMessagesInfoRepo> { ExposedPollsToMessagesInfoRepo(database) }
        single<VariantTransformer> {
            val ratingsSettings = get<RatingsVariants>(ratingVariantsQualifier)
            VariantTransformer {
                ratingsSettings[it]
            }
        }
    }

    override suspend fun BehaviourContext.setupBotPlugin(koin: Koin) {
        val pollsToPostsIdsRepo = koin.get<PollsToPostsIdsRepo>()
        val pollsToMessageInfoRepo = koin.get<PollsToMessagesInfoRepo>()
        val variantsTransformer = koin.get<VariantTransformer>()
        val ratingsRepo = koin.get<RatingsRepo>()
        val postsRepo = koin.get<PostsRepo>()
        val config = koin.get<Config>()

        onPollUpdates (markerFactory = { it.id }) { poll ->
            val postId = pollsToPostsIdsRepo.get(poll.id) ?: return@onPollUpdates
            val newRating = poll.options.sumOf {
                (variantsTransformer(it.text) ?.double ?.times(it.votes)) ?: 0.0
            }
            ratingsRepo.set(postId, Rating(newRating))
        }

        suspend fun attachPoll(postId: PostId): Boolean {
            if (pollsToPostsIdsRepo.keys(postId, firstPageWithOneElementPagination).results.isNotEmpty()) {
                return false
            }

            val post = postsRepo.getById(postId) ?: return false
            for (content in post.content) {
                runCatchingSafely {
                    val sent = send(
                        content.chatId,
                        config.ratingOfferText,
                        config.variants.keys.toList(),
                        replyToMessageId = content.messageId
                    )
                    pollsToPostsIdsRepo.set(sent.content.poll.id, postId)
                    pollsToMessageInfoRepo.set(sent.content.poll.id, sent.short())
                }.getOrNull() ?: continue
                return true
            }
            return false
        }

        suspend fun detachPoll(postId: PostId): Boolean {
            val postIds = pollsToPostsIdsRepo.getAll { keys(postId, it) }.takeIf { it.isNotEmpty() } ?: return false
            return postIds.map { (pollId) ->
                val messageInfo = pollsToMessageInfoRepo.get(pollId) ?: return@map false
                runCatchingSafely {
                    delete(messageInfo.chatId, messageInfo.messageId)
                }.onFailure {
                    this@Plugin.logger.e(it) { "Something went wrong when trying to remove ratings message ($messageInfo) for post $postId" }
                }.isSuccess
            }.any().also {
                if (it) {
                    pollsToPostsIdsRepo.unset(postIds.map { it.id })
                    pollsToMessageInfoRepo.unset(postIds.map { it.id })
                }
            }
        }

        postsRepo.deletedObjectsIdsFlow.subscribeSafelyWithoutExceptions(this) { postId ->
            detachPoll(postId)
        }

        if (config.autoAttach) {
            postsRepo.newObjectsFlow.subscribeSafelyWithoutExceptions(this) {
                attachPoll(it.id)
            }
        }

        onCommand("attach_ratings", requireOnlyCommandInMessage = true) {
            val replyTo = it.replyTo ?: run {
                reply(
                    it,
                    "You should reply to post message to attach ratings"
                )
                return@onCommand
            }

            val postId = postsRepo.getIdByChatAndMessage(replyTo.chat.id, replyTo.messageId) ?: run {
                reply(
                    it,
                    "Unable to find post where the message in reply is presented"
                )
                return@onCommand
            }

            if (attachPoll(postId)) {
                runCatchingSafely {
                    edit(
                        it,
                        it.content.textSources + regular(" $SuccessfulSymbol")
                    )
                }
            } else {
                runCatchingSafely {
                    edit(
                        it,
                        it.content.textSources + regular(" $UnsuccessfulSymbol")
                    )
                }
            }
        }

        onCommand("detach_ratings", requireOnlyCommandInMessage = true) {
            val replyTo = it.replyTo ?: run {
                reply(
                    it,
                    "You should reply to post message to detach ratings"
                )
                return@onCommand
            }

            val postId = postsRepo.getIdByChatAndMessage(replyTo.chat.id, replyTo.messageId) ?: run {
                reply(
                    it,
                    "Unable to find post where the message in reply is presented"
                )
                return@onCommand
            }


            if (detachPoll(postId)) {
                runCatchingSafely {
                    edit(
                        it,
                        it.content.textSources + regular(" $SuccessfulSymbol")
                    )
                }
            } else {
                runCatchingSafely {
                    edit(
                        it,
                        it.content.textSources + regular(" $UnsuccessfulSymbol")
                    )
                }
            }
        }

        koin.getOrNull<InlineTemplatesRepo>() ?.apply {
            addTemplate(
                OfferTemplate(
                    "Enable ratings for post",
                    listOf(Format("attach_ratings")),
                    "Require reply on post message"
                )
            )
            addTemplate(
                OfferTemplate(
                    "Disable ratings for post",
                    listOf(Format("detach_ratings")),
                    "Require reply on post message"
                )
            )
        }
    }
}
