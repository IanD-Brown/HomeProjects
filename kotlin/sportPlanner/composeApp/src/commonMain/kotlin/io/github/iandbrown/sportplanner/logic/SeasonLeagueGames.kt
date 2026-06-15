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

data class Side(val teamCategoryId: TeamCategoryId, val associationId: AssociationId, val teamNumber: TeamNumber) {
    override fun toString(): String = "$associationId / $teamNumber"
}
private typealias GamePreference = MutableMap<AssociationId, Location>
private val random = Random(System.currentTimeMillis())

data class PlannedGame(
    val competitionId : CompetitionId,
    val home : Side,
    val away : Side,
    var message : String? = null,
    val distantAwayGame: Boolean = false,
    var gamesToSchedule : Int = 0,
    var homeGameCount : Int = 0
) {
    override fun toString(): String = "($home vs $away)"
}

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
    val gamePreference: GamePreference = mutableMapOf()
    val gamesToSchedule: MutableMap<Side, Int> = mutableMapOf()

    init {
        plannedGamesByTeamCategoryId.forEach { (teamCategoryId, plannedGames) ->
            plannedGames.forEach { plannedGame ->
                val homeTeam = Side(teamCategoryId, plannedGame.home.associationId, plannedGame.home.teamNumber)
                gamesToSchedule[homeTeam] = gamesToSchedule.getOrPut(homeTeam) { 0 } + 1
                val awayTeam = Side(teamCategoryId, plannedGame.away.associationId, plannedGame.away.teamNumber)
                gamesToSchedule[awayTeam] = gamesToSchedule.getOrPut(awayTeam) { 0 } + 1
            }
        }
    }

    fun scheduleFixtures(seasonId: SeasonId) : List<SeasonFixture> {
        val fixtures = mutableListOf<SeasonFixture>()
        val seasonCompRoundsByWeek = seasonCompetitionRounds.groupBy { it.week }
        for (competitionId in seasonWeeks.competitions().filter { activeLeagueCompetitions.contains(it) }) {
            val teamCategoriesByMatchDay = seasonTeamCategories
                .filter { it.seasonId == seasonId && it.competitionId == competitionId }
                .filter { !it.locked }
                .filter { it.games > 0 }
                .groupBy {teamCategoryIdToMatchDay[it.teamCategoryId]!!}

            for (week in seasonWeeks.competitionWeeks(competitionId)!!) {
                WeekScheduler(seasonCompRoundsByWeek[week] ?: emptyList(), teamCategoriesByMatchDay)
                    .plannedGames
                    .entries
                    .forEach {(_, games) ->
                        games.forEach { game -> fixtures.add(fixtureOf(seasonId, competitionId, week, game)) }}
            }
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
                    message = "INCOMPLETE",
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

    private inner class WeekScheduler(val compRoundsForWeekAndSeason: List<SeasonCompRoundView>, teamCategoriesByMatchDay: Map<Short,List<SeasonTeamCategory>>) {
        val plannedGames = mutableMapOf<TeamCategoryId, MutableList<PlannedGame>>()

        init {
            for (teamCategories in teamCategoriesByMatchDay.values) {
                val playingTeamsByTeamCategory = mutableMapOf<TeamCategoryId, MutableList<Side>>()
                val doneTeamCategories = mutableSetOf<TeamCategoryId>()
                val homeGameByAssociation = mutableMapOf<AssociationId, Int>()

                for (force in listOf(false, true)) {
                    for (teamCategory in teamCategories) {
                        if (doneTeamCategories.contains(teamCategory.teamCategoryId)) {
                            continue
                        }
                        if (!force && scheduleBreak(teamCategory)) {
                            doneTeamCategories.add(teamCategory.teamCategoryId)
                            continue
                        }
                        val playingTeams = playingTeamsByTeamCategory.computeIfAbsent(teamCategory.teamCategoryId) {mutableListOf()}
                        if (plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]?.isEmpty() == true ||
                            (force && playingTeams.size.plus(1) >= teamsByCategoryAndCompetition[Pair(teamCategory.teamCategoryId, teamCategory.competitionId)]!!)) {
                            continue
                        }

                        scheduleGames(getOrderedGames(
                            plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]!!,
                            gamesToSchedule,
                            homeGameByAssociation
                        ), teamCategory, playingTeams, force, homeGameByAssociation)
                    }
                }
            }
        }

        private fun scheduleGames(orderedGames: List<PlannedGame>, teamCategory: SeasonTeamCategory, playingTeams: MutableList<Side>, force: Boolean, homeGameByAssociation: MutableMap<AssociationId, Int>) {
            for (plannedGame in orderedGames) {
                val homeSide = sideOf(teamCategory.teamCategoryId, plannedGame, Location.HOME)
                val awaySide = sideOf(teamCategory.teamCategoryId, plannedGame, Location.AWAY)

                if (!playingTeams.contains(homeSide) && !playingTeams.contains(awaySide) && isGamePreference(plannedGame, force)) {
                    playingTeams.add(homeSide)
                    gamesToSchedule[homeSide] = gamesToSchedule.getOrPut(homeSide) { 0 } - 1
                    homeGameByAssociation[plannedGame.home.associationId] = homeGameByAssociation.getOrPut(plannedGame.home.associationId) { 0 } + 1
                    playingTeams.add(awaySide)
                    gamesToSchedule[awaySide] = gamesToSchedule.getOrPut(awaySide) { 0 } - 1

                    plannedGame.message = compRoundsForWeekAndSeason.firstOrNull { it.teamCategoryId == teamCategory.teamCategoryId }?.description
                    plannedGames.computeIfAbsent(teamCategory.teamCategoryId) { mutableListOf() }.add(plannedGame)

                    plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]?.remove(plannedGame)
                }
            }
        }

        private fun scheduleBreak(teamCategory: SeasonTeamCategory) : Boolean {
            val rounds = compRoundsForWeekAndSeason.filter { it.teamCategoryId == teamCategory.teamCategoryId }
            val roundMessage = rounds.firstOrNull()?.description
            val optional = rounds.firstOrNull()?.optional ?: false

            if (roundMessage != null && !optional) {
                plannedGames.computeIfAbsent(teamCategory.teamCategoryId) {mutableListOf()}
                    .add(PlannedGame(teamCategory.competitionId,
                        Side(teamCategory.teamCategoryId, 0,0),
                        Side(teamCategory.teamCategoryId, 0,0), roundMessage))
                return true
            }
            return false
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

    private fun sideOf(teamCategoryId: TeamCategoryId, plannedGame: PlannedGame, location: Location) : Side
        = when (location) {
            Location.HOME -> Side(teamCategoryId, plannedGame.home.associationId, plannedGame.home.teamNumber)
            Location.AWAY -> Side(teamCategoryId, plannedGame.away.associationId, plannedGame.away.teamNumber)
        }

    private fun isGamePreference(plannedGame: PlannedGame, force: Boolean): Boolean {
        if (force || (isGamePreference(plannedGame.home.associationId, Location.HOME) && isGamePreference(plannedGame.away.associationId, Location.AWAY))) {
            gamePreference[plannedGame.home.associationId] = Location.AWAY
            gamePreference[plannedGame.away.associationId] = Location.HOME
            return true
        }
        return false
    }

    private fun isGamePreference(association: AssociationId, location: Location): Boolean =
        !gamePreference.containsKey(association) || gamePreference[association] == location
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
        activeLeagueCompetitions: Set<CompetitionId>
    ) : List<SeasonFixture> =
        FixtureScheduler(seasonWeeks,
            allTeamCategories,
            seasonTeamCategories,
            seasonCompetitionRounds,
            plannedGamesByTeamCategoryId,
            teamsByCategoryAndCompetition,
            activeLeagueCompetitions).scheduleFixtures(seasonId)

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

    fun getPlannedGames(teamCategoryId : TeamCategoryId) : List<PlannedGame> = plannedGamesByTeamCategoryId.getOrDefault(teamCategoryId, emptyList())
}

internal fun getOrderedGames(planedGames: List<PlannedGame>,
                             gamesToSchedule: Map<Side, Int>,
                             homeGameByAssociation: Map<AssociationId, Int>): List<PlannedGame> {
    // Add sort criteria
    for (game in planedGames) {
        game.gamesToSchedule = gamesToSchedule[game.home]!!.coerceAtLeast(gamesToSchedule[game.away]!!)
        game.homeGameCount = homeGameByAssociation[game.home.associationId] ?: 0
    }

    // Sort highest games to schedule (from either side), then lowest home games, then distant away games
    return planedGames.sortedWith(compareByDescending<PlannedGame> { it.gamesToSchedule }
            .thenBy { it.homeGameCount }
            .thenByDescending { it.distantAwayGame })
}
