package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query

private const val viewName = "SeasonCompRoundView"

// Used as an input into scheduling fixtures via direct dao usage (not view a ViewModel)
@DatabaseView(viewName = viewName,
    value = "SELECT " +
            "r.seasonId AS seasonId, " +
            "r.description AS description, " +
            "r.week AS week, " +
            "r.optional AS optional, " +
            "st.teamCategoryId, " +
            "c.name as competitionName  " +
            "FROM SeasonCompetitionRounds r, SeasonTeams st, Competitions c " +
            "WHERE st.seasonId = r.seasonId " +
            "AND st.competitionId = r.competitionId " +
            "AND st.count > 0 " + "" +
            "AND c.id = r.competitionId "
)

data class SeasonCompRoundView(
    val seasonId: SeasonId,
    val description: String,
    val week: Int,
    val optional: Boolean,
    val teamCategoryId: TeamCategoryId,
    val competitionName: String
)

@Dao
interface SeasonCompRoundViewDao : BaseSeasonReadDao<SeasonCompRoundView> {
    @Query("SELECT * FROM $viewName WHERE seasonId = :seasonId")
    override suspend fun get(seasonId : SeasonId): List<SeasonCompRoundView>
}
