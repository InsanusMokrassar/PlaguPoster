package dev.inmo.plaguposter.triggers.timer

import korlibs.time.DateFormat
import korlibs.time.DateTime
import korlibs.time.DateTimeTz
import korlibs.time.Month
import korlibs.time.Year
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.repos.unset
import dev.inmo.plaguposter.common.SuccessfulSymbol
import dev.inmo.plaguposter.common.UnsuccessfulSymbol
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.withContentOrNull
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.bold
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row

object ButtonsBuilder {
    private const val changeTimeData = "timer_time_hint"
    private const val changeDateData = "timer_date_hint"
    private const val changeHoursDataPrefix = "timer_h"
    private const val changeMinutesDataPrefix = "timer_m"
    private const val changeDayDataPrefix = "timer_d"
    private const val changeMonthDataPrefix = "timer_M"
    private const val changeYearDataPrefix = "timer_y"
    private const val changeDateDataPrefix = "timer_s"
    private const val cancelDateData = "timer_c"
    private const val deleteDateDataPrefix = "timer_r"
    val datePrintFormat = DateFormat("HH:mm, dd.MM.yyyy, zzz")

    fun buildTimerButtons(
        postId: PostId,
        dateTime: DateTimeTz,
        exists: Boolean
    ) = inlineKeyboard {
        val unixMillis = dateTime.utc.unixMillisLong
        row {
            dataButton("Time (hh:mm):", changeTimeData)
            dataButton(dateTime.hours.toString(), "$changeHoursDataPrefix $postId $unixMillis")
            dataButton(dateTime.minutes.toString(), "$changeMinutesDataPrefix $postId $unixMillis")
        }
        row {
            dataButton("Date (dd.mm.yyyy):", changeDateData)
            dataButton("${dateTime.dayOfMonth}", "$changeDayDataPrefix $postId $unixMillis")
            dataButton("${dateTime.month1}", "$changeMonthDataPrefix $postId $unixMillis")
            dataButton("${dateTime.yearInt}", "$changeYearDataPrefix $postId $unixMillis")
        }

        row {
            if (exists) {
                dataButton("\uD83D\uDDD1", "$deleteDateDataPrefix $postId")
            }
            dataButton(UnsuccessfulSymbol, cancelDateData)
            dataButton(SuccessfulSymbol, "$changeDateDataPrefix $postId $unixMillis")
        }
    }

    fun buildTimerTextSources(
        currentDateTime: DateTime,
        previousTime: DateTime?
    ) = buildEntities {
        previousTime ?.let {
            + "Previous timer time: " + bold(it.local.toString(datePrintFormat)) + "\n"
        }
        +"Currently editing time: " + bold(currentDateTime.local.toString(datePrintFormat))
    }

    suspend fun BehaviourContext.includeKeyboardHandling(
        timersRepo: TimersRepo,
        onSavePublishingTime: suspend (PostId, DateTime) -> Boolean
    ) {
        fun buildKeyboard(
            prefix: String,
            postId: PostId,
            values: Iterable<Int>,
            min: DateTime = nearestAvailableTimerTime(),
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
            name: String,
            prefix: String,
            possibleValues: (DateTimeTz) -> Iterable<Int>,
            dateTimeConverter: (Int, DateTimeTz) -> DateTimeTz
        ) {
            val setPrefix = "${prefix}s"
            onMessageDataCallbackQuery(Regex("$prefix .+")) {
                val (_, rawPostId, rawDateTimeMillis) = it.data.split(" ")
                val currentMillis = rawDateTimeMillis.toLongOrNull() ?: return@onMessageDataCallbackQuery
                val currentDateTime = DateTime(currentMillis)
                val currentDateTimeLocal = DateTime(currentMillis).local
                val postId = PostId(rawPostId)
                val previousTime = timersRepo.get(postId)

                edit (
                    it.message.withContentOrNull() ?: return@onMessageDataCallbackQuery,
                    replyMarkup = buildKeyboard(
                        setPrefix,
                        postId,
                        possibleValues(currentDateTimeLocal)
                    ) {
                        dateTimeConverter(it, currentDateTimeLocal)
                    }
                ) {
                    +buildTimerTextSources(currentDateTime, previousTime) + "\n"
                    +"You are about to edit $name"
                }
            }

            onMessageDataCallbackQuery(Regex("$setPrefix .+")) {
                val (_, rawPostId, rawDateTimeMillis) = it.data.split(" ")

                val currentMillis = rawDateTimeMillis.toLongOrNull() ?: return@onMessageDataCallbackQuery
                val currentDateTime = DateTime(currentMillis)
                val postId = PostId(rawPostId)
                val previousTime = timersRepo.get(postId)
                edit(
                    it.message.withContentOrNull() ?: return@onMessageDataCallbackQuery,
                    replyMarkup = buildTimerButtons(
                        postId,
                        currentDateTime.local,
                        timersRepo.contains(postId)
                    )
                ) {
                    +buildTimerTextSources(currentDateTime, previousTime)
                }
            }
        }

        fun DateTimeTz.dateEq(other: DateTimeTz) = yearInt == other.yearInt && month0 == other.month0 && dayOfMonth == other.dayOfMonth

        buildStandardDataCallbackQuery(
            "hour",
            changeHoursDataPrefix,
            {
                val now = nearestAvailableTimerTime().local

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
            "minute",
            changeMinutesDataPrefix,
            {
                val now = nearestAvailableTimerTime().local

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
            "day",
            changeDayDataPrefix,
            {
                val now = nearestAvailableTimerTime().local

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
            "month",
            changeMonthDataPrefix,
            {
                val now = nearestAvailableTimerTime().local

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
            "year",
            changeYearDataPrefix,
            {
                val now = nearestAvailableTimerTime().local
                (now.year.year .. (now.year.year + 5))
            }
        ) { newValue, oldDateTime ->
            DateTimeTz.local(
                oldDateTime.local.copyDayOfMonth(year = Year(newValue)),
                oldDateTime.offset
            )
        }

        onMessageDataCallbackQuery(changeTimeData) {
            answer(it, "Use the buttons to the right to set post publishing time (hh:mm)", showAlert = true)
        }

        onMessageDataCallbackQuery(changeDateData) {
            answer(it, "Use the buttons to the right to set post publishing date (dd.MM.yyyy)", showAlert = true)
        }

        onMessageDataCallbackQuery(Regex("$changeDateDataPrefix .*")) {
            val (_, rawPostId, rawDateTimeMillis) = it.data.split(" ")
            val currentMillis = rawDateTimeMillis.toLongOrNull() ?: return@onMessageDataCallbackQuery
            val currentDateTime = DateTime(currentMillis)
            val postId = PostId(rawPostId)

            val success = runCatchingSafely {
                onSavePublishingTime(postId, currentDateTime)
            }.getOrElse { false }

            answer(
                it,
                if (success) "Successfully set timer" else "Unable to set timer"
            )

            it.message.delete(this)
        }

        onMessageDataCallbackQuery(Regex("$deleteDateDataPrefix .*")) {
            val (_, rawPostId) = it.data.split(" ")
            val postId = PostId(rawPostId)

            val success = runCatchingSafely {
                timersRepo.unset(postId)
                true
            }.getOrElse { false }

            answer(
                it,
                if (success) "Successfully unset timer" else "Unable to unset timer"
            )

            it.message.delete(this)
        }

        onMessageDataCallbackQuery(cancelDateData) {
            delete(it.message)
        }
    }
}
