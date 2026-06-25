package io.github.iandbrown.home_energy.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Meters"

@Serializable
@Entity(tableName = table)
data class Meter(
    @PrimaryKey
    val meterPointAdminNumber : String,
    val serial : String)

@Dao
interface MeterDao : BaseReadDao<Meter>, BaseWriteDao<Meter> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Meter>
}
