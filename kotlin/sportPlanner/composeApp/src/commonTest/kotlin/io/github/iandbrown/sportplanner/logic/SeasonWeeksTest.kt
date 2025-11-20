package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import java.text.SimpleDateFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeasonWeeksTest {
    @Test
    fun testVisitWeeks() {
        val competitions = listOf(
            SeasonCompetition(1, 1, getMsTime("01/09/2025"), getMsTime("21/09/2025")),
            SeasonCompetition(2, 1, getMsTime("15/09/2025"), getMsTime("05/10/2025"))
        )
        val breaks = listOf(
            SeasonBreak(1, 1, "Break 1", getMsTime("08/09/2025"))
        )

        val seasonWeeks = SeasonWeeks(competitions, breaks)

        assertEquals(4, seasonWeeks.playingWeeks())

        val weeks = mutableMapOf<Long, String?>()
        seasonWeeks.visitWeeks { monday, message ->
            weeks[monday] = message
        }

        assertTrue(weeks.containsKey(getMsTime("01/09/2025")))
        assertTrue(weeks.containsKey(getMsTime("08/09/2025")))
        assertTrue(weeks.containsKey(getMsTime("15/09/2025")))
        assertTrue(weeks.containsKey(getMsTime("22/09/2025")))
        assertTrue(weeks.containsKey(getMsTime("29/09/2025")))

        assertNull(weeks[getMsTime("1/9/2025")])
        assertTrue(weeks.get(getMsTime("8/9/2025")).equals("Break 1"))
        assertNull(weeks[getMsTime("15/9/2025")])
        assertNull(weeks[getMsTime("22/9/2025")])
        assertNull(weeks[getMsTime("29/9/2025")])
    }

    private fun getMsTime(date : String): Long =
        SimpleDateFormat("dd/MM/yyyy").parse(date).time

}
