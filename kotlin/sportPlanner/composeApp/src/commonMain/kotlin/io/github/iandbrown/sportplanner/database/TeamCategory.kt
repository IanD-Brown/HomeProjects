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
    val id : Short = 0,
    var name: String,
    var matchDay: Short)

@Dao
interface TeamCategoryDao : BaseDao<TeamCategory> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<TeamCategory>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int
}