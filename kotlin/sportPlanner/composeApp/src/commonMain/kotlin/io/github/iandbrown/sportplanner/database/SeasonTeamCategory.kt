package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "SeasonTeamCategories"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["seasonId", "competitionId", "teamCategoryId"],
    foreignKeys = [
        ForeignKey(
            entity = Season::class,
            parentColumns = ["id"],
            childColumns = ["seasonId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Competition::class,
            parentColumns = ["id"],
            childColumns = ["competitionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamCategory::class,
            parentColumns = ["id"],
            childColumns = ["teamCategoryId"],
            onDelete = ForeignKey.CASCADE
        )]
)
data class SeasonTeamCategory(
    val seasonId: Short,
    val competitionId: Short,
    val teamCategoryId: Short,
    var games: Short,
    var locked: Boolean
)

@Dao
interface SeasonTeamCategoryDao : BaseDao<SeasonTeamCategory> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: SeasonTeamCategory): Long

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonTeamCategory>

    @Query("SELECT * FROM $table WHERE seasonId=:seasonId")
    suspend fun getBySeason(seasonId: Short): List<SeasonTeamCategory>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int
}