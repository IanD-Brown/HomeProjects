package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Competitions"

@Serializable
@Entity(
    tableName = table,
    indices = [Index(value = ["name"], unique = true)]
)
data class Competition(
    @PrimaryKey(autoGenerate = true)
    val id: Short = 0,
    val name: String,
    val type: Short
)

@Dao
interface CompetitionDao : BaseDao<Competition> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<Competition>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int
}
