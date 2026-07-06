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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.AssociationDao
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionDao
import io.github.iandbrown.sportplanner.database.FarAssociationDao
import io.github.iandbrown.sportplanner.database.FarAssociationView
import io.github.iandbrown.sportplanner.database.FarAssociationViewDao
import io.github.iandbrown.sportplanner.database.Season
import io.github.iandbrown.sportplanner.database.SeasonBreakDao
import io.github.iandbrown.sportplanner.database.SeasonCompViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetitionRoundDao
import io.github.iandbrown.sportplanner.database.SeasonTeamCategoryDao
import io.github.iandbrown.sportplanner.database.SeasonTeamDao
import io.github.iandbrown.sportplanner.database.TeamCategory
import io.github.iandbrown.sportplanner.database.TeamCategoryDao
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.at
import org.jetbrains.kotlinx.dataframe.api.insert
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.java.KoinJavaComponent.inject

enum class Editors(val displayName: String, val showOnHome: Boolean = true) {
    SEASON_BREAK("Season Break", false),
    SEASON_COMPETITION_ROUND("Season competition round", false),
    SEASON_TEAMS("Season Teams", false),
    SEASON_TEAM_CATEGORY("", false),
    SEASON_LEAGUE_FIXTURES("League Fixtures"),
    SEASON_CUP_FIXTURES("Cup Fixtures"),
    SEASONS("Seasons"),
    COMPETITIONS("Competitions"),
    TEAM_CATEGORIES("Team Categories"),
    ASSOCIATIONS("Associations"),
    FAR_ASSOCIATIONS("Distant Away Games");

    fun viewRoute(): Route = when (this) {
        SEASON_BREAK -> Route.Home
        SEASON_COMPETITION_ROUND -> Route.Home
        SEASON_TEAMS -> Route.Home
        SEASON_TEAM_CATEGORY -> Route.Home
        SEASON_LEAGUE_FIXTURES -> Route.LeagueFixtures
        SEASON_CUP_FIXTURES -> Route.CupFixtures
        SEASONS -> Route.SeasonList
        COMPETITIONS -> Route.CompetitionList
        TEAM_CATEGORIES -> Route.TeamCategoryList
        ASSOCIATIONS -> Route.AssociationList
        FAR_ASSOCIATIONS -> Route.FarAssociationList
    }

    fun addRoute(): Route = when (this) {
        ASSOCIATIONS -> Route.AssociationEdit(null)
        COMPETITIONS -> Route.CompetitionEdit(null)
        SEASONS -> Route.SeasonEdit(null)
        TEAM_CATEGORIES -> Route.TeamCategoryEdit(null)
        FAR_ASSOCIATIONS -> Route.FarAssociationEdit(null)
        else -> Route.Home
    }

    inline fun <reified T> editRoute(item: T): Route = when (this) {
        ASSOCIATIONS -> Route.AssociationEdit(item as Association)
        COMPETITIONS -> Route.CompetitionEdit(item as Competition)
        SEASONS -> Route.SeasonEdit(item as Season)
        TEAM_CATEGORIES -> Route.TeamCategoryEdit(item as TeamCategory)
        FAR_ASSOCIATIONS -> Route.FarAssociationEdit(item as FarAssociationView)
        else -> Route.Home
    }

