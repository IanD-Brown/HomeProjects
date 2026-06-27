package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.iandbrown.sportplanner.database.AssociationDao
import io.github.iandbrown.sportplanner.database.CompetitionDao
import io.github.iandbrown.sportplanner.database.FarAssociationDao
import io.github.iandbrown.sportplanner.database.FarAssociationViewDao
import io.github.iandbrown.sportplanner.database.SeasonBreakDao
import io.github.iandbrown.sportplanner.database.SeasonCompViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRoundDao
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import io.github.iandbrown.sportplanner.di.inject
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.at
import org.jetbrains.kotlinx.dataframe.api.insert
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.writeJson

enum class Editors(val displayName: String, val showOnHome: Boolean = true) {
    SEASON_BREAK("Season Break", false),
    SEASON_COMPETITION_ROUND("Season competition round", false),
    SEASON_TEAMS("Season Teams", false),
    SEASON_TEAM_CATEGORY("", false),
    SEASON_LEAGUE_FIXTURES("League Fixtures"),
    SEASONS("Seasons"),
    COMPETITIONS("Competitions"),
    TEAM_CATEGORIES("Team Categories"),
    ASSOCIATIONS("Associations"),
    FAR_ASSOCIATIONS("Distant Away Games");

    fun viewRoute() : String = "$name/View"
    fun addRoute() : String = "$name/Add"
    inline fun<reified T> editRoute(item : T) = "$name/${Json.encodeToString(item)}"
    inline fun<reified T> viewRoute(item : T) = "$name/View&${Json.encodeToString(item)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val coroutineScope = rememberCoroutineScope()
    val exceptionState = remember {mutableStateOf<Exception?>(null)}

    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Season Planner") }, actions = {
                IconButton(onClick = AppState.switchThemeCallback) {
                    Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
                }
            })
        },
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings(imageVector = Icons.Default.Download) {
                    coroutineScope.launch {
                        try {
                            export()
                            exceptionState.value = null
                        } catch (exception: Exception) {
                            logException(javaClass.simpleName, exception, "Export failed:")
                            exceptionState.value = exception
                        }
                    }
                },
                ButtonSettings(imageVector = Icons.Default.Upload) {
                    coroutineScope.launch {
                        tryTransaction({
                            logException(javaClass.simpleName, it, "Import failed:")
                            exceptionState.value = it
                        }, { import() })
                    }
                }
            )
        }
    ) { paddingValues ->
        if (exceptionState.value != null) {
            AlertDialog(
                onDismissRequest = { exceptionState.value = null },
                confirmButton = { Button(onClick = { exceptionState.value = null }) { Text("OK") } },
                title = { ViewText("Error") },
                text = { ViewText(exceptionState.value!!.message ?: exceptionState.value!!.javaClass.simpleName) }
            )
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(
                    items = Editors.entries.filter { it.showOnHome }.toTypedArray(),
                    key = { entry -> entry.ordinal }) { editor ->
                    OutlinedTextButton(value = editor.displayName)
                    { appNavController.navigate(editor.viewRoute()) }
                }
            }
        }
    }
}

private const val ENTITY = "entity"
private const val ASSOCIATION = "Association"
private const val COMPETITION = "Competition"
private const val TEAM_CATEGORY = "TeamCategory"
private const val FAR_ASSOCIATION = "FarAssociation"
private const val SEASON = "Season"

private suspend fun export(competitionDao: CompetitionDao = inject<CompetitionDao>().value,
                           teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>().value,
                           associationDao: AssociationDao = inject<AssociationDao>().value,
                           farAssociationDao: FarAssociationViewDao = inject<FarAssociationViewDao>().value,
                           seasonCompViewDao: SeasonCompViewDao = inject<SeasonCompViewDao>().value) {
    val competitions = competitionDao.get()
    val teamCategories = teamCategoryDao.get()
    val associations = associationDao.get()
    val farAssociations = farAssociationDao.get()
    val seasonCompViews = seasonCompViewDao.get()
    val seasonBreaks = inject<SeasonBreakDao>().value.getAll()
    val seasonTeams = inject<SeasonTeamDao>().value.getAll()
    val seasonTeamCategories = inject<SeasonTeamCategoryDao>().value.getAll()
    val seasonCompetitionRounds = inject<SeasonCompetitionRoundDao>().value.getAll()

    exportToFile("setup", extension = "json") { output ->
        (0 until 1).toDataFrame {
            "competitions" from {
                toDataFrame(competitions).insert(ENTITY) { COMPETITION }.at(0)
            }
            "teamCategory" from {
                toDataFrame(teamCategories).insert(ENTITY) { TEAM_CATEGORY }.at(0)
            }
            "associations" from {
                toDataFrame(associations).insert(ENTITY) { ASSOCIATION }.at(0)
            }
            "farAssociations" from {
                toDataFrame(farAssociations).insert(ENTITY) { FAR_ASSOCIATION }.at(0)
            }
            "season" from {
                toDataFrame(seasonCompViews,
                    seasonBreaks,
                    seasonTeams,
                    seasonTeamCategories,
                    seasonCompetitionRounds,
                    competitions,
                    associations,
                    teamCategories
                    ).insert(ENTITY) { SEASON }.at(0)
            }
        }.writeJson(output, true)
    }
}


private suspend fun import(
    competitionDao: CompetitionDao = inject<CompetitionDao>().value,
    teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>().value,
    associationDao: AssociationDao = inject<AssociationDao>().value,
    farAssociationDao: FarAssociationDao = inject<FarAssociationDao>().value
) {
    importFromFile(
        "json",
        {
            val dataFrame = DataFrame.readJson(it)
            competitionDao.deleteAll()
            teamCategoryDao.deleteAll()
            associationDao.deleteAll()
            farAssociationDao.deleteAll()
            dataFrame
        },
        { row ->
            for (cell in row.values()) {
                if (cell is DataFrame<*>) {
                    cell.rows().forEach {
                        when (it[ENTITY]) {
                            COMPETITION -> competitionDao.insert(toCompetition(it))
                            TEAM_CATEGORY -> teamCategoryDao.insert(toTeamCategory(it))
                            ASSOCIATION -> associationDao.insert(toAssociation(it))
                            FAR_ASSOCIATION -> farAssociationDao.insert(toFarAssociation(it))
                            SEASON -> importRow(it)
                        }
                    }
                }
            }
        }
    )

}
