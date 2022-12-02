package dev.inmo.plaguposter.ratings.source.buttons

import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.SimplePagination
import dev.inmo.micro_utils.pagination.utils.paginate
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.ratings.utils.postsByRatings
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.row

const val RootButtonsShowRatingData = "ratings_buttons_show"
const val RootButtonsShowRatingPageData = "ratings_buttons_show_page"
const val RootButtonsToPageData = "ratings_buttons_to_page"

suspend fun RatingsRepo.buildRootButtons(
    pagination: Pagination = FirstPagePagination(16),
    rowSize: Int = 4
): InlineKeyboardMarkup {
    val postsByRatings = postsByRatings().toList().paginate(pagination)
    return inlineKeyboard {
        if (postsByRatings.pagesNumber > 1) {
            row {
                if (postsByRatings.page > 0) {
                    dataButton("<", "$RootButtonsToPageData ${postsByRatings.page - 1} ${postsByRatings.size}")
                }
                dataButton("${postsByRatings.page}: \uD83D\uDD04", "$RootButtonsToPageData ${postsByRatings.page} ${postsByRatings.size}")
                if (postsByRatings.pagesNumber - postsByRatings.page > 1) {
                    dataButton(">", "$RootButtonsToPageData ${postsByRatings.page + 1} ${postsByRatings.size}")
                }
            }
        }
        postsByRatings.results.chunked(rowSize).map {
            row {
                it.forEach { (rating, posts) ->
                    dataButton("${rating.double}: ${posts.size}", "$RootButtonsShowRatingData ${rating.double}")
                }
            }
        }
    }
}

suspend fun RatingsRepo.buildRatingButtons(
    rating: Rating,
    pagination: Pagination = FirstPagePagination(8),
    rowSize: Int = 2
): InlineKeyboardMarkup {
    val postsByRatings = getPosts(rating .. rating, true).keys.paginate(pagination)
    return inlineKeyboard {
        if (postsByRatings.pagesNumber > 1) {
            row {
                if (postsByRatings.page > 0) {
                    dataButton("<", "$RootButtonsShowRatingPageData ${postsByRatings.page - 1} ${postsByRatings.size}")
                }
                dataButton("${postsByRatings.page}: \uD83D\uDD04", "$RootButtonsShowRatingPageData ${postsByRatings.page} ${postsByRatings.size}")
                if (postsByRatings.pagesNumber - postsByRatings.page > 1) {
                    dataButton(">", "$RootButtonsShowRatingPageData ${postsByRatings.page + 1} ${postsByRatings.size}")
                }
            }
        }
        postsByRatings.results.chunked(rowSize).map {
            row {
                it.forEach { (rating, posts) ->
                    dataButton("${rating.double}: ${posts.size}", "$RootButtonsShowRatingData ${rating.double}")
                }
            }
        }
    }
}

suspend fun BehaviourContext.includeRootNavigationButtonsHandler(
    allowedChats: Set<ChatIdentifier>,
    ratingsRepo: RatingsRepo
) {
    suspend fun registerPageQueryListener(
        dataPrefix: String,
        onPageUpdate: suspend (pagination: Pagination) -> InlineKeyboardMarkup?
    ) {
        onMessageDataCallbackQuery(
            initialFilter = { it.message.chat.id in allowedChats }
        ) {
            val (prefix, pageRaw, sizeRaw) = it.data.split(" ").takeIf { it.size == 3 } ?: return@onMessageDataCallbackQuery

            if (prefix == dataPrefix) {
                runCatchingSafely {
                    val page = pageRaw.toIntOrNull() ?: return@runCatchingSafely
                    val size = sizeRaw.toIntOrNull() ?: return@runCatchingSafely

                    edit(
                        it.message,
                        onPageUpdate(SimplePagination(page, size)) ?: return@runCatchingSafely
                    )
                }

                answer(it)
            }
        }
    }
    registerPageQueryListener(
        RootButtonsToPageData,
        ratingsRepo::buildRootButtons
    )
    onMessageDataCallbackQuery(
        initialFilter = { it.message.chat.id in allowedChats }
    ) {
        val (prefix, pageRaw, sizeRaw) = it.data.split(" ").takeIf { it.size == 3 } ?: return@onMessageDataCallbackQuery

        if (prefix == RootButtonsToPageData) {
            runCatchingSafely {
                val page = pageRaw.toIntOrNull() ?: return@runCatchingSafely
                val size = sizeRaw.toIntOrNull() ?: return@runCatchingSafely

                edit(
                    it.message,
                    ratingsRepo.buildRootButtons(SimplePagination(page, size))
                )
            }

            answer(it)
        }
    }
    onMessageDataCallbackQuery(
        initialFilter = { it.message.chat.id in allowedChats }
    ) {
        val (prefix, ratingRaw) = it.data.split(" ").takeIf { it.size == 2 } ?: return@onMessageDataCallbackQuery

        if (prefix == RootButtonsShowRatingData) {
            runCatchingSafely {
                val rating = ratingRaw.toDoubleOrNull() ?: return@runCatchingSafely
            }

            answer(it)
        }
    }
}
