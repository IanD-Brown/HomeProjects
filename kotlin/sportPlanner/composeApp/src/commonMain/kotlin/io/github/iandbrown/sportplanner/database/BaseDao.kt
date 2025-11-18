package io.github.iandbrown.sportplanner.database

interface BaseDao<ENTITY> {
    suspend fun insert(entity: ENTITY) : Long

    suspend fun getAll(): List<ENTITY>

    suspend fun count(): Int

    suspend fun delete(entity: ENTITY)

    suspend fun update(entity : ENTITY)
}