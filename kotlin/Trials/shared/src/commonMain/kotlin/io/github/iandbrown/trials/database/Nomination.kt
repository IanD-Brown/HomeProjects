package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.serialization.Serializable

private const val table = "Nominations"

@Serializable
@Entity(
    tableName = table,
    foreignKeys = [
        ForeignKey(
            entity = State::class,
            parentColumns = ["name"],
            childColumns = ["state"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["state"]),
        Index(
            value = ["firstNameOfChild", "surnameOfChild", "childDateOfBirth"],
            unique = true
        )
    ]
)
data class Nomination(
    val state: String? = null,
    val firstNameOfChild: String = "",
    val surnameOfChild: String = "",
    val childDateOfBirth: Int = 0,
    val gender: String = "",
    val whichSchoolDoesYourChildAttend: String = "",
    val whichYearGroupIsYourChildInNow: String = "",
    val primaryPosition: String = "",
    val secondaryPosition: String = "",
    val foot: String = "",
    val doesYourChildPlayForATeamOnTheWeekend: Boolean = false,
    val whatTypeOfTeamDoTheyPlayFor: String = "",
    val teamTheyPlayFor: String = "",
    val medicalConditions: String = "",
    val iWishMyChildToAttendTrials: Boolean = false,
    val consentToTheTakingOfPhotographs: Boolean = false,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)

@Dao
interface NominationDao : BaseReadDao<Nomination>, BaseWriteDao<Nomination> {
    @Query("SELECT * FROM $table ORDER BY surnameOfChild, firstNameOfChild")
    override suspend fun get(): List<Nomination>

    @Transaction
    @Query("SELECT * FROM $table ORDER BY surnameOfChild, firstNameOfChild")
    suspend fun getNominationsWithAdultDetails(): List<NominationWithAdultDetails>

    @Transaction
    @Query("SELECT * FROM $table WHERE id = :id")
    suspend fun getNominationWithAdultDetails(id: Int): NominationWithAdultDetails?

    @Query("SELECT * FROM $table WHERE firstNameOfChild = :firstNameOfChild AND surnameOfChild = :surnameOfChild AND childDateOfBirth = :childDateOfBirth")
    suspend fun getByChildDetails(firstNameOfChild: String, surnameOfChild: String, childDateOfBirth: Int): Nomination?

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
