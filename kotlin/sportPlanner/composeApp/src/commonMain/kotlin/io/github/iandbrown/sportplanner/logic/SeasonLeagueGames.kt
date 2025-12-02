package io.github.iandbrown.sportplanner.logic

import io.github.iandbrown.sportplanner.database.SeasonCompRoundView
import io.github.iandbrown.sportplanner.database.SeasonFixture
import io.github.iandbrown.sportplanner.database.SeasonTeam
import io.github.iandbrown.sportplanner.database.SeasonTeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategory
import kotlin.collections.iterator

private enum class Location { HOME, AWAY }

data class PlannedGame(
    val competitionId : Short,
    val homeAssociationId : Short = 0.toShort(),
    val homeTeamNumber: Short = 0.toShort(),
    val awayAssociationId : Short = 0.toShort(),
    val awayTeamNumber: Short = 0.toShort(),
    var message : String? = null
)

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

    fun scheduleFixtures(seasonId : Short,
                         seasonWeeks : SeasonWeeks,
                         allTeamCategories : List<TeamCategory>,
                         seasonTeamCategories : List<SeasonTeamCategory>,
                         seasonCompetitionRounds: List<SeasonCompRoundView>) : List<SeasonFixture> {
        val fixtures = mutableListOf<SeasonFixture>()

        val teamCategoryIdToMatchDay = allTeamCategories.associateBy({ it.id }, {it.matchDay} )

        for (competitionId in seasonWeeks.competitions()) {
            val teamCategoriesByMatchDay = seasonTeamCategories
                .filter { it.seasonId == seasonId && it.competitionId == competitionId }
                .groupBy {teamCategoryIdToMatchDay[it.teamCategoryId]!!}

            for (week in seasonWeeks.competitionWeeks(competitionId)!!) {
                for (teamCategories in teamCategoriesByMatchDay.values) {
                    val compRoundsForWeekAndSeason = seasonCompetitionRounds
                        .filter { it.seasonId == seasonId && it.week == week }
                    for (teamPlan in scheduleWeek(teamCategories, compRoundsForWeekAndSeason)) {
                        for (game in teamPlan.value) {
                            fixtures.add(
                                SeasonFixture(
                                    seasonId = seasonId,
                                    teamCategoryId = teamPlan.key,
                                    competitionId = competitionId,
                                    date = week,
                                    homeAssociationId = game.homeAssociationId,
                                    homeTeamNumber = game.homeTeamNumber,
                                    awayAssociationId = game.awayAssociationId,
                                    awayTeamNumber = game.awayTeamNumber,
                                    message = game.message
                                )
                            )
                        }
                    }
                }
            }
        }

        return fixtures
    }

    private fun scheduleWeek(teamCategories: List<SeasonTeamCategory>, compRoundsForWeekAndSeason: List<SeasonCompRoundView>)
    : Map<Short, MutableList<PlannedGame>> {
        val gamePreference = mutableMapOf<Short, Location>()
        return teamCategories.associateBy({it.teamCategoryId},
            {teamCategory -> teamCategoryGames(compRoundsForWeekAndSeason, teamCategory, gamePreference)})
    }

    private fun teamCategoryGames(compRoundsForWeekAndSeason: List<SeasonCompRoundView>, teamCategory: SeasonTeamCategory, gamePreference: MutableMap<Short, Location>): MutableList<PlannedGame> {
         val rounds = compRoundsForWeekAndSeason.filter { it.teamCategoryId == teamCategory.teamCategoryId }
        val roundMessage = rounds.firstOrNull()?.description
        val optional = rounds.firstOrNull()?.optional ?: false

        if (roundMessage != null && !optional) {
            return mutableListOf(PlannedGame(competitionId = teamCategory.competitionId, message = roundMessage))
        }
        val plannedGames = mutableListOf<PlannedGame>()
        val playingTeams = mutableSetOf<Pair<Short, Short>>()
        val plannedGameIterator = plannedGamesByTeamCategoryId[teamCategory.teamCategoryId]!!.iterator()
        while (plannedGameIterator.hasNext()) {
            val plannedGame = plannedGameIterator.next()
            val homeTeam = Pair(plannedGame.homeAssociationId, plannedGame.homeTeamNumber)
            val awayTeam = Pair(plannedGame.awayAssociationId, plannedGame.awayTeamNumber)

            if (!playingTeams.contains(homeTeam) && !playingTeams.contains(awayTeam)) {
                if ((!gamePreference.containsKey(homeTeam.first) || gamePreference[homeTeam.first] == Location.HOME) &&
                    (!gamePreference.containsKey(awayTeam.first) || gamePreference[awayTeam.first] == Location.AWAY)
                ) {
                    plannedGameIterator.remove()

                    playingTeams.add(homeTeam)
                    playingTeams.add(awayTeam)
                    gamePreference[homeTeam.first] = Location.AWAY
                    gamePreference[awayTeam.first] = Location.HOME

                    plannedGame.message = roundMessage
                    plannedGames.add(plannedGame)
                }
            }
        }

        return plannedGames
    }

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
