package dev.inmo.plaguposter.triggers.timer

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.Month
import dev.inmo.plaguposter.common.SuccessfulSymbol
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.commonMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.ifCallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.extensions.utils.ifCommonMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.withContent
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.queries.callback.MessageDataCallbackQuery
import dev.inmo.tgbotapi.utils.row

object ButtonsBuilder {
    private const val changeHoursDataPrefix = "timer_hours"
    private const val changeMinutesDataPrefix = "timer_minutes"
    private const val changeDayDataPrefix = "timer_day"
    private const val changeMonthDataPrefix = "timer_month"
    private const val changeYearDataPrefix = "timer_year"
    private const val changeDateDataPrefix = "timer_set"

    private fun buildTimerButtons(
        postId: PostId,
        dateTime: DateTime
    ) = flatInlineKeyboard {
        val unixMillis = dateTime.unixMillisLong
        val local = dateTime.local
        dataButton(local.hours.toString(), "$changeHoursDataPrefix $postId $unixMillis")
        dataButton(":${local.minutes}", "$changeMinutesDataPrefix $postId $unixMillis")

        dataButton(local.dayOfMonth.toString(), "$changeDayDataPrefix $postId $unixMillis")
        dataButton(".${local.month1}", "$changeMonthDataPrefix $postId $unixMillis")
        dataButton(".${local.yearInt}", "$changeYearDataPrefix $postId $unixMillis")

        dataButton(SuccessfulSymbol, "$changeDateDataPrefix $postId $unixMillis")
    }

    suspend fun BehaviourContext.includeKeyboardHandling() {
        fun buildKeyboard(
            prefix: String,
            postId: PostId,
            values: Iterable<Int>,
            dateConverter: (Int) -> DateTime
        ): InlineKeyboardMarkup {
            return inlineKeyboard {
                values.chunked(5).forEach {
                    row {
                        it.forEach {
                            dataButton(it.toString(), "$prefix $postId ${dateConverter(it).unixMillisLong}")
                        }
                    }
                }
            }
        }

        suspend fun buildStandardDataCallbackQuery(
            prefix: String,
            possibleValues: (DateTime) -> Iterable<Int>,
            dateTimeConverter: (Int, DateTime) -> DateTime
        ) {
            val setPrefix = "${prefix}_set"
            onMessageDataCallbackQuery(Regex("$prefix .+")) {
                val (_, rawPostId, rawDateTimeMillis) = it.data.split(" ")
                val currentMillis = rawDateTimeMillis.toLongOrNull() ?: return@onMessageDataCallbackQuery
                val currentDateTime = DateTime(currentMillis)

                edit (
                    it.message,
                    replyMarkup = buildKeyboard(
                        setPrefix,
                        PostId(rawPostId),
                        possibleValues(DateTime(currentMillis))
                    ) {
                        dateTimeConverter(it, currentDateTime)
                    }
                )
            }

            onMessageDataCallbackQuery(Regex("$setPrefix .+")) {
                val (_, rawPostId, rawDateTimeMillis) = it.data.split(" ")

                val currentMillis = rawDateTimeMillis.toLongOrNull() ?: return@onMessageDataCallbackQuery
                val currentDateTime = DateTime(currentMillis)
                edit(
                    it.message,
                    buildTimerButtons(
                        PostId(rawPostId),
                        currentDateTime
                    )
                )
            }
        }

        fun DateTimeTz.dateEq(other: DateTimeTz) = yearInt == other.yearInt && month0 == other.month0 && dayOfMonth == other.dayOfMonth

        buildStandardDataCallbackQuery(
            changeHoursDataPrefix,
            {
                val now = DateTime.now().local
                val local = it.local

                if (now.dateEq(local)) {
                    now.hours .. 23
                } else {
                    0 .. 23
                }
            }
        ) { newValue, oldDateTime ->
            oldDateTime.copyDayOfMonth(hours = newValue)
        }

        buildStandardDataCallbackQuery(
            changeMinutesDataPrefix,
            {
                val now = DateTime.now().local
                val local = it.local

                if (now.dateEq(local)) {
                    now.minutes .. 60
                } else {
                    0 .. 60
                }
            }
        ) { newValue, oldDateTime ->
            oldDateTime.copyDayOfMonth(minutes = newValue)
        }

        buildStandardDataCallbackQuery(
            changeDayDataPrefix,
            {
                val now = DateTime.now().local
                val local = it.local

                if (now.dateEq(local)) {
                    now.dayOfMonth .. it.month.days(it.year)
                } else {
                    1 .. it.month.days(it.year)
                }
            }
        ) { newValue, oldDateTime ->
            oldDateTime.copyDayOfMonth(dayOfMonth = newValue)
        }

        buildStandardDataCallbackQuery(
            changeMonthDataPrefix,
            {
                val now = DateTime.now().local
                val local = it.local

                if (now.year == local.year) {
                    now.month1 .. 12
                } else {
                    1 .. 12
                }
            }
        ) { newValue, oldDateTime ->
            oldDateTime.copyDayOfMonth(month = Month(newValue))
        }

        buildStandardDataCallbackQuery(
            changeYearDataPrefix,
            {
                (it.year.year .. (it.year.year + 5))
            }
        ) { newValue, oldDateTime ->
            oldDateTime.copyDayOfMonth(month = Month(newValue))
        }
    }
}
