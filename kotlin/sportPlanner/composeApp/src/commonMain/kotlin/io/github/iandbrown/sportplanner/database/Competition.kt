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
    val id: CompetitionId = 0,
    val name: String,
    val type: Short
)

@Dao
interface CompetitionDao : ConfigReadDao<Competition>, BaseWriteDao<Competition> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Competition>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getByName(name : String) : CompetitionId?
}
