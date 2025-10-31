package org.idb.database

interface BaseDao<ENTITY> {
    suspend fun insert(entity: ENTITY)

    suspend fun getAll(): List<ENTITY>

    suspend fun count(): Int

    suspend fun delete(entity: ENTITY)

    suspend fun update(entity : ENTITY)
}