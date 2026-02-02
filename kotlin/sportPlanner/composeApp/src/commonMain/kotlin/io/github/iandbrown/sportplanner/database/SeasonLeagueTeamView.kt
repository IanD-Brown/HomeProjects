package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val viewName = "SeasonLeagueTeamView"

@DatabaseView(viewName = viewName,
    value = "SELECT st.seasonId, " +
            "st.teamCategoryId, " +
            "a.name AS associationName, " +
            "st.competitionId, " +
            "st.count " +
            "FROM SeasonTeams st, Associations a " +
            "WHERE st.competitionId IN (SELECT id FROM Competitions WHERE type = 0) " +
            "AND a.id = st.associationId")

data class SeasonLeagueTeamView(
    val seasonId: SeasonId,
    val teamCategoryId: TeamCategoryId,
    val associationName: String,
    val competitionId: CompetitionId,
    val count: Short)

@Dao
interface SeasonLeagueTeamViewDao : BaseSeasonReadDao<SeasonLeagueTeamView> {
    @Query("SELECT * FROM $viewName WHERE seasonId = :seasonId")
    override fun get(seasonId: SeasonId): Flow<List<SeasonLeagueTeamView>>
}
