package io.github.iandbrown.sportplanner.ui

import androidx.navigation3.runtime.NavKey
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.FarAssociationView
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonBreak
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRound
import io.github.iandbrown.sportplanner.database.TeamCategory
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Home : Route

    // Associations
    @Serializable data object AssociationList : Route
    @Serializable data class AssociationEdit(val association: Association? = null) : Route

    // Competitions
    @Serializable data object CompetitionList : Route
    @Serializable data class CompetitionEdit(val competition: Competition? = null) : Route

    // Far Associations
    @Serializable data object FarAssociationList : Route
    @Serializable data class FarAssociationEdit(val farAssociation: FarAssociationView? = null) : Route

    // Seasons
    @Serializable data object SeasonList : Route
    @Serializable data class SeasonEdit(val season: Season? = null) : Route

    // Season Breaks
    @Serializable data class SeasonBreakList(val season: Season) : Route
    @Serializable data class SeasonBreakEdit(val season: Season, val seasonBreak: SeasonBreak? = null) : Route

    // Season Competition Rounds
    @Serializable data class SeasonCompetitionRoundList(val param: SeasonCompetitionParam) : Route
    @Serializable data class SeasonCompetitionRoundEdit(val param: SeasonCompetitionParam, val competitionRound: SeasonCompetitionRound? = null) : Route
    @Serializable data class SeasonCupFixtures(val param: SeasonCompetitionParam, val competitionRound: SeasonCompetitionRound) : Route

    // Fixtures
    @Serializable data object LeagueFixtures : Route
    @Serializable data class LeagueFixturesSummary(val season: Season) : Route
    @Serializable data class LeagueFixturesDate(val season: Season) : Route
    @Serializable data class LeagueFixturesTable(val season: Season) : Route
    @Serializable data object CupFixtures : Route
    @Serializable data class CupFixturesSummary(val season: Season) : Route
    @Serializable data class CupFixturesTable(val season: Season) : Route

    // Teams
    @Serializable data class SeasonTeams(val param: SeasonCompetitionParam) : Route
    @Serializable data class SeasonTeamsByCategory(val param: SeasonCompetitionParam) : Route
    @Serializable data class SeasonTeamCategory(val param: SeasonCompetitionParam) : Route

    // Team Categories
    @Serializable data object TeamCategoryList : Route
    @Serializable data class TeamCategoryEdit(val teamCategory: TeamCategory? = null) : Route
}
