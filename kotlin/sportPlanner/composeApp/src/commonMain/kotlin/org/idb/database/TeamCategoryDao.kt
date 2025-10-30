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
    override suspend fun insert(entity: TeamCategory)

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<TeamCategory>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Delete
    override suspend fun delete(entity: TeamCategory)

    @Query("SELECT * FROM $table WHERE id=:id")
    override suspend fun getById(id: Long): TeamCategory

    @Update
    override suspend fun update(entity : TeamCategory)

    /**
     * Updating only name by id
     */
    @Query("UPDATE $table SET name=:name WHERE id=:id")
    suspend fun update(name: String, id: Long)

    @Query("UPDATE $table SET name=:newName WHERE name=:oldName")
    suspend fun rename(oldName: String, newName : String)
}