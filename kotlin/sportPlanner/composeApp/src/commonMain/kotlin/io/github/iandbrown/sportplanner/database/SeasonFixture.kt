package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
    val seasonId : SeasonId,
    val competitionId : CompetitionId,
    val teamCategoryId : TeamCategoryId,
    val date : Int,
    val message : String?,
    val homeAssociationId : AssociationId,
    val homeTeamNumber : TeamNumber,
    val awayAssociationId : AssociationId,
    val awayTeamNumber : TeamNumber,
)

@Dao
interface SeasonFixtureDao : BaseSeasonCompReadDao<SeasonFixture>, BaseWriteDao<SeasonFixture> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId AND competitionId = :competitionId")
    override fun get(seasonId: SeasonId, competitionId: CompetitionId): Flow<List<SeasonFixture>>

    @Query("DELETE FROM $table WHERE seasonId = :seasonId AND teamCategoryId = :teamCategoryId AND competitionId = :competitionId")
    suspend fun deleteBySeasonTeamCategory(seasonId: SeasonId, teamCategoryId: TeamCategoryId, competitionId: CompetitionId)

    @Query("DELETE FROM $table WHERE seasonId = :seasonId")
    suspend fun deleteBySeason(seasonId: SeasonId)
}
