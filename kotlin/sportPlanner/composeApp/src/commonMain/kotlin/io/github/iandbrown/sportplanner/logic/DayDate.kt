package io.github.iandbrown.sportplanner.logic

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val YEAR_FACTOR = 500

class DayDate {
    companion object {
        fun isMondayIn(startDate : Int, endDate: Int, date : Int): Boolean =
            date in startDate..endDate && DayDate(date).isMonday()
    }
    private val value: Int

    constructor(dayDate: Int) {
        this.value = dayDate
    }

    constructor(dateString : String) {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        value = YEAR_FACTOR * date.year + date.dayOfYear
    }

    constructor(msTime: Long) : this(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(msTime))!!) {}

    fun isValid() : Boolean = value > 0

    fun isMonday() : Boolean {
        if (value == 0) {
            return false
        }
        return LocalDate.ofYearDay(value / YEAR_FACTOR, value % YEAR_FACTOR).dayOfWeek == java.time.DayOfWeek.MONDAY
    }

    fun addDays(days: Int) : DayDate {
        if (value == 0) {
            return this
        }
        val date = LocalDate.ofYearDay(value / YEAR_FACTOR, value % YEAR_FACTOR)
        val changedDate = date.plusDays(days.toLong())
        return DayDate(YEAR_FACTOR * changedDate.year + changedDate.dayOfYear)
    }

    override fun toString() : String {
        if (value == 0) {
            return ""
        }

        val date = LocalDate.ofYearDay(value / YEAR_FACTOR, value % YEAR_FACTOR)
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
