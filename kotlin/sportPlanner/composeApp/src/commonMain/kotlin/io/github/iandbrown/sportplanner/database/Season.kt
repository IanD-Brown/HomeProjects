package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Seasons"

@Serializable
@Entity(tableName = table, indices = [Index(value = ["name"], unique = true)])
data class Season(
    @PrimaryKey(autoGenerate = true)
    val id: Short = 0,
    val name: String
)

@Dao
interface SeasonDao : BaseDao<Season> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<Season>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int
}
