package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.ui.CompetitionTypes
import java.util.Calendar

class SeasonWeeks(
    seasonCompetitions: List<SeasonCompetition>,
    private val breaks: List<SeasonBreak>,
    competitions: List<Competition>
) {
    private val seasonStart: Long
    private val seasonEnd: Long
    private val leagueStart: Long
    private val leagueEnd: Long

    init {
        val leagueCompetitionSet = competitions.filter { it.type == CompetitionTypes.LEAGUE.ordinal.toShort() }.map { it.id }.toSet()
        seasonStart = seasonCompetitions.minOfOrNull { it.startDate } ?: 0L
        seasonEnd = seasonCompetitions.maxOfOrNull { it.endDate } ?: 0L
        leagueStart = seasonCompetitions.filter { leagueCompetitionSet.contains(it.competitionId) }.minOfOrNull { it.startDate } ?: 0L
        leagueEnd = seasonCompetitions.filter { leagueCompetitionSet.contains(it.competitionId) }.maxOfOrNull { it.endDate } ?: 0L
    }

    fun leaguePlayingWeeks(): Int {
        var result = 0
        visitWeeks { _, message, isLeague -> if (isLeague && message == null) result++ }
        return result
    }

    fun visitWeeks(processor: (monday: Long, message: String?, isLeague: Boolean) -> Unit) {
        if (seasonStart == 0L) {
            return
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = seasonStart
        // Find the first Monday
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        while (calendar.timeInMillis <= seasonEnd) {
            val currentMonday = calendar.timeInMillis
            val breakForWeek = breaks.firstOrNull { currentMonday == it.week }
            processor(currentMonday, breakForWeek?.name, currentMonday in leagueStart..leagueEnd)
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
    }
}
