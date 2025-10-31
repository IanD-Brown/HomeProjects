package org.idb.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

private const val table = "Seasons"

@Dao
interface SeasonDao : BaseDao<Season> {
    @Insert
    override suspend fun insert(entity: Season)

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<Season>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Delete
    override suspend fun delete(entity: Season)

    @Query("SELECT * FROM $table WHERE id=:id")
    override suspend fun getById(id: Long): Season

    @Update
    override suspend fun update(entity : Season)
}