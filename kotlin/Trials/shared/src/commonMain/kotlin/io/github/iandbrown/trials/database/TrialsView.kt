package io.github.iandbrown.trials.database

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Query
import kotlinx.serialization.Serializable

private const val viewName = "TrialsView"

@Serializable
@DatabaseView(viewName = viewName,
    value = "SELECT DISTINCT " +
            "tc.name, " +
            "tc.yearGroup, " +
            "tc.gender, " +
            "t.date " +
            "FROM Nominations n, TeamCategories tc, Trials t " +
            "WHERE UPPER(n.whichYearGroupIsYourChildInNow) = tc.yearGroup AND UPPER(n.gender) = tc.gender AND t.nominee = n.id " +
            "ORDER BY name ASC, date DESC"
)
data class TrialsView(
    val name: String,
    val yearGroup: YearGroup,
    val gender: Gender,
    val date: Int
)

@Dao
interface TrialsViewDao : BaseReadDao<TrialsView> {
    @Query("SELECT * FROM $viewName")
    override suspend fun get(): List<TrialsView>
}
