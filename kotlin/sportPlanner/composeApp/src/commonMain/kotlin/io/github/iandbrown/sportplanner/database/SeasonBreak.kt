package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "SeasonBreaks"

@Serializable
@Entity(tableName = table,
    indices = [Index(value = ["seasonId", "name"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = Season::class,
        parentColumns = ["id"],
        childColumns = ["seasonId"],
        onDelete = ForeignKey.CASCADE)]
)
data class SeasonBreak(
    @PrimaryKey(autoGenerate = true)
    val id: Short = 0,
    val seasonId : Short,
    val name: String,
    val week: Int
)

@Dao
interface SeasonBreakDao : BaseSeasonReadDao<SeasonBreak>, BaseWriteDao<SeasonBreak> {
    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    override fun get(seasonId : SeasonId): Flow<List<SeasonBreak>>

    @Query("SELECT * FROM $table WHERE seasonId = :seasonId")
    suspend fun getAsList(seasonId : SeasonId): List<SeasonBreak>
}
