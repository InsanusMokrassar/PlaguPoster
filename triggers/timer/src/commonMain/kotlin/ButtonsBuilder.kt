package dev.inmo.plaguposter.triggers.timer

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.Month
import com.soywiz.klock.Year
import dev.inmo.plaguposter.common.SuccessfulSymbol
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.row

object ButtonsBuilder {
    private const val changeHoursDataPrefix = "timer_h"
    private const val changeMinutesDataPrefix = "timer_m"
    private const val changeDayDataPrefix = "timer_d"
    private const val changeMonthDataPrefix = "timer_M"
    private const val changeYearDataPrefix = "timer_y"
    private const val changeDateDataPrefix = "timer_s"

    fun buildTimerButtons(
        postId: PostId,
        dateTime: DateTimeTz
    ) = flatInlineKeyboard {
        val unixMillis = dateTime.utc.unixMillisLong
        dataButton(dateTime.hours.toString(), "$changeHoursDataPrefix $postId $unixMillis")
        dataButton(":${dateTime.minutes}", "$changeMinutesDataPrefix $postId $unixMillis")

        dataButton(dateTime.dayOfMonth.toString(), "$changeDayDataPrefix $postId $unixMillis")
        dataButton(".${dateTime.month1}", "$changeMonthDataPrefix $postId $unixMillis")
        dataButton(".${dateTime.yearInt}", "$changeYearDataPrefix $postId $unixMillis")

        dataButton(SuccessfulSymbol, "$changeDateDataPrefix $postId $unixMillis")
    }

    suspend fun BehaviourContext.includeKeyboardHandling() {
        fun buildKeyboard(
            prefix: String,
            postId: PostId,
            values: Iterable<Int>,
            min: DateTime = DateTime.now(),
            dateConverter: (Int) -> DateTimeTz
        ): InlineKeyboardMarkup {
            return inlineKeyboard {
                values.chunked(6).forEach {
                    row {
                        it.forEach {
                            dataButton(it.toString(), "$prefix $postId ${dateConverter(it).utc.unixMillisLong.coerceAtLeast(min.unixMillisLong)}")
                        }
                    }
                }
            }
        }

        suspend fun buildStandardDataCallbackQuery(
            prefix: String,
            possibleValues: (DateTimeTz) -> Iterable<Int>,
            dateTimeConverter: (Int, DateTimeTz) -> DateTimeTz
        ) {
            val setPrefix = "${prefix}s"
            onMessageDataCallbackQuery(Regex("$prefix .+")) {
                val (_, rawPostId, rawDateTimeMillis) = it.data.split(" ")
                val currentMillis = rawDateTimeMillis.toLongOrNull() ?: return@onMessageDataCallbackQuery
                val currentDateTime = DateTime(currentMillis).local

                edit (
                    it.message,
                    replyMarkup = buildKeyboard(
                        setPrefix,
                        PostId(rawPostId),
                        possibleValues(currentDateTime)
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
                        currentDateTime.local
                    )
                )
            }
        }

        fun DateTimeTz.dateEq(other: DateTimeTz) = yearInt == other.yearInt && month0 == other.month0 && dayOfMonth == other.dayOfMonth

        buildStandardDataCallbackQuery(
            changeHoursDataPrefix,
            {
                val now = DateTime.now().local

                if (now.dateEq(it)) {
                    now.hours .. 23
                } else {
                    0 .. 23
                }
            }
        ) { newValue, oldDateTime ->
            DateTimeTz.local(
                oldDateTime.local.copyDayOfMonth(hours = newValue),
                oldDateTime.offset
            )
        }

        buildStandardDataCallbackQuery(
            changeMinutesDataPrefix,
            {
                val now = DateTime.now().local

                if (now.dateEq(it) && now.hours >= it.hours) {
                    now.minutes until 60
                } else {
                    0 until 60
                }
            }
        ) { newValue, oldDateTime ->
            DateTimeTz.local(
                oldDateTime.local.copyDayOfMonth(minutes = newValue),
                oldDateTime.offset
            )
        }

        buildStandardDataCallbackQuery(
            changeDayDataPrefix,
            {
                val now = DateTime.now().local

                if (now.yearInt == it.yearInt && now.month0 == it.month0) {
                    now.dayOfMonth .. it.month.days(it.year)
                } else {
                    1 .. it.month.days(it.year)
                }
            }
        ) { newValue, oldDateTime ->
            DateTimeTz.local(
                oldDateTime.local.copyDayOfMonth(dayOfMonth = newValue),
                oldDateTime.offset
            )
        }

        buildStandardDataCallbackQuery(
            changeMonthDataPrefix,
            {
                val now = DateTime.now().local

                if (now.year == it.year) {
                    now.month1 .. 12
                } else {
                    1 .. 12
                }
            }
        ) { newValue, oldDateTime ->
            DateTimeTz.local(
                oldDateTime.local.copyDayOfMonth(month = Month(newValue)),
                oldDateTime.offset
            )
        }

        buildStandardDataCallbackQuery(
            changeYearDataPrefix,
            {
                (it.year.year .. (it.year.year + 5))
            }
        ) { newValue, oldDateTime ->
            DateTimeTz.local(
                oldDateTime.local.copyDayOfMonth(year = Year(newValue)),
                oldDateTime.offset
            )
        }
    }
}
