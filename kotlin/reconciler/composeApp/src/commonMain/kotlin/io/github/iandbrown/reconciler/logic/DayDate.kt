package io.github.iandbrown.reconciler.logic

import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.time.Clock

private const val YEAR_FACTOR = 500

internal const val TO_STRING_PATTERN = "dd/MM/yy"

class DayDate private constructor(private val value: Int) {

    companion object {
        fun of(dayDate: Int) = DayDate(dayDate)

        fun of(dateString : String, datePattern: String = "dd/MM/yyyy"): DayDate {
            val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(datePattern))
            return DayDate(YEAR_FACTOR * date.year + date.dayOfYear)
        }

        fun of(msTime: Long): DayDate = of(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(msTime)))

        fun of(localDateTime: kotlinx.datetime.LocalDateTime): DayDate {
            return DayDate(YEAR_FACTOR * localDateTime.year + localDateTime.dayOfYear)
        }

        fun of(localDate : LocalDate): DayDate {
            return DayDate(YEAR_FACTOR * localDate.year + localDate.dayOfYear)
        }

        fun ofCurrentYearStart(): DayDate {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            return DayDate(YEAR_FACTOR * today.year + 1)
        }
    }

    override fun toString() : String {
        if (value == 0) {
            return ""
        }

        return getLocalDate().format(DateTimeFormatter.ofPattern(TO_STRING_PATTERN))
    }

    fun value() : Int = value

    fun asUtcMs(): Long {
        if (value == 0) {
            return LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        }

        return getLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    fun startOfMonth() : DayDate {
        val date = getLocalDate()
        return of(date.withDayOfMonth(1))
    }

    fun nextMonth() : DayDate {
        val date = getLocalDate()
        return of(date.plusMonths(1)).startOfMonth()
    }

    private fun getLocalDate(): LocalDate = LocalDate.ofYearDay(value / YEAR_FACTOR, value % YEAR_FACTOR)
}
