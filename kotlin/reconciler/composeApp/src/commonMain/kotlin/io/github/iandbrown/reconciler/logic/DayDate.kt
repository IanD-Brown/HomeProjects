package io.github.iandbrown.reconciler.logic

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val YEAR_FACTOR = 500

internal const val TO_STRING_PATTERN = "dd/MM/yy"

class DayDate {
    private val value: Int

    constructor(dayDate: Int) {
        this.value = dayDate
    }

    constructor(dateString : String, datePattern: String = "dd/MM/yyyy") {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(datePattern))
        value = YEAR_FACTOR * date.year + date.dayOfYear
    }

    constructor(msTime: Long) : this(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(msTime)))

    constructor(localDateTime: kotlinx.datetime.LocalDateTime) {
        value = YEAR_FACTOR * localDateTime.year + localDateTime.dayOfYear
    }

    constructor(localDate : LocalDate) {
        value = YEAR_FACTOR * localDate.year + localDate.dayOfYear
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
        return DayDate(date.withDayOfMonth(1))
    }

    fun nextMonth() : DayDate {
        val date = getLocalDate()
        return DayDate(date.plusMonths(1)).startOfMonth()
    }

    private fun getLocalDate(): LocalDate = LocalDate.ofYearDay(value / YEAR_FACTOR, value % YEAR_FACTOR)
}
