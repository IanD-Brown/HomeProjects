package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent

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

    private val breakWeeks: Map<Int, String>
    private val competitionWeeks: Map<Short, List<Int>>

    constructor(seasonCompetitions: List<SeasonCompetition>, breaks: List<SeasonBreak>) {
        val validSeasonCompetitions = seasonCompetitions.filter { it.isValid() }
        val seasonStart = validSeasonCompetitions.minOfOrNull { it.startDate } ?: 0
        val seasonEnd = validSeasonCompetitions.maxOfOrNull { it.endDate } ?: 0

        var dayDate = DayDate(seasonStart)
        while (dayDate.isValid() && !dayDate.isMonday()) {
            dayDate = dayDate.addDays(1)
        }
        breakWeeks = breaks.filter { DayDate.isMondayIn(dayDate.value(), seasonEnd, it.week) }
            .associateBy({ it.week }, { it.name })

        competitionWeeks = mutableMapOf<Short, MutableList<Int>>()

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
