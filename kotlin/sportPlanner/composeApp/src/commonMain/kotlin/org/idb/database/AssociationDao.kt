package org.idb.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

private const val table = "Associations"

@Dao
interface AssociationDao : BaseDao<Association> {
    @Insert
    override suspend fun insert(entity: Association)

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<Association>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Delete
    override suspend fun delete(entity: Association)

    @Update
    override suspend fun update(entity : Association)
}