package dev.inmo.plaguposter.ratings.source.buttons

import dev.inmo.micro_utils.pagination.FirstPagePagination
import dev.inmo.micro_utils.pagination.Pagination
import dev.inmo.micro_utils.pagination.isFirstPage
import dev.inmo.micro_utils.pagination.utils.paginate
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import dev.inmo.plaguposter.ratings.utils.postsByRatings
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.row

const val RootButtonsShowRatingData = "ratings_buttons_show"
const val RootButtonsToPageData = "ratings_buttons_to_page"

suspend fun RatingsRepo.buildRootButtons(
    pagination: Pagination = FirstPagePagination(16),
    rowSize: Int = 4
) {
    val postsByRatings = postsByRatings().toList().paginate(pagination)
    inlineKeyboard {
        if (postsByRatings.pagesNumber > 1) {
            row {
                if (postsByRatings.page > 0) {
                    dataButton("<", "$RootButtonsToPageData ${postsByRatings.page - 1}")
                }
                dataButton("${postsByRatings.page}: \uD83D\uDD04", "$RootButtonsToPageData ${postsByRatings.page}")
                if (postsByRatings.pagesNumber - postsByRatings.page > 1) {
                    dataButton(">", "$RootButtonsToPageData ${postsByRatings.page + 1}")
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
