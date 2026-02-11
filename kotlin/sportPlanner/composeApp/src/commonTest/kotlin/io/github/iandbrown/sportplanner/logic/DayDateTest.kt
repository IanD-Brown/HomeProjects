package io.github.iandbrown.sportplanner.logic

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter

class DayDateTest : ShouldSpec({
    should("match ") {
        for (year in 1970..2100) {
            val daysInYear = if (IsoChronology.INSTANCE.isLeapYear(year.toLong())) 366 else 365
            for (day in 1..daysInYear) {
                val dateString = LocalDate.ofYearDay(year, day)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val date = DayDate(dateString)

                date.toString() shouldBe dateString
                date.value() shouldBe 500 * year + day
            }
        }
    }

    context("isMonday") {
        should("return true for a Monday") {
            val monday = DayDate("02/09/2024") // Monday
            monday.isMonday() shouldBe true
        }

        should("return false for a Tuesday") {
            val tuesday = DayDate("03/09/2024") // Tuesday
            tuesday.isMonday() shouldBe false
        }

        should("Be false when constructed with 0") {
            DayDate(0).isMonday() shouldBe false
        }
    }

    context("isSunday") {
        should("return true for a Sunday") {
            val monday = DayDate("01/09/2024") // Sunday
            monday.isSunday() shouldBe true
        }

        should("return false for a Monday") {
            val tuesday = DayDate("02/09/2024") // Monday
            tuesday.isSunday() shouldBe false
        }

        should("Be false when constructed with 0") {
            DayDate(0).isSunday() shouldBe false
        }
    }

    context("addDays") {
        should("add days correctly within the same year") {
            val date = DayDate("02/09/2024")
            val newDate = date.addDays(5)
            newDate.toString() shouldBe "07/09/2024"
        }

        should("add days correctly across year boundary") {
            val date = DayDate("30/12/2024")
            val newDate = date.addDays(5)
            newDate.toString() shouldBe "04/01/2025"
        }

        should("be unchanged when constructed with 0") {
            DayDate(0).addDays(5).isValid() shouldBe false
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

    context("isMondayIn") {
        val startDate = DayDate("02/09/2024").value() // Monday
        val endDate = DayDate("16/09/2024").value() // Monday

        should("return true for a Monday within range") {
            val date = DayDate("09/09/2024").value() // Monday
            DayDate.isMondayIn(IntRange(startDate, endDate), date) shouldBe true
        }

        should("return false for a non-Monday within range") {
            val date = DayDate("10/09/2024").value() // Tuesday
            DayDate.isMondayIn(IntRange(startDate, endDate), date) shouldBe false
        }

        should("return false for a Monday outside range") {
            val date = DayDate("23/09/2024").value() // Monday
            DayDate.isMondayIn(IntRange(startDate, endDate), date) shouldBe false
        }

        should("return true for start date if it is a monday") {
            DayDate.isMondayIn(IntRange(startDate, endDate), startDate) shouldBe true
        }

        should("return true for end date if it is a monday") {
            DayDate.isMondayIn(IntRange(startDate, endDate), endDate) shouldBe true
        }
    }
})
