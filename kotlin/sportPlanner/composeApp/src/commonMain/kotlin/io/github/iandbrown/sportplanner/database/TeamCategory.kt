package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "TeamCategories"

@Serializable
@Entity(tableName = table,
    indices = [Index(value = ["name"], unique = true)])
data class TeamCategory (
    @PrimaryKey(autoGenerate = true)
    val id : TeamCategoryId = 0,
    val name: String,
    val matchDay: Short)

@Dao
interface TeamCategoryDao : ConfigReadDao<TeamCategory>, BaseWriteDao<TeamCategory> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<TeamCategory>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getByName(name: String) : TeamCategoryId?
}
