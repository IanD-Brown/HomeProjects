package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "FarAssociations"

@Serializable
@Entity(tableName = table,
    primaryKeys = ["homeAssociation", "awayAssociation"])
data class FarAssociation(
    val homeAssociation : AssociationId,
    val awayAssociation : AssociationId
)

@Dao
interface FarAssociationDao : ConfigReadDao<FarAssociation>, BaseWriteDao<FarAssociation> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<FarAssociation>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
