package io.github.iandbrown.sportplanner.database

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface BaseDao<ENTITY> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ENTITY) : Long

    suspend fun getAll(): List<ENTITY>

    suspend fun count(): Int

    @Delete
    suspend fun delete(entity: ENTITY)

    @Update
    suspend fun update(entity : ENTITY)
}