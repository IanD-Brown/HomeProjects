package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonCompRoundView
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryId
import io.github.iandbrown.sportplanner.database.TeamNumber
import kotlin.collections.component1
import kotlin.collections.component2

private enum class Location { HOME, AWAY }

private typealias Team = Triple<TeamCategoryId, AssociationId, TeamNumber>
private typealias GamePreference = MutableMap<Team, Location>

data class PlannedGame(
    val competitionId : Short,
    val homeAssociationId : Short = 0.toShort(),
    val homeTeamNumber: Short = 0.toShort(),
    val awayAssociationId : Short = 0.toShort(),
    val awayTeamNumber: Short = 0.toShort(),
    var message : String? = null
)

private class FixtureScheduler(
    val seasonWeeks: SeasonWeeks,
    allTeamCategories: List<TeamCategory>,
    val seasonTeamCategories: List<SeasonTeamCategory>,
    val seasonCompetitionRounds: List<SeasonCompRoundView>,
    val plannedGamesByTeamCategoryId: MutableMap<Short, MutableList<PlannedGame>>,
    val teamsByCategoryAndCompetition: Map<Pair<Short, Short>, Int>,
    val activeLeagueCompetitions: Set<Short>
) {
    val teamCategoryIdToMatchDay: Map<Short, Short> = allTeamCategories.associateBy({ it.id }, {it.matchDay} )
    val gamePreference: GamePreference = mutableMapOf()

    fun scheduleFixtures(seasonId: Short) : List<SeasonFixture> {
        val fixtures = mutableListOf<SeasonFixture>()
        for (competitionId in seasonWeeks.competitions().filter { activeLeagueCompetitions.contains(it) }) {
            val teamCategoriesByMatchDay = seasonTeamCategories
                .filter { it.seasonId == seasonId && it.competitionId == competitionId }
                .filter { !it.locked }
                .groupBy {teamCategoryIdToMatchDay[it.teamCategoryId]!!}

            for (week in seasonWeeks.competitionWeeks(competitionId)!!) {
                for (teamCategories in teamCategoriesByMatchDay.values) {
                    val compRoundsForWeekAndSeason = seasonCompetitionRounds
                        .filter { it.week == week }
                    scheduleWeek(teamCategories, compRoundsForWeekAndSeason)
                        .forEach { (teamCategoryId, games) ->
                            games.forEach { game -> fixtures.add(fixtureOf(seasonId, competitionId, teamCategoryId, week, game))}}
                }
            }
        }

        for (plannedGameEntry in plannedGamesByTeamCategoryId) {
            if (!plannedGameEntry.value.isEmpty()) {
                println("Missing fixtures $plannedGameEntry")
            }
        }

        return fixtures
    }

    private fun fixtureOf(seasonId: Short, competitionId: Short, teamCategoryId: Short, week: Int, game: PlannedGame): SeasonFixture =
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

    private fun scheduleWeek(teamCategories: List<SeasonTeamCategory>, compRoundsForWeekAndSeason: List<SeasonCompRoundView>)
            : Map<Short, MutableList<PlannedGame>> {
        return teamCategories.associateBy({it.teamCategoryId},
            {teamCategory -> teamCategoryGames(compRoundsForWeekAndSeason, teamCategory)})
    }

    private fun teamCategoryGames(compRoundsForWeekAndSeason: List<SeasonCompRoundView>, teamCategory: SeasonTeamCategory): MutableList<PlannedGame> {
        val rounds = compRoundsForWeekAndSeason.filter { it.teamCategoryId == teamCategory.teamCategoryId }
        val roundMessage = rounds.firstOrNull()?.description
        val optional = rounds.firstOrNull()?.optional ?: false

        if (roundMessage != null && !optional) {
            return mutableListOf(PlannedGame(competitionId = teamCategory.competitionId, message = roundMessage))
        }
        val plannedGames = mutableListOf<PlannedGame>()
        val playingTeams = mutableSetOf<Team>()
        for (force in listOf(false, true)) {
            if (force && (plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]?.isEmpty() == true ||
                        playingTeams.size + 1 >= teamsByCategoryAndCompetition[Pair(teamCategory.teamCategoryId, teamCategory.competitionId)]!!)) {
                break
            }
            val plannedGameIterator = plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]!!.iterator()
            while (plannedGameIterator.hasNext()) {
                val plannedGame = plannedGameIterator.next()
                val homeTeam = teamOf(teamCategory.teamCategoryId, plannedGame, Location.HOME)
                val awayTeam = teamOf(teamCategory.teamCategoryId, plannedGame, Location.AWAY)

                if (!playingTeams.contains(homeTeam) && !playingTeams.contains(awayTeam) && isGamePreference(homeTeam, awayTeam, force)) {
                    plannedGameIterator.remove()

                    playingTeams.add(homeTeam)
                    playingTeams.add(awayTeam)

                    plannedGame.message = roundMessage
                    plannedGames.add(plannedGame)
                }
            }
        }

        return plannedGames
    }

    private fun teamOf(teamCategoryId: TeamCategoryId, plannedGame: PlannedGame, location: Location) : Team
        = when (location) {
            Location.HOME -> Team(teamCategoryId, plannedGame.homeAssociationId, plannedGame.homeTeamNumber)
            Location.AWAY -> Team(teamCategoryId, plannedGame.awayAssociationId, plannedGame.awayTeamNumber)
        }


    private fun isGamePreference(homeTeam: Team, awayTeam: Team, force: Boolean): Boolean {
        if (force || (!gamePreference.containsKey(homeTeam) || gamePreference[homeTeam] == Location.HOME) &&
            (!gamePreference.containsKey(awayTeam) || gamePreference[awayTeam] == Location.AWAY)) {
            gamePreference[homeTeam] = Location.AWAY
            gamePreference[awayTeam] = Location.HOME
            return true
        }
        return false
    }
}

class SeasonLeagueGames {
    private val plannedGamesByTeamCategoryId = mutableMapOf<Short, MutableList<PlannedGame>>()

    fun prepareGames(competitionId: Short, teamCategoryId: Short, gameStructure: Short, teams: List<SeasonTeam>) {
        val allTeams = expandTeams(teams)
        plannedGamesByTeamCategoryId[teamCategoryId] = when (gameStructure) {
            1.toShort() -> prepareSingleGames(competitionId, allTeams)
            2.toShort() -> prepareHomeAndAwayGames(competitionId, allTeams)
            else -> mutableListOf()
        }
     }

    fun scheduleFixtures(
        seasonId: Short,
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

    private fun expandTeams(teams: List<SeasonTeam>): List<Pair<Short, Short>> {
        return teams.flatMap { seasonTeam ->
            (1..seasonTeam.count).map { teamNumber ->
                Pair(seasonTeam.associationId, teamNumber.toShort())
            }
        }
    }

    private fun prepareHomeAndAwayGames(competitionId: Short, teams: List<Pair<Short, Short>>): MutableList<PlannedGame> {
        val games = mutableListOf<PlannedGame>()
        val homeShuffle = teams.shuffled()
        val awayShuffle = homeShuffle.shuffled()
        homeShuffle.forEach { home ->
            awayShuffle.filter { it != home }
                .forEach { away -> games.add(PlannedGame(
                    competitionId = competitionId,
                    homeAssociationId = home.first,
                    homeTeamNumber = home.second,
                    awayAssociationId = away.first,
                    awayTeamNumber = away.second
                )) }
        }
        return games.shuffled().toMutableList()
    }

    private fun prepareSingleGames(competitionId: Short, teams: List<Pair<Short, Short>>): MutableList<PlannedGame> {
        val games = mutableListOf<PlannedGame>()
        var planHome = true
        val shuffledTeams = teams.shuffled()
        for (i in shuffledTeams.indices) {
            for (j in (i + 1) until shuffledTeams.size) {
                val team1 = shuffledTeams[if (planHome) i else j]
                val team2 = shuffledTeams[if (planHome) j else i]
                games.add(PlannedGame(competitionId, team1.first, team1.second, team2.first, team2.second))
                planHome = !planHome
            }
        }
        return games.shuffled().toMutableList()
    }

    fun getPlannedGames(teamCategoryId : Short) : List<PlannedGame> = plannedGamesByTeamCategoryId.getOrDefault(teamCategoryId, emptyList())
}
