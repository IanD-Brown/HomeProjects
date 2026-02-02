package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonId
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent

class SeasonWeeks {
    companion object : KoinComponent {
        private val db: AppDatabase by KoinJavaComponent.inject(AppDatabase::class.java)

        suspend fun createSeasonWeeks(seasonId: SeasonId): SeasonWeeks {
            return SeasonWeeks(
                db.getSeasonCompViewDao().getAsList(seasonId),
                db.getSeasonBreakDao().getAsList(seasonId)
            )
        }
    }

    private val breakWeeks: Map<Int, String>
    private val competitionWeeks: Map<CompetitionId, List<Int>>

    constructor(seasonCompetitions: List<SeasonCompView>, breaks: List<SeasonBreak>) {
        val validSeasonCompetitions = seasonCompetitions.filter { it.startDate in 1..it.endDate }
        val seasonStart = validSeasonCompetitions.minOfOrNull { it.startDate } ?: 0
        val seasonEnd = validSeasonCompetitions.maxOfOrNull { it.endDate } ?: 0

        var dayDate = DayDate(seasonStart)
        while (dayDate.isValid() && !dayDate.isMonday()) {
            dayDate = dayDate.addDays(1)
        }
        breakWeeks = breaks.filter { DayDate.isMondayIn(IntRange(dayDate.value(), seasonEnd), it.week) }
            .associateBy({ it.week }, { it.name })

        competitionWeeks = mutableMapOf<CompetitionId, MutableList<Int>>()

        while (dayDate.isValid() && dayDate.value() <= seasonEnd) {
            if (!breakWeeks.contains(dayDate.value())) {
                validSeasonCompetitions.filter { dayDate.value() in it.startDate..it.endDate }
                    .map { it.competitionId }
                    .forEach { id -> competitionWeeks.getOrPut(id) { mutableListOf() }.add(dayDate.value()) }

            }

            dayDate = dayDate.addDays(7)
        }
    }

    fun breakWeeks(): Map<Int, String> = breakWeeks

    fun competitionWeeks(id: Short): List<Int>? = competitionWeeks[id]

    fun competitions(): List<Short> = competitionWeeks.keys.toList()
}
