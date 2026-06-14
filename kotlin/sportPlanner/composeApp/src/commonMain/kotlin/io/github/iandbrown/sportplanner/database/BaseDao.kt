package io.github.iandbrown.sportplanner.database

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface ReadDao<ENTITY>

interface ConfigReadDao<ENTITY> : ReadDao<ENTITY> {
    suspend fun get(): List<ENTITY>
}

interface BaseWriteDao<ENTITY> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ENTITY) : Long

    @Delete
    suspend fun delete(entity: ENTITY)

    @Update
    suspend fun update(entity : ENTITY)

    suspend fun deleteAll()
}

interface BaseSeasonReadDao<ENTITY> : ReadDao<ENTITY> {
    suspend fun get(seasonId: SeasonId): List<ENTITY>
}

interface BaseSeasonCompReadDao<ENTITY> : ReadDao<ENTITY> {
    suspend fun get(seasonId: SeasonId, competitionId: CompetitionId): List<ENTITY>
}
