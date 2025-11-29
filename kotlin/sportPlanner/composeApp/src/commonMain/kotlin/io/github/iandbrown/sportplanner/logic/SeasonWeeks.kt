package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.ui.isMondayIn
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent
import java.util.*

class SeasonWeeks {
    companion object : KoinComponent {
        private val db: AppDatabase by KoinJavaComponent.inject(AppDatabase::class.java)

        suspend fun createSeasonWeeks(seasonId: Short): SeasonWeeks {
            return SeasonWeeks(
                db.getSeasonCompetitionDao().getBySeason(seasonId),
                db.getSeasonBreakDao().getBySeason(seasonId)
            )
        }
    }

    private val breakWeeks: Map<Long, String>
    private val competitionWeeks: Map<Short, List<Long>>

    constructor(seasonCompetitions: List<SeasonCompetition>, breaks: List<SeasonBreak>) {
        val validSeasonCompetitions = seasonCompetitions.filter { it.isValid() }
        val seasonStart = validSeasonCompetitions.minOfOrNull { it.startDate } ?: 0L
        val seasonEnd = validSeasonCompetitions.maxOfOrNull { it.endDate } ?: 0L
        breakWeeks = breaks.filter { isMondayIn(seasonStart, seasonEnd, it.week) }
            .associateBy({ it.week }, { it.name })

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = seasonStart
        // Find the first Monday
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        competitionWeeks = mutableMapOf<Short, MutableList<Long>>()

        while (calendar.timeInMillis <= seasonEnd) {
            val millis = calendar.timeInMillis

            if (!breakWeeks.contains(millis)) {
                validSeasonCompetitions.filter { millis in it.startDate..it.endDate }
                    .map { it.competitionId }
                    .forEach { id -> competitionWeeks.getOrPut(id, { mutableListOf() }).add(millis) }

            }

            calendar.add(Calendar.DAY_OF_YEAR, 7)
        }
    }

    fun breakWeeks(): Map<Long, String> = breakWeeks

    fun competitionWeeks(id: Short): List<Long>? = competitionWeeks[id]
}
