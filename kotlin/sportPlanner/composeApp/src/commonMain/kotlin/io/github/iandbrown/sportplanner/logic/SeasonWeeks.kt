package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.ui.CompetitionTypes
import java.util.Calendar

/**
 * A read-only reference to a competition, including its type.
 */
data class CompetitionRef(
    val id: Short,
    val name: String,
    val type: CompetitionTypes
)

class SeasonWeeks(
    seasonCompetitions: List<SeasonCompetition>,
    private val breaks: List<SeasonBreak>,
    private val competitions: List<Competition>
) {
    private val validSeasonCompetitions = seasonCompetitions.filter { it.isValid() }
    private val seasonStart = validSeasonCompetitions.minOfOrNull { it.startDate } ?: 0L
    private val seasonEnd = validSeasonCompetitions.maxOfOrNull { it.endDate } ?: 0L

    /**
     * Returns a list of active competitions for the week starting on the given Monday.
     * @param week The timestamp for the Monday of the week to check.
     */
    fun getActiveCompetitions(week: Long): List<CompetitionRef> {
        val activeCompIds = validSeasonCompetitions.filter { week in it.startDate..it.endDate }
            .map { it.competitionId }.toSet()

        return competitions
            .filter { activeCompIds.contains(it.id) }
            .map { comp ->
                CompetitionRef(
                    comp.id,
                    comp.name,
                    CompetitionTypes.entries.getOrElse(comp.type.toInt()) { CompetitionTypes.LEAGUE }
                )
            }
    }

    fun leaguePlayingWeeks(): Int {
        var result = 0
        visitWeeks { monday, message ->
            if (message == null && getActiveCompetitions(monday).any { it.type == CompetitionTypes.LEAGUE }) {
                result++
            }
        }
        return result
    }

    fun visitWeeks(processor: (monday: Long, breakMessage: String?) -> Unit) {
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
            processor(calendar.timeInMillis, breaks.firstOrNull { calendar.timeInMillis == it.week }?.name)

            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
    }
}
