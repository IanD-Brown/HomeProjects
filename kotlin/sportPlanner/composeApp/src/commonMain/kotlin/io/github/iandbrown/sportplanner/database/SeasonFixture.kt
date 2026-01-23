package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query

private const val table = "SeasonFixtures"

@Entity(tableName = table,
    indices = [
        Index(value = ["seasonId", "teamCategoryId", "date", "homeAssociationId", "homeTeamNumber"], unique = true),
        Index(value = ["seasonId", "teamCategoryId", "date", "awayAssociationId", "awayTeamNumber"], unique = true)
    ],
    foreignKeys = [ForeignKey(
        entity = Season::class,
        parentColumns = ["id"],
        childColumns = ["seasonId"],
        onDelete = ForeignKey.CASCADE)]
)
data class SeasonFixture(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,
    val seasonId : Short,
    val competitionId : Short,
    val teamCategoryId : Short,
    val date : Int,
    val message : String?,
    val homeAssociationId : Short,
    val homeTeamNumber : Short,
    val awayAssociationId : Short,
    val awayTeamNumber : Short,
)

@Dao
interface SeasonFixtureDao : BaseSeasonDao<SeasonFixture> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    override suspend fun get(seasonId: Short): List<SeasonFixture>

    @Query("DELETE FROM $table WHERE seasonId = :seasonId AND teamCategoryId = :teamCategoryId AND competitionId = :competitionId")
    suspend fun deleteBySeasonTeamCategory(seasonId: Short, teamCategoryId: Short, competitionId: Short)

    @Query("DELETE FROM $table WHERE seasonId = :seasonId")
    suspend fun deleteBySeason(seasonId: Short)
}
