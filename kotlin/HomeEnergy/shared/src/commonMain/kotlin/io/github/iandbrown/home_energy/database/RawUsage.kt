package io.github.iandbrown.home_energy.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "RawUsages"

@Serializable
@Entity(tableName = table,
    primaryKeys = ["year", "day", "period", "month", "meterId"])
data class RawUsage(
    val year: Short,
    val month: Short,
    val day: Short,
    val period: Short,
    val meterId: Int,
    val consumption: Double)

@Dao
interface RawUsageDao : BaseReadDao<RawUsage>, BaseWriteDao<RawUsage> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<RawUsage>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()

    @Query("SELECT * FROM $table WHERE meterId = :meterId")
    suspend fun get(meterId: Int): List<RawUsage>
}
