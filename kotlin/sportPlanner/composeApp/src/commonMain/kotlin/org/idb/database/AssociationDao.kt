package org.idb.database

import Association
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

private const val table = "Associations"

@Dao
interface AssociationDao {
    @Insert
    suspend fun insert(association: Association)

    @Query("SELECT * FROM $table")
    suspend fun getAll(): List<Association>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()

    @Query("SELECT count(1) FROM $table")
    suspend fun count(): Int

    @Delete
    suspend fun delete(association: Association)

    /**
     * Updating only name by id
     */
    @Query("UPDATE $table SET name=:name WHERE id=:id")
    suspend fun update(name: String, id: Long)

    @Query("UPDATE $table SET name=:newName WHERE name=:oldName")
    suspend fun rename(oldName: String, newName : String)
}