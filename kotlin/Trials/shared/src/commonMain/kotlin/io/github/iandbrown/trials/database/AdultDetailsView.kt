package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query

private const val viewName = "AdultDetailsView"

@DatabaseView(viewName = viewName,
    value = "SELECT " +
            "n.nominationId AS nomineeId, " +
            "n.adultDetailsId AS adultId, " +
            "d.type, " +
            "d.emailAddress, " +
            "d.firstNameOfParentOrCarer AS firstName, " +
            "d.surnameOfParentOrCarer as surname " +
            "FROM NominationAdultDetails n, AdultDetails d " +
            "WHERE d.id = n.adultDetailsId "
)
data class AdultDetailsView(
    val nomineeId: Int,
    val adultId: Int,
    val type: AdultType,
    val emailAddress: String,
    val firstName: String,
    val surname: String,
)

@Dao
interface AdultDetailsViewDao : BaseReadDao<AdultDetailsView> {
    @Query("SELECT * FROM $viewName")
    override suspend fun get(): List<AdultDetailsView>

    @Query("SELECT * FROM $viewName WHERE nomineeId = :nomineeId")
    suspend fun getForNomination(nomineeId: Int): List<AdultDetailsView>

}
