package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import java.util.Calendar

class SeasonWeeks(competitions: List<SeasonCompetition>, private val breaks: List<SeasonBreak>) {
    private val minDate = competitions.minOfOrNull { it.startDate } ?: 0L
    private val maxDate = competitions.maxOfOrNull { it.endDate } ?: 0L

    fun playingWeeks() : Int {
        var result = 0
        visitWeeks({_, message -> if (message == null) result++})
        return result
    }

    fun visitWeeks(processor: (monday: Long, message: String?) -> Unit) {
        if (minDate == 0L) {
            return
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = minDate
        // Find the first Monday
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        while (calendar.timeInMillis <= maxDate) {
            val currentMonday = calendar.timeInMillis
            val breakForWeek = breaks.firstOrNull { currentMonday >= it.week && currentMonday <= it.week }
            processor(currentMonday, breakForWeek?.name)
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
    }
}