    inline fun <reified T> viewRoute(item: T): Route = when (this) {
        SEASON_BREAK -> Route.SeasonBreakList(item as Season)
        SEASON_COMPETITION_ROUND -> Route.SeasonCompetitionRoundList(item as SeasonCompetitionParam)
        SEASON_TEAM_CATEGORY -> Route.SeasonTeamCategory(item as SeasonCompetitionParam)
        SEASON_TEAMS -> Route.SeasonTeams(item as SeasonCompetitionParam)
        SEASON_LEAGUE_FIXTURES -> Route.LeagueFixturesTable(item as Season)
        SEASON_CUP_FIXTURES -> Route.CupFixturesTable(item as Season)
        else -> Route.Home
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val coroutineScope = rememberCoroutineScope()
    var exception by remember { mutableStateOf<Exception?>(null) }

    HomeContent(
        exception = exception,
        onDismissException = { exception = null },
        onExport = {
            coroutineScope.launch {
                try {
                    export()
                    exception = null
                } catch (e: Exception) {
                    logException("HomeScreen", e, "Export failed:")
                    exception = e
                }
            }
        },
        onImport = {
            coroutineScope.launch {
                tryTransaction({
                    logException("HomeScreen", it, "Import failed:")
                    exception = it
                }, { import() })
            }
        },
        onNavigate = { appNavigator.navigate(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    exception: Exception?,
    onDismissException: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onNavigate: (Route) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Season Planner") }, actions = {
                IconButton(onClick = AppState.switchThemeCallback) {
                    Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
                }
            })
        },
        bottomBar = {
            BottomBarWithButtons(
                ButtonSettings(imageVector = Icons.Default.Download) { onExport() },
                ButtonSettings(imageVector = Icons.Default.Upload) { onImport() }
            )
        }
    ) { paddingValues ->
        if (exception != null) {
            AlertDialog(
                onDismissRequest = onDismissException,
                confirmButton = { Button(onClick = onDismissException) { Text("OK") } },
                title = { ViewText("Error") },
                text = { ViewText(exception.message ?: exception.javaClass.simpleName) }
            )
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(
                    items = Editors.entries.filter { it.showOnHome }.toTypedArray(),
                    key = { entry -> entry.ordinal }) { editor ->
                    OutlinedTextButton(value = editor.displayName)
                    { onNavigate(editor.viewRoute()) }
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

private suspend fun export(
    competitionDao: CompetitionDao = inject<CompetitionDao>(CompetitionDao::class.java).value,
    teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>(TeamCategoryDao::class.java).value,
    associationDao: AssociationDao = inject<AssociationDao>(AssociationDao::class.java).value,
    farAssociationDao: FarAssociationViewDao = inject<FarAssociationViewDao>(FarAssociationViewDao::class.java).value,
    seasonCompViewDao: SeasonCompViewDao = inject<SeasonCompViewDao>(SeasonCompViewDao::class.java).value,
    seasonBreaksDao: SeasonBreakDao = inject<SeasonBreakDao>(SeasonBreakDao::class.java).value,
    seasonTeamsDao: SeasonTeamDao = inject<SeasonTeamDao>(SeasonTeamDao::class.java).value,
    seasonTeamCategoriesDao: SeasonTeamCategoryDao = inject<SeasonTeamCategoryDao>(SeasonTeamCategoryDao::class.java).value,
    seasonCompetitionRoundsDao: SeasonCompetitionRoundDao = inject<SeasonCompetitionRoundDao>(SeasonCompetitionRoundDao::class.java).value
) {
    val competitions = competitionDao.get()
    val teamCategories = teamCategoryDao.get()
    val associations = associationDao.get()
    val farAssociations = farAssociationDao.get()
    val seasonCompViews = seasonCompViewDao.get()
    val seasonBreaks = seasonBreaksDao.getAll()
    val seasonTeams = seasonTeamsDao.getAll()
    val seasonTeamCategories = seasonTeamCategoriesDao.getAll()
    val seasonCompetitionRounds = seasonCompetitionRoundsDao.getAll()

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
                toDataFrame(
                    seasonCompViews,
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
    competitionDao: CompetitionDao = inject<CompetitionDao>(CompetitionDao::class.java).value,
    teamCategoryDao: TeamCategoryDao = inject<TeamCategoryDao>(TeamCategoryDao::class.java).value,
    associationDao: AssociationDao = inject<AssociationDao>(AssociationDao::class.java).value,
    farAssociationDao: FarAssociationDao = inject<FarAssociationDao>(FarAssociationDao::class.java).value
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
