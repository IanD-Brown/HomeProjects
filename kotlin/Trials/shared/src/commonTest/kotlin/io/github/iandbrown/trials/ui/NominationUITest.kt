package io.github.iandbrown.trials.ui

import io.github.iandbrown.trials.logic.DayDate
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NominationUITest {
    @Test
    fun testValidDateOfBirth() {
        val currentYear = 2026

        for ((year, group) in listOf(
            Pair(2015, "Five"),
            Pair(2014, "Six"),
            Pair(2013, "Seven"),
            Pair(2012, "Eight"),
            Pair(2011, "Nine"),
        )) {
            for (date in utcMs(year, 9, 1)..<utcMs(year + 1, 9, 1) step 24 * 60 * 60 * 1000) {
                assertTrue("Testing ${DayDate(date)} for $group") {
                    validDateOfBirth(currentYear, group, date)
                }
            }

            assertFalse { validDateOfBirth(currentYear, group, utcMs(year, 8, 30)) }
            assertFalse { validDateOfBirth(currentYear, group, utcMs(year + 1, 9, 2)) }
        }

        assertFalse { validDateOfBirth(currentYear, "bad", 0) }
    }

    @Test
    fun testDateExtraction() {
        assertEquals(DayDate(utcMs(2013, 11, 30)).value(),
            date("30112013")
        )
        assertEquals(DayDate(utcMs(2013, 11, 30)).value(),
            date("301113")
        )
    }

    private fun utcMs(year: Int, month: Int, day: Int) : Long {
        val localDate = LocalDate(year, month, day)
        return DayDate(localDate.year, localDate.dayOfYear).asUtcMs()
    }
}
