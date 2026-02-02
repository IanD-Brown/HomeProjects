package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "TeamCategories"

@Serializable
@Entity(tableName = table,
    indices = [Index(value = ["name"], unique = true)])
data class TeamCategory (
    @PrimaryKey(autoGenerate = true)
    val id : Short = 0,
    val name: String,
    val matchDay: Short)

@Dao
interface TeamCategoryDao : BaseReadDao<TeamCategory>, BaseWriteDao<TeamCategory> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<TeamCategory>>

    @Query("SELECT * FROM $table")
    suspend fun getAsList() : List<TeamCategory>
}
