package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Seasons"

@Serializable
@Entity(tableName = table, indices = [Index(value = ["name"], unique = true)])
data class Season(
    @PrimaryKey(autoGenerate = true)
    val id: Short = 0,
    var name: String
)

@Dao
interface SeasonDao : BaseDao<Season> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: Season): Long

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<Season>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int
}