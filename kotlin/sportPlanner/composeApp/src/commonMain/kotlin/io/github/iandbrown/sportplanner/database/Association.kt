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
    val id : AssociationId = 0,
    val name: String)

@Dao
interface AssociationDao : ConfigReadDao<Association>, BaseWriteDao<Association> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Association>

    @Query("SELECT id FROM $table WHERE name = :name")
    suspend fun getByName(name: String) : AssociationId?

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
