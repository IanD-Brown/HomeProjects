package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query

@DatabaseView(viewName = "SeasonFixtureView",
    value = "SELECT f.id AS id," +
        "f.seasonId AS seasonId, " +
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
    val teamCategoryName: String,
    val date: Long,
    val message: String,
    val homeAssociation: String,
    val homeTeamNumber: Short,
    val awayAssociation: String,
    val awayTeamNumber: Short
)

@Dao
interface SeasonFixtureViewDao {
    @Query("SELECT * FROM SeasonFixtureView WHERE seasonId = :seasonId")
    suspend fun get(seasonId : Short): List<SeasonFixtureView>
}
