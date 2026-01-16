package io.github.iandbrown.sportplanner.database

interface ReadonlyDao<ENTITY> {
    suspend fun getAll(): List<ENTITY>
}
