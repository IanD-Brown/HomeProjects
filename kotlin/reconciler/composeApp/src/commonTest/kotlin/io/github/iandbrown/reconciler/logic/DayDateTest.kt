package io.github.iandbrown.reconciler.logic

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter

class DayDateTest : ShouldSpec({
    should("construct using date string ") {
        for (year in 1970..2100) {
            val daysInYear = if (IsoChronology.INSTANCE.isLeapYear(year.toLong())) 366 else 365
            for (day in 1..daysInYear) {
                val dateString = LocalDate.ofYearDay(year, day)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val date = DayDate(dateString)

                date.toString() shouldBe LocalDate.ofYearDay(year, day)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                date.value() shouldBe 500 * year + day
            }
        }
    }

    should("construct using LocalDateTime") {
        for (year in 1970..2100) {
            val daysInYear = if (IsoChronology.INSTANCE.isLeapYear(year.toLong())) 366 else 365
            for (day in 1..daysInYear) {
                val dateString = LocalDate.ofYearDay(year, day)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val date = DayDate(LocalDateTime(kotlinx.datetime.LocalDate.parse(dateString),
                    LocalTime(1, 1, 1, 1)))

                date.toString() shouldBe LocalDate.ofYearDay(year, day)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                date.value() shouldBe 500 * year + day
            }
        }
    }

    should("construct using ms") {
        for (year in 1970..2100) {
            val daysInYear = if (IsoChronology.INSTANCE.isLeapYear(year.toLong())) 366 else 365
            for (day in 1..daysInYear) {
                val localDate = LocalDate.ofYearDay(year, day)
                val date = DayDate(localDate.toEpochDay() * 24 * 60 * 60 * 1000)

                date.toString() shouldBe LocalDate.ofYearDay(year, day)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                date.value() shouldBe 500 * year + day
            }
        }
    }

    context("asUtcMs") {
        should("return correct ms for a given date") {
            val date = DayDate("01/01/1970")
            date.asUtcMs() shouldBe 0L
        }

        should("return the current day start ms when constructed with 0") {
            DayDate(0).asUtcMs() shouldNotBe 0
        }
    }

    context("toString") {
        should("return empty string when constructed with 0") {
            DayDate(0).toString() shouldBe ""
        }
    }
})
