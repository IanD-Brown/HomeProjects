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
    val id: SeasonId = 0,
    val name: String
)

@Dao
interface SeasonDao : ConfigReadDao<Season>, BaseWriteDao<Season> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Season>

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getSeasonId(name : String) : SeasonId?

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
