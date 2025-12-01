package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "SeasonBreaks"

@Serializable
@Entity(tableName = table, indices = [Index(value = ["seasonId", "name"], unique = true)])
data class SeasonBreak(
    @PrimaryKey(autoGenerate = true)
    val id: Short = 0,
    val seasonId : Short,
    var name: String,
    var week: Long
)


@Dao
interface SeasonBreakDao : BaseDao<SeasonBreak> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonBreak>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    suspend fun getBySeason(seasonId: Short): List<SeasonBreak>
}