package io.github.iandbrown.sportplanner.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val viewName = "FarAssociationView"

@Serializable
@DatabaseView(viewName = viewName,
    value = "SELECT " +
            "s.homeAssociation AS homeAssociationId, " +
            "h.name AS homeAssociationName, " +
            "s.awayAssociation AS awayAssociationId, " +
            "a.name AS awayAssociationName " +
            "FROM FarAssociations s, " +
            "Associations h,  " +
            "Associations a " +
            "WHERE h.id = s.homeAssociation AND a.id = s.awayAssociation " +
            "ORDER BY h.name, a.name")
data class FarAssociationView(
    val homeAssociationId : AssociationId,
    val homeAssociationName : String,
    val awayAssociationId : AssociationId,
    val awayAssociationName : String,
)

@Dao
interface FarAssociationViewDao : BaseReadDao<FarAssociationView> {
    @Query("SELECT * FROM $viewName")
    override fun get(): Flow<List<FarAssociationView>>

    @Query("DELETE FROM FarAssociations WHERE homeAssociation = :homeAssociationId AND awayAssociation = :awayAssociationId")
    suspend fun delete(homeAssociationId: AssociationId, awayAssociationId: AssociationId)

    @Query("SELECT * FROM $viewName")
    suspend fun getAll() : List<FarAssociationView>
}
