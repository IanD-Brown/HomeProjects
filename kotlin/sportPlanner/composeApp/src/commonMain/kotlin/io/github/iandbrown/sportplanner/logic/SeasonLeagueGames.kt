package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonCompRoundView
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonId
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryId
import io.github.iandbrown.sportplanner.database.TeamNumber
import io.github.iandbrown.sportplanner.ui.MatchStructures
import kotlin.random.Random

private enum class Location { HOME, AWAY }

internal data class Side(val teamCategoryId: TeamCategoryId, val associationId: AssociationId, val teamNumber: TeamNumber) {
    override fun toString(): String = "$associationId / $teamNumber"
}
private val random = Random(System.currentTimeMillis())

internal data class PlannedGame(
    val competitionId : CompetitionId,
    val home : Side,
    val away : Side,
    var message : String? = null,
    val distantAwayGame: Boolean = false,
    var gamesToSchedule : Int = 0,
    var homeGameCount : Int = 0,
    var random : Int = 0
) {
    override fun toString(): String = "($home vs $away)"
}

private enum class Tolerance{NONE, SINGLE, ALL}

internal const val INCOMPLETE = "INCOMPLETE"

private class FixtureScheduler(
    val seasonWeeks: SeasonWeeks,
    allTeamCategories: List<TeamCategory>,
    val seasonTeamCategories: List<SeasonTeamCategory>,
    val seasonCompetitionRounds: List<SeasonCompRoundView>,
    val plannedGamesByTeamCategoryId: MutableMap<TeamCategoryId, MutableList<PlannedGame>>,
    val teamsByCategoryAndCompetition: Map<Pair<TeamCategoryId, CompetitionId>, Int>,
    val activeLeagueCompetitions: Set<CompetitionId>
) {
    val teamCategoryIdToMatchDay: Map<TeamCategoryId, Short> = allTeamCategories.associateBy({ it.id }, {it.matchDay} )
    val gamesToSchedule: MutableMap<Side, Int> = mutableMapOf()
    val associations = mutableSetOf<AssociationId>()

    init {
        plannedGamesByTeamCategoryId.forEach { (teamCategoryId, plannedGames) ->
            plannedGames.forEach { plannedGame ->
                gamesToSchedule.merge(plannedGame.home, 1, Int::plus)
                gamesToSchedule.merge(plannedGame.away, 1, Int::plus)
                associations.add(plannedGame.home.associationId)
                associations.add(plannedGame.away.associationId)
            }
        }
    }

    fun scheduleFixtures(
        seasonId: SeasonId,
        currentHomeFixtureCount: Map<Pair<Int, AssociationId>, Int>
    ) : List<SeasonFixture> {
        val fixtures = mutableListOf<SeasonFixture>()
        val seasonCompRoundsByWeek = seasonCompetitionRounds.groupBy { it.week }
        for (competitionId in seasonWeeks.competitions().filter { activeLeagueCompetitions.contains(it) }) {
            val teamCategoriesByMatchDay = seasonTeamCategories
                .filter { it.seasonId == seasonId && it.competitionId == competitionId }
                .filter { !it.locked }
                .filter { it.games > 0 }
                .groupBy { teamCategoryIdToMatchDay[it.teamCategoryId]!! }

            scheduleWeeks(competitionId, seasonCompRoundsByWeek, teamCategoriesByMatchDay, currentHomeFixtureCount, fixtures, seasonId)
        }

        var missingCount = 0
        for (plannedGameEntry in plannedGamesByTeamCategoryId) {
            if (!plannedGameEntry.value.isEmpty()) {
                missingCount += plannedGameEntry.value.size
                println("Missing fixtures $plannedGameEntry")
                fixtures.add(SeasonFixture(
                    seasonId = seasonId,
                    competitionId = plannedGameEntry.value[0].competitionId,
                    teamCategoryId = plannedGameEntry.key,
                    date = seasonWeeks.competitionWeeks(plannedGameEntry.value[0].competitionId)?.get(0) ?: 0,
                    message = INCOMPLETE,
                    homeAssociationId = 0,
                    homeTeamNumber = 0,
                    awayAssociationId = 0,
                    awayTeamNumber = 0
                ))
            }
        }
        println("Total missing $missingCount")

        return fixtures
    }

    private fun scheduleWeeks(
        competitionId: CompetitionId,
        seasonCompRoundsByWeek: Map<Int, List<SeasonCompRoundView>>,
        teamCategoriesByMatchDay: Map<Short, List<SeasonTeamCategory>>,
        currentHomeFixtureCount: Map<Pair<Int, AssociationId>, Int>,
        fixtures: MutableList<SeasonFixture>,
        seasonId: SeasonId
    ) {
        val playingSidesWithWeek = mutableSetOf<Pair<Side, Int>>()
        val breakWeeksForTeamCategory = mutableMapOf<TeamCategoryId, MutableSet<Int>>()
        val sideLocationByWeek = mutableMapOf<Pair<Side, Int>, Location>()
        for (force in listOf(false, true)) {
            val priorWeeks = mutableListOf<Int>()
            for (week in seasonWeeks.competitionWeeks(competitionId)!!) {
                val compRoundsForWeekAndSeason = seasonCompRoundsByWeek[week] ?: emptyList()
                val findPriorGameLocation = { side: Side ->
                    priorWeeks.map { sideLocationByWeek[Pair(side, it)] }.filterNotNull().firstOrNull()
                }
                val existingHomeFixtures = { day: Short ->
                    currentHomeFixtureCount
                        .filter { it.key.first == DayDate(week).addDays(day.toInt())
                            .value() && associations.contains(it.key.second) }
                        .mapKeys { it.key.second }
                }
                WeekScheduler(
                    compRoundsForWeekAndSeason,
                    teamCategoriesByMatchDay,
                    playingSidesWithWeek,
                    week,
                    force,
                    breakWeeksForTeamCategory,
                    findPriorGameLocation,
                    existingHomeFixtures).processGames {
                    sideLocationByWeek[Pair(it.home, week)] = Location.HOME
                    sideLocationByWeek[Pair(it.away, week)] = Location.AWAY
                    fixtures.add(fixtureOf(seasonId, competitionId, week, it))
                }
                priorWeeks.add(0, week)
            }
        }
    }

    private inner class WeekScheduler(
        val compRoundsForWeekAndSeason: List<SeasonCompRoundView>,
        teamCategoriesByMatchDay: Map<Short, List<SeasonTeamCategory>>,
        val playingSidesWithWeek: MutableSet<Pair<Side, Int>>,
        val week: Int,
        force: Boolean,
        val breakWeeksForTeamCategory: MutableMap<TeamCategoryId, MutableSet<Int>>,
        val findPriorGameLocation: (Side) -> Location?,
        existingHomeFixtures: (Short) -> Map<AssociationId, Int>
    ) {
        val plannedGames = mutableMapOf<TeamCategoryId, MutableList<PlannedGame>>()

        init {
            for (tolerance in if (force) listOf(Tolerance.ALL) else listOf(Tolerance.NONE, Tolerance.SINGLE)) {
                for (entry in teamCategoriesByMatchDay) {
                    val teamCategories = entry.value
                    val homeGameByAssociation = existingHomeFixtures(entry.key).toMutableMap()

                    for (teamCategory in teamCategories) {
                        if (isBreakScheduled(teamCategory) ||
                            scheduleBreak(teamCategory) ||
                            plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]?.isEmpty() == true ||
                            (tolerance != Tolerance.NONE && countPlaying(teamCategory.teamCategoryId).plus(1)
                                    >= teamsByCategoryAndCompetition[Pair(teamCategory.teamCategoryId, teamCategory.competitionId)]!!)) {
                            continue
                        }

                        scheduleGames(
                            getOrderedGames(plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]!!, gamesToSchedule, homeGameByAssociation),
                            teamCategory,
                            homeGameByAssociation,
                            tolerance
                        )
                    }
                }
            }
        }

        fun processGames(handler: (PlannedGame) -> Unit) {
            plannedGames.values.forEach { gameList -> gameList.forEach { handler(it) } }
        }

        private fun isPlaying(side: Side) : Boolean = playingSidesWithWeek.contains(Pair(side, week))

        private fun addPlaying(side: Side) = playingSidesWithWeek.add(Pair(side, week))

        private fun countPlaying(teamCategoryId: TeamCategoryId): Int =
            playingSidesWithWeek.filter { it.second == week && it.first.teamCategoryId == teamCategoryId }.size

        private fun scheduleGames(orderedGames: List<PlannedGame>, teamCategory: SeasonTeamCategory,  homeGameByAssociation: MutableMap<AssociationId, Int>, tolerance: Tolerance) {
            var scheduled = 0
            for (plannedGame in orderedGames) {
                val homeSide = plannedGame.home
                val awaySide = plannedGame.away

                if (!isPlaying(homeSide) && !isPlaying(awaySide) && isGamePreference(tolerance, plannedGame)) {
                    addPlaying(homeSide)
                    gamesToSchedule.merge(homeSide, 1, Int::minus)
                    homeGameByAssociation.merge(plannedGame.home.associationId, 1, Int::plus)
                    addPlaying(awaySide)
                    gamesToSchedule.merge(awaySide, 1, Int::minus)

                    plannedGame.message = getMessage(teamCategory)
                    plannedGames.computeIfAbsent(teamCategory.teamCategoryId) { mutableListOf() }.add(plannedGame)

                    plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]?.remove(plannedGame)
                    ++scheduled
                }
            }
            if (tolerance != Tolerance.NONE && scheduled > 0) {
                println("$tolerance ${DayDate(week)} added $scheduled")
            }
        }

        private fun getMessage(teamCategory: SeasonTeamCategory): String? {
            val compRoundView = compRoundsForWeekAndSeason.firstOrNull { it.teamCategoryId == teamCategory.teamCategoryId }
            return getMessage(compRoundView)
        }

        private fun getMessage(compRoundView: SeasonCompRoundView?): String? =
            if (compRoundView != null && compRoundView.description.isNotEmpty()) {
                "${compRoundView.competitionName} ${compRoundView.description}"
            } else {
                null
            }

        private fun isBreakScheduled(teamCategory: SeasonTeamCategory) : Boolean =
            breakWeeksForTeamCategory[teamCategory.teamCategoryId]?.contains(week) ?: false

        private fun scheduleBreak(teamCategory: SeasonTeamCategory) : Boolean {
            val compRoundView = compRoundsForWeekAndSeason.firstOrNull { it.teamCategoryId == teamCategory.teamCategoryId }
            val roundMessage = getMessage(compRoundView)
            val optional = compRoundView?.optional ?: false

            if (roundMessage != null && !optional) {
                plannedGames.computeIfAbsent(teamCategory.teamCategoryId) {mutableListOf()}
                    .add(PlannedGame(teamCategory.competitionId,
                        Side(teamCategory.teamCategoryId, 0,0),
                        Side(teamCategory.teamCategoryId, 0,0), roundMessage))
                breakWeeksForTeamCategory.merge(teamCategory.teamCategoryId, mutableSetOf(week)) { a, b -> a.apply { addAll(b) } }
                return true
            }
            return false
        }

        private fun isGamePreference(tolerance: Tolerance, plannedGame: PlannedGame): Boolean =
            when (tolerance) {
                Tolerance.NONE -> {
                    findPriorGameLocation(plannedGame.home) != Location.HOME &&
                            findPriorGameLocation(plannedGame.away) != Location.AWAY
                }
                Tolerance.SINGLE -> {
                    findPriorGameLocation(plannedGame.home) != Location.HOME ||
                            findPriorGameLocation(plannedGame.away) != Location.AWAY
                }
                Tolerance.ALL -> {
                    true
                }
            }
    }

    private fun fixtureOf(seasonId: SeasonId, competitionId: CompetitionId, week: Int, game: PlannedGame): SeasonFixture =
        SeasonFixture(
            seasonId = seasonId,
            competitionId = competitionId,
            teamCategoryId = game.home.teamCategoryId,
            date = week,
            message = game.message,
            homeAssociationId = game.home.associationId,
            homeTeamNumber = game.home.teamNumber,
            awayAssociationId = game.away.associationId,
            awayTeamNumber = game.away.teamNumber
        )
}

