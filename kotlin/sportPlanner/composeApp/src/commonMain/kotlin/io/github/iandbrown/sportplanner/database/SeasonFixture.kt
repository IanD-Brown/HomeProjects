package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query


private const val table = "SeasonFixtures"

@Entity(tableName = table,
    indices = [
        Index(value = ["seasonId", "teamCategoryId", "date", "homeAssociationId", "homeTeamNumber"], unique = true),
        Index(value = ["seasonId", "teamCategoryId", "date", "awayAssociationId", "awayTeamNumber"], unique = true)
    ])
data class SeasonFixture(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,
    val seasonId : Short,
    val teamCategoryId : Short,
    val date : Long,
    val homeAssociationId : Short,
    val homeTeamNumber : Short,
    val awayAssociationId : Short,
    val awayTeamNumber : Short,
)

@Dao
interface SeasonFixtureDao : BaseDao<SeasonFixture> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonFixture>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Query("SELECT * FROM $table where seasonId = :seasonId and teamCategoryId = :teamCategoryId")
    suspend fun getBySeasonTeamCategory(seasonId: Short, teamCategoryId: Short): List<SeasonFixture>

    @Query("SELECT * FROM $table where seasonId = :seasonId and (homeAssociationId = :associationId or awayAssociationId = :associationId)")
    suspend fun getBySeasonAssociation(seasonId: Short, associationId: Short): List<SeasonFixture>

    @Query("DELETE FROM $table where seasonId = :seasonId and teamCategoryId = :teamCategoryId")
    suspend fun deleteBySeasonTeamCategory(seasonId: Short, teamCategoryId: Short)

    @Query("DELETE FROM $table where seasonId = :seasonId")
    suspend fun deleteBySeason(seasonId: Short)
}