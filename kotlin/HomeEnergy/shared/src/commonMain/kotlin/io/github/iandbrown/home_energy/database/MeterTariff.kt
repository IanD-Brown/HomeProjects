package io.github.iandbrown.home_energy.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "MeterTariffs"

@Serializable
@Entity(tableName = table)
data class MeterTariff(
    val meterId : Int,
    val fromHour: Short,
    val fromPeriod: Short,
    val toHour: Short,
    val toPeriod: Short,
    val tariff: Double,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0)

@Dao
interface MeterTariffDao : BaseReadDao<MeterTariff>, BaseWriteDao<MeterTariff> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<MeterTariff>

    @Query("SELECT * FROM $table WHERE meterId = :meterId")
    suspend fun get(meterId: Int): List<MeterTariff>

    @Query("SELECT * FROM $table")
    suspend fun getAll() : List<MeterTariff>
}