class SeasonLeagueGames {
    private val plannedGamesByTeamCategoryId = mutableMapOf<TeamCategoryId, MutableList<PlannedGame>>()

    fun prepareGames(
        competitionId: CompetitionId,
        teamCategoryId: TeamCategoryId,
        gameStructure: Short,
        teams: List<SeasonTeam>,
        farAwayGames: Map<AssociationId, Set<AssociationId>>
    ) {
        val allTeams = buildSides(teams)
        plannedGamesByTeamCategoryId[teamCategoryId] = when (gameStructure) {
            MatchStructures.SINGLE.ordinal.toShort() -> prepareSingleGames(competitionId, allTeams, farAwayGames)
            MatchStructures.HOME_AWAY.ordinal.toShort() -> prepareHomeAndAwayGames(competitionId, allTeams, farAwayGames)
            else -> mutableListOf()
        }
     }

    fun scheduleFixtures(
        seasonId: SeasonId,
        seasonWeeks: SeasonWeeks,
        allTeamCategories: List<TeamCategory>,
        seasonTeamCategories: List<SeasonTeamCategory>,
        seasonCompetitionRounds: List<SeasonCompRoundView>,
        teamsByCategoryAndCompetition: Map<Pair<TeamCategoryId, CompetitionId>, Int>,
        activeLeagueCompetitions: Set<CompetitionId>,
        currentHomeFixtureCount: Map<Pair<Int, AssociationId>, Int>
    ) : List<SeasonFixture> =
        FixtureScheduler(seasonWeeks,
            allTeamCategories,
            seasonTeamCategories,
            seasonCompetitionRounds,
            plannedGamesByTeamCategoryId,
            teamsByCategoryAndCompetition,
            activeLeagueCompetitions).scheduleFixtures(seasonId, currentHomeFixtureCount)

