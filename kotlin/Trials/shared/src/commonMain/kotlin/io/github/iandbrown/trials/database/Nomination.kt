package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val table = "Nominations"

@Serializable
@Entity(tableName = table)
data class Nomination(
    val emailAddress:  String,
    val firstNameOfChild:  String,
    val surnameOfChild:  String,
    val childDateOfBirth:  Int,
    val gender:  String,
    val whichSchoolDoesYourChildAttend:  String,
    val whichYearGroupIsYourChildInNow:  String,
    val primaryPosition:  String,
    val secondaryPosition:  String,
    val foot:  String,
    val doesYourChildPlayForATeamOnTheWeekend:  String,
    val whatTypeOfTeamDoTheyPlayFor:  String,
    val teamTheyPlayFor:  String,
    val firstNameOfParentOrCarer:  String,
    val surnameOfParentOrCarer:  String,
    val parentOrCarerMobileNumber:  String,
    val parentOrCarerEmail:  String,
    val confirmEmail:  String,
    val medicalConditions:  String,
    val iWishMyChildToAttendTrials:  String,
    val consentToTheTakingOfPhotographs:  String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

@Dao
interface NominationDao : BaseReadDao<Nomination>, BaseWriteDao<Nomination> {
    @Query("SELECT * FROM $table")
    override suspend fun get(): List<Nomination>

    @Query("DELETE FROM $table")
    override suspend fun deleteAll()
}
