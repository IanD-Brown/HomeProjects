package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Associations"

@Serializable
@Entity(tableName = table,
    indices = [Index(value = ["name"], unique = true)])
data class Association(
    @PrimaryKey(autoGenerate = true)
    val id : Short = 0,
    var name: String)

@Dao
interface AssociationDao : BaseDao<Association> {
    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<Association>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int
}