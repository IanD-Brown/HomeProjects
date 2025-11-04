package org.idb.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

private const val table = "TeamCategories"

@Dao
interface TeamCategoryDao : BaseDao<TeamCategory> {
    @Insert
    override suspend fun insert(entity: TeamCategory): Long

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<TeamCategory>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Delete
    override suspend fun delete(entity: TeamCategory)

    @Update
    override suspend fun update(entity : TeamCategory)
}