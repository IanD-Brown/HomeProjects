package org.idb.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.serialization.Serializable

private const val table = "SeasonBreaks"

@Serializable
@Entity(tableName = table, indices = [Index(value = ["name"], unique = true)])
data class SeasonBreak(
    @PrimaryKey(autoGenerate = true)
    val id: Short = 0,
    var name: String,
    var week: Long
)


@Dao
interface SeasonBreakDao : BaseDao<SeasonBreak> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: SeasonBreak): Long

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonBreak>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Delete
    override suspend fun delete(entity: SeasonBreak)

    @Update
    override suspend fun update(entity : SeasonBreak)
}