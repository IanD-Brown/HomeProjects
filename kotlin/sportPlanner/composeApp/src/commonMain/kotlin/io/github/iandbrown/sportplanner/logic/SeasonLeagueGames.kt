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

private typealias Team = Triple<TeamCategoryId, AssociationId, TeamNumber>
private typealias GamePreference = MutableMap<AssociationId, Location>
private val random = Random(System.currentTimeMillis())

data class PlannedGame(
    val competitionId : CompetitionId,
    val homeAssociationId : AssociationId = 0.toShort(),
    val homeTeamNumber: TeamNumber = 0.toShort(),
    val awayAssociationId : AssociationId = 0.toShort(),
    val awayTeamNumber: TeamNumber = 0.toShort(),
    var message : String? = null,
    var gamesToSchedule : Int = 0,
    var homeGameCount : Int = 0
) {
    override fun toString(): String = "$homeAssociationId vs $awayAssociationId"
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
    val gamesToSchedule: MutableMap<Team, Int> = mutableMapOf()

    init {
        plannedGamesByTeamCategoryId.forEach { (teamCategoryId, plannedGames) ->
            plannedGames.forEach { plannedGame ->
                val homeTeam = Triple(teamCategoryId, plannedGame.homeAssociationId, plannedGame.homeTeamNumber)
                gamesToSchedule[homeTeam] = gamesToSchedule.getOrPut(homeTeam) { 0 } + 1
                val awayTeam = Triple(teamCategoryId, plannedGame.awayAssociationId, plannedGame.awayTeamNumber)
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
                    .forEach {(teamCategoryId, games) ->
                        games.forEach { game -> fixtures.add(fixtureOf(seasonId, competitionId, teamCategoryId, week, game)) }}
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
                val playingTeamsByTeamCategory = mutableMapOf<TeamCategoryId, MutableList<Team>>()
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

                        scheduleGames(getOrderedGames(teamCategory, homeGameByAssociation), teamCategory, playingTeams, force, homeGameByAssociation)
                    }
                }
            }
        }

        private fun getOrderedGames(teamCategory: SeasonTeamCategory, homeGameByAssociation: MutableMap<AssociationId, Int>): List<PlannedGame> {
            // Add sort criteria
            for (game in plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]!!) {
                val homeTeam1 = teamOf(teamCategory.teamCategoryId, game, Location.HOME)
                val awayTeam1 = teamOf(teamCategory.teamCategoryId, game, Location.AWAY)
                game.gamesToSchedule = gamesToSchedule[homeTeam1]!!.coerceAtLeast(gamesToSchedule[awayTeam1]!!)
                game.homeGameCount = homeGameByAssociation[game.homeAssociationId] ?: 0
            }

            return plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]
                ?.sortedWith(compareByDescending<PlannedGame> { it.gamesToSchedule }.thenBy { it.homeGameCount })!!
        }

        private fun scheduleGames(orderedGames: List<PlannedGame>, teamCategory: SeasonTeamCategory, playingTeams: MutableList<Team>, force: Boolean, homeGameByAssociation: MutableMap<AssociationId, Int>) {
            for (plannedGame in orderedGames) {
                val homeTeam = teamOf(teamCategory.teamCategoryId, plannedGame, Location.HOME)
                val awayTeam = teamOf(teamCategory.teamCategoryId, plannedGame, Location.AWAY)

                if (!playingTeams.contains(homeTeam) && !playingTeams.contains(awayTeam) && isGamePreference(plannedGame, force)) {
                    playingTeams.add(homeTeam)
                    gamesToSchedule[homeTeam] = gamesToSchedule.getOrPut(homeTeam) { 0 } - 1
                    homeGameByAssociation[plannedGame.homeAssociationId] = homeGameByAssociation.getOrPut(plannedGame.homeAssociationId) { 0 } + 1
                    playingTeams.add(awayTeam)
                    gamesToSchedule[awayTeam] = gamesToSchedule.getOrPut(awayTeam) { 0 } - 1

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
                    .add(PlannedGame(competitionId = teamCategory.competitionId, message = roundMessage))
                return true
            }
            return false
        }
    }

    private fun fixtureOf(seasonId: SeasonId, competitionId: CompetitionId, teamCategoryId: TeamCategoryId, week: Int, game: PlannedGame): SeasonFixture =
        SeasonFixture(
            seasonId = seasonId,
            competitionId = competitionId,
            teamCategoryId = teamCategoryId,
            date = week,
            message = game.message,
            homeAssociationId = game.homeAssociationId,
            homeTeamNumber = game.homeTeamNumber,
            awayAssociationId = game.awayAssociationId,
            awayTeamNumber = game.awayTeamNumber
        )

    private fun teamOf(teamCategoryId: TeamCategoryId, plannedGame: PlannedGame, location: Location) : Team
        = when (location) {
            Location.HOME -> Team(teamCategoryId, plannedGame.homeAssociationId, plannedGame.homeTeamNumber)
            Location.AWAY -> Team(teamCategoryId, plannedGame.awayAssociationId, plannedGame.awayTeamNumber)
        }

    private fun isGamePreference(plannedGame: PlannedGame, force: Boolean): Boolean {
        if (force || (isGamePreference(plannedGame.homeAssociationId, Location.HOME) && isGamePreference(plannedGame.awayAssociationId, Location.AWAY))) {
            gamePreference[plannedGame.homeAssociationId] = Location.AWAY
            gamePreference[plannedGame.awayAssociationId] = Location.HOME
            return true
        }
        return false
    }

    private fun isGamePreference(association: AssociationId, location: Location): Boolean =
        !gamePreference.containsKey(association) || gamePreference[association] == location
}

