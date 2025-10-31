package org.idb.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

private const val table = "SeasonTeams"

@Dao
interface SeasonTeamDao : BaseDao<SeasonTeam> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: SeasonTeam)

    @Query("SELECT * FROM $table")
    override suspend fun getAll(): List<SeasonTeam>

    @Query("SELECT count(1) FROM $table")
    override suspend fun count(): Int

    @Delete
    override suspend fun delete(entity: SeasonTeam)

    @Update
    override suspend fun update(entity : SeasonTeam)
}