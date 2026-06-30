package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query

private const val viewName = "SeasonCupSummaryView"

@DatabaseView(viewName = viewName,
    value = "SELECT " +
            "st.seasonId AS seasonId, " +
            "c.name as competitionName, " +
            "tc.name AS teamCategoryName, " +
            "st.count " +
            "FROM Competitions c, TeamCategories tc, SeasonTeams st " +
            "WHERE st.competitionId = c.id AND st.teamCategoryId = tc.id AND c.type = 1 AND st.count > 0 " +
            "ORDER BY competitionId, teamCategoryName"
)
data class SeasonCupSummaryView(
    val seasonId: SeasonId,
    val competitionName: String,
    val teamCategoryName: String,
    val count: Short
)

@Dao
interface SeasonCupSummaryViewDao : BaseSeasonReadDao<SeasonCupSummaryView> {
    @Query("SELECT * FROM $viewName WHERE seasonId = :seasonId")
    override suspend fun get(seasonId : SeasonId): List<SeasonCupSummaryView>
}