class SeasonLeagueGames {
    private val plannedGamesByTeamCategoryId = mutableMapOf<TeamCategoryId, MutableList<PlannedGame>>()

    fun prepareGames(competitionId: CompetitionId, teamCategoryId: TeamCategoryId, gameStructure: Short, teams: List<SeasonTeam>) {
        val allTeams = expandTeams(teams)
        plannedGamesByTeamCategoryId[teamCategoryId] = when (gameStructure) {
            MatchStructures.SINGLE.ordinal.toShort() -> prepareSingleGames(competitionId, allTeams)
            MatchStructures.HOME_AWAY.ordinal.toShort() -> prepareHomeAndAwayGames(competitionId, allTeams)
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

    private fun expandTeams(teams: List<SeasonTeam>): List<Pair<AssociationId, TeamNumber>> {
        return teams.flatMap { seasonTeam ->
            (1..seasonTeam.count).map { teamNumber ->
                Pair(seasonTeam.associationId, teamNumber.toShort())
            }
        }
    }

    private fun prepareHomeAndAwayGames(competitionId: CompetitionId, teams: List<Pair<AssociationId, TeamNumber>>): MutableList<PlannedGame> {
        val games = mutableListOf<PlannedGame>()
        teams.forEach { home ->
            teams.filter { it != home }
                .forEach { away ->
                    games.add(
                        PlannedGame(
                            competitionId = competitionId,
                            homeAssociationId = home.first,
                            homeTeamNumber = home.second,
                            awayAssociationId = away.first,
                            awayTeamNumber = away.second
                        )
                    )
                }
        }
        return games.shuffled(random).toMutableList()
    }

    private fun prepareSingleGames(competitionId: CompetitionId, teams: List<Pair<AssociationId, TeamNumber>>): MutableList<PlannedGame> {
        val games = mutableListOf<PlannedGame>()
        var planHome = true
        val shuffledTeams = teams.shuffled(random)
        for (i in shuffledTeams.indices) {
            for (j in (i + 1) until shuffledTeams.size) {
                val team1 = shuffledTeams[if (planHome) i else j]
                val team2 = shuffledTeams[if (planHome) j else i]
                games.add(PlannedGame(competitionId, team1.first, team1.second, team2.first, team2.second))
                planHome = !planHome
            }
        }
        return games.shuffled(random).toMutableList()
    }

    fun getPlannedGames(teamCategoryId : TeamCategoryId) : List<PlannedGame> = plannedGamesByTeamCategoryId.getOrDefault(teamCategoryId, emptyList())
}
