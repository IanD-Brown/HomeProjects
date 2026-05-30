package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val table = "FarAssociations"

@Serializable
@Entity(tableName = table,
    primaryKeys = ["homeAssociation", "awayAssociation"])
data class FarAssociation(
    val homeAssociation : AssociationId,
    val awayAssociation : AssociationId
) {
}

@Dao
interface FarAssociationDao : BaseReadDao<FarAssociation>, BaseWriteDao<FarAssociation> {
    @Query("SELECT * FROM $table")
    override fun get(): Flow<List<FarAssociation>>
}
