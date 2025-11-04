package org.idb.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
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
    var name: String,
    var type: Short
)

@Dao
interface CompetitionDao : BaseDao<Competition> {
    @Insert
    override suspend fun insert(entity: Competition): Long

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<Competition>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Delete
    override suspend fun delete(entity: Competition)

    @Update
    override suspend fun update(entity : Competition)
}