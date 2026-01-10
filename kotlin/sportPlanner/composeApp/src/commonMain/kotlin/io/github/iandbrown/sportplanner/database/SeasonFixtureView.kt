package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query

private const val viewName = "SeasonFixtureView"

@DatabaseView(viewName = viewName,
    value = "SELECT f.id AS id," +
        "f.seasonId AS seasonId, " +
        "f.competitionId AS competitionId, " +
        "f.teamCategoryId AS teamCategoryId, " +
        "tc.name AS teamCategoryName, " +
        "f.date, " +
        "f.message, " +
        "h.name AS homeAssociation, " +
        "f.homeTeamNumber AS homeTeamNumber, " +
        "a.name AS awayAssociation, " +
        "f.awayTeamNumber AS awayTeamNumber " +
        "FROM SeasonFixtures f " +
        "LEFT JOIN TeamCategories tc ON tc.id = f.teamCategoryId " +
        "LEFT JOIN Associations h ON h.id = f.homeAssociationId " +
        "LEFT JOIN Associations a ON a.id = f.awayAssociationId")

data class SeasonFixtureView(
    val id : Long,
    val seasonId: Short,
    val competitionId: Short,
    val teamCategoryId: Short,
    val teamCategoryName: String,
    val date: Int,
    val message: String,
    val homeAssociation: String,
    val homeTeamNumber: Short,
    val awayAssociation: String,
    val awayTeamNumber: Short
)

@Dao
interface SeasonFixtureViewDao {
    @Query("SELECT * FROM $viewName WHERE seasonId = :seasonId")
    suspend fun get(seasonId : Short): List<SeasonFixtureView>
}
