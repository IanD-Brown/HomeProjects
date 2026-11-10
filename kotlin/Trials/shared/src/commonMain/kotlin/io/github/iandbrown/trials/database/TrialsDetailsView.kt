package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val viewName = "TrialDetailsView"

@Serializable
@DatabaseView(viewName = viewName,
    value = "SELECT " +
            "n.firstNameOfChild, " +
            "n.surnameOfChild, " +
            "n.gender, " +
            "n.whichYearGroupIsYourChildInNow, " +
            "n.primaryPosition, " +
            "n.secondaryPosition, " +
            "n.foot, " +
            "t.date, " +
            "t.confirmed, " +
            "t.attended, " +
            "t.outcome, " +
            "t.id " +
            "FROM Nominations n, Trials t " +
            "WHERE  t.nominee = n.id " +
            "ORDER BY surnameOfChild, firstNameOfChild"
)
data class TrialDetailsView(
    val firstNameOfChild: String,
    val surnameOfChild: String,
    val gender: String,
    val whichYearGroupIsYourChildInNow: String,
    val primaryPosition: String,
    val secondaryPosition: String,
    val foot: String,
    val date: Int,
    val confirmed: Boolean,
    val attended: Boolean,
    val outcome: String? = null,
    val id: Long = 0
)

@Dao
interface TrialDetailsViewDao : BaseReadDao<TrialDetailsView> {
    @Query("SELECT * FROM $viewName")
    override suspend fun get(): List<TrialDetailsView>

    @Query("SELECT * FROM $viewName WHERE gender = :gender AND whichYearGroupIsYourChildInNow = :yearGroup AND date = :date")
    suspend fun getForTrial(gender: String, yearGroup: String, date: Int): List<TrialDetailsView>

    @Query("UPDATE Trials SET confirmed = 1 - confirmed WHERE id = :id")
    suspend fun toggleConfirmed(id: Long)

    @Query("UPDATE Trials SET attended = 1 - attended WHERE id = :id")
    suspend fun toggleAttended(id: Long)
}
