package io.github.iandbrown.home_energy.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Usages"

@Serializable
@Entity(tableName = table,
    primaryKeys = ["year", "month", "day", "period", "meterPointAdminNumber"])
data class Usage(
    val year: Short,
    val month: Short,
    val day: Short,
    val period: Short,
    val meterPointAdminNumber: String,
    val consumption: Double)

@Dao
interface UsageDao : BaseReadDao<Usage>, BaseWriteDao<Usage> {
    @Query("SELECT * FROM $table ORDER BY year, month, day, period, meterPointAdminNumber")
    override suspend fun get(): List<Usage>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()
}