    private fun buildSides(teams: List<SeasonTeam>): List<Side> {
        return teams.flatMap { seasonTeam ->
            (1..seasonTeam.count).map { teamNumber ->
                Side(seasonTeam.teamCategoryId, seasonTeam.associationId, teamNumber.toShort())
            }
        }
    }

    private fun prepareHomeAndAwayGames(competitionId: CompetitionId, teams: List<Side>, farAwayGames: Map<AssociationId, Set<AssociationId>>): MutableList<PlannedGame> {
        val games = mutableListOf<PlannedGame>()
        teams.forEach { home ->
            teams.filter { it != home }
                .forEach { away ->
                    games.add(plannedGameOf(competitionId, home, away, farAwayGames))
                }
        }
        return games.shuffled(random).toMutableList()
    }

    private fun plannedGameOf(competitionId: CompetitionId, home: Side, away: Side, farAwayGames: Map<AssociationId, Set<AssociationId>>
    ): PlannedGame = PlannedGame(
        competitionId, home, away,
        distantAwayGame = farAwayGames[away.associationId]?.contains(home.associationId) ?: false
    )

    private fun prepareSingleGames(competitionId: CompetitionId, sides: List<Side>, farAwayGames: Map<AssociationId, Set<AssociationId>>
    ): MutableList<PlannedGame> {
        val games = mutableListOf<PlannedGame>()
        val shuffledTeams = sides.shuffled(random)
        for (i in shuffledTeams.indices) {
            for (j in (i + 1) until shuffledTeams.size) {
                val home: Side
                val away: Side
                if (games.size % 2 == 0) {
                    home = shuffledTeams[i]
                    away = shuffledTeams[j]
                } else {
                    home = shuffledTeams[j]
                    away = shuffledTeams[i]
                }
                games.add(plannedGameOf(competitionId, home, away, farAwayGames))
            }
        }

        return games.shuffled(random).toMutableList()
    }

    internal fun getPlannedGames(teamCategoryId : TeamCategoryId) : List<PlannedGame> = plannedGamesByTeamCategoryId.getOrDefault(teamCategoryId, emptyList())
}

internal fun getOrderedGames(planedGames: List<PlannedGame>,
                             gamesToSchedule: Map<Side, Int>,
                             homeGameByAssociation: Map<AssociationId, Int>): List<PlannedGame> {
    // Add sort criteria
    for (game in planedGames) {
        game.gamesToSchedule = gamesToSchedule[game.home]!!.coerceAtLeast(gamesToSchedule[game.away]!!)
        game.homeGameCount = homeGameByAssociation[game.home.associationId] ?: 0
        game.random = random.nextInt() // Store a random number so the order changes when all else is the same
    }

    // Sort highest games to schedule (from either side), then lowest home games, then distant away games
    return planedGames.sortedWith(compareByDescending<PlannedGame> { it.gamesToSchedule }
        .thenByDescending { it.distantAwayGame }
        .thenBy { it.homeGameCount }
        .thenBy { it.random })
}
