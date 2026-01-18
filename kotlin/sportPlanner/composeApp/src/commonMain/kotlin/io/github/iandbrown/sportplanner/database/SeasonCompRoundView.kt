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
            "stc.teamCategoryId " +
            "FROM SeasonCompetitionRounds r, SeasonTeamCategories stc " +
            "WHERE stc.seasonId = r.seasonId " +
            "AND stc.competitionId = r.competitionId " +
            "AND stc.locked = 0")

data class SeasonCompRoundView(
    val seasonId: Short,
    val description: String,
    val week: Int,
    val optional: Boolean,
    val teamCategoryId: Short
)

@Dao
interface SeasonCompRoundViewDao {
    @Query("SELECT * FROM $viewName WHERE seasonId = :seasonId")
    suspend fun getBySeason(seasonId : Short): List<SeasonCompRoundView>
}
