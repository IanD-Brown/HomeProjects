package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
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
        Index(value = ["state"])
    ]
)
data class Nomination(
    val state: String? = null,
    @Embedded(prefix = "adult_")
    val adultDetails: AdultDetails = AdultDetails(),
    val firstNameOfChild: String,
    val surnameOfChild: String,
    val childDateOfBirth: Int,
    val gender: String,
    val whichSchoolDoesYourChildAttend: String,
    val whichYearGroupIsYourChildInNow: String,
    val primaryPosition: String,
    val secondaryPosition: String,
    val foot: String,
    val doesYourChildPlayForATeamOnTheWeekend: String,
    val whatTypeOfTeamDoTheyPlayFor: String,
    val teamTheyPlayFor: String,
    val medicalConditions: String,
    val iWishMyChildToAttendTrials: String,
    val consentToTheTakingOfPhotographs: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {
    val emailAddress: String get() = adultDetails.emailAddress
}

@Dao
interface NominationDao : BaseReadDao<Nomination>, BaseWriteDao<Nomination> {
    @Query("SELECT * FROM $table ORDER BY surnameOfChild, firstNameOfChild")
    override suspend fun get(): List<Nomination>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
