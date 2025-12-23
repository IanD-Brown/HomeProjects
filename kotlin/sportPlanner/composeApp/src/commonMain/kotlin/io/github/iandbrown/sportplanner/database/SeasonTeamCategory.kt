package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "SeasonTeamCategories"

@Serializable
@Entity(
    tableName = table,
    primaryKeys = ["seasonId", "competitionId", "teamCategoryId"],
    indices = [Index(value = ["seasonId"]), Index(value = ["competitionId"]), Index(value = ["teamCategoryId"])],
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
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonTeamCategory>

    @Query("SELECT * FROM $table WHERE seasonId=:seasonId")
    suspend fun getBySeason(seasonId: Short): List<SeasonTeamCategory>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Query("SELECT * FROM $table "+
            "WHERE seasonId = :seasonId AND competitionId = :competitionId AND locked = 0")
    suspend fun getActiveTeamCategories(seasonId : Short, competitionId : Short) : List<SeasonTeamCategory>
}
