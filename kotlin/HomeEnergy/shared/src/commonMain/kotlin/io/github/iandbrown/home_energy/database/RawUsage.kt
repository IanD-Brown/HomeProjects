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
    val consumption: Double) {

    fun humanReadable(meterName: (Int) -> String) = "${meterName(meterId)} ${date()} $consumption"

    private fun date() = "${str(year, 4)}/${str(month)}/${str(day)} ${str((period / 48).toShort())}:${str((30 * (period % 2)).toShort())}"

    private fun str(value: Short, places: Int = 2) = value.toString().padStart(places, '0')
}

@Dao
interface RawUsageDao : BaseReadDao<RawUsage>, BaseWriteDao<RawUsage> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<RawUsage>

    @Query("DELETE FROM $table")
    suspend fun deleteAll()

    @Query("SELECT * FROM $table WHERE meterId = :meterId")
    suspend fun get(meterId: Int): List<RawUsage>

    @Query("SELECT * FROM $table WHERE year = :year AND month = :month AND day = :day AND period = :period AND meterId = :meterId")
    suspend fun get(year: Short, month: Short, day: Short, period: Short, meterId: Int): List<RawUsage>
}
