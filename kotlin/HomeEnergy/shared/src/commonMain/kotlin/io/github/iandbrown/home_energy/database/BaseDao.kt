package io.github.iandbrown.home_energy.database

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface ReadDao<ENTITY>

interface BaseReadDao<ENTITY> : ReadDao<ENTITY> {
    suspend fun get(): List<ENTITY>
}

interface BaseWriteDao<ENTITY> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ENTITY) : Long

    @Delete
    suspend fun delete(entity: ENTITY)

    @Update
    suspend fun update(entity : ENTITY)
}
