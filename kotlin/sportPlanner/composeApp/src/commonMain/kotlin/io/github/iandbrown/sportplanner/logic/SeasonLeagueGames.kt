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
    var message : String? = null
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
        for (competitionId in seasonWeeks.competitions().filter { activeLeagueCompetitions.contains(it) }) {
            val teamCategoriesByMatchDay = seasonTeamCategories
                .filter { it.seasonId == seasonId && it.competitionId == competitionId }
                .filter { !it.locked }
                .filter { it.games > 0 }
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

        return fixtures
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

    private fun scheduleWeek(teamCategories: List<SeasonTeamCategory>, compRoundsForWeekAndSeason: List<SeasonCompRoundView>):
            Map<TeamCategoryId, MutableList<PlannedGame>> =
        teamCategories.associateBy({it.teamCategoryId},
            {teamCategory -> teamCategoryGames(compRoundsForWeekAndSeason, teamCategory)})


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
            if (plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]?.isEmpty() == true ||
                (force && playingTeams.size + 1 >= teamsByCategoryAndCompetition[Pair(teamCategory.teamCategoryId, teamCategory.competitionId)]!!)) {
                break
            }

            val gameComparator = Comparator<PlannedGame> { game1, game2 ->
                val homeTeam1 = teamOf(teamCategory.teamCategoryId, game1, Location.HOME)
                val awayTeam1 = teamOf(teamCategory.teamCategoryId, game1, Location.AWAY)
                val homeTeam2 = teamOf(teamCategory.teamCategoryId, game2, Location.HOME)
                val awayTeam2 = teamOf(teamCategory.teamCategoryId, game2, Location.AWAY)
                val gamesToSchedule1 = gamesToSchedule[homeTeam1]!!.coerceAtLeast(gamesToSchedule[awayTeam1]!!)
                val gamesToSchedule2 = gamesToSchedule[homeTeam2]!!.coerceAtLeast(gamesToSchedule[awayTeam2]!!)

                gamesToSchedule2 - gamesToSchedule1
            }

            val games = plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]?.sortedWith( gameComparator)

            for (plannedGame in games!!) {
                val homeTeam = teamOf(teamCategory.teamCategoryId, plannedGame, Location.HOME)
                val awayTeam = teamOf(teamCategory.teamCategoryId, plannedGame, Location.AWAY)

                if (!playingTeams.contains(homeTeam) && !playingTeams.contains(awayTeam) && isGamePreference(plannedGame, force)) {
                    playingTeams.add(homeTeam)
                    gamesToSchedule[homeTeam] = gamesToSchedule.getOrPut(homeTeam) { 0 } - 1
                    playingTeams.add(awayTeam)
                    gamesToSchedule[awayTeam] = gamesToSchedule.getOrPut(awayTeam) { 0 } - 1

                    plannedGame.message = roundMessage
                    plannedGames.add(plannedGame)
                }
            }
        }

        plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]?.removeAll(plannedGames)

        return plannedGames
    }

    private fun teamOf(teamCategoryId: TeamCategoryId, plannedGame: PlannedGame, location: Location) : Team
        = when (location) {
            Location.HOME -> Team(teamCategoryId, plannedGame.homeAssociationId, plannedGame.homeTeamNumber)
            Location.AWAY -> Team(teamCategoryId, plannedGame.awayAssociationId, plannedGame.awayTeamNumber)
        }


    private fun isGamePreference(plannedGame: PlannedGame, force: Boolean): Boolean {
        if (force || (isGamePreference(plannedGame.homeAssociationId, Location.HOME) && isGamePreference(plannedGame.awayAssociationId, Location.AWAY))) {
            gamePreference[plannedGame.homeTeamNumber] = Location.AWAY
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
