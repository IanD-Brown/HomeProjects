package io.github.iandbrown.reconciler.logic

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val YEAR_FACTOR = 500

class DayDate {
    private val value: Int

    constructor(dayDate: Int) {
        this.value = dayDate
    }

    constructor(dateString : String) {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        value = YEAR_FACTOR * date.year + date.dayOfYear
    }

    constructor(msTime: Long) : this(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(msTime)))

    constructor(localDateTime: kotlinx.datetime.LocalDateTime) {
        value = YEAR_FACTOR * localDateTime.year + localDateTime.dayOfYear
    }

    override fun toString() : String {
        if (value == 0) {
            return ""
        }

        val date = LocalDate.ofYearDay(value / YEAR_FACTOR, value % YEAR_FACTOR)
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
    }

    fun value() : Int = value

    fun asUtcMs(): Long {
        if (value == 0) {
            return LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        }

        val date = LocalDate.ofYearDay(value / YEAR_FACTOR, value % YEAR_FACTOR)
        return date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
}
