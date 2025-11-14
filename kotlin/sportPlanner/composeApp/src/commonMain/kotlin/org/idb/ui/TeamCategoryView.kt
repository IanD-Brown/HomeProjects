package org.idb.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.idb.database.AppDatabase
import org.idb.database.TeamCategory
import org.idb.database.TeamCategoryDao
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

class TeamCategoryViewModel : BaseViewModel<TeamCategoryDao, TeamCategory>() {
    override fun getDao(db: AppDatabase): TeamCategoryDao = db.getTeamCategoryDao()
}

private val editor = Editors.TEAM_CATEGORIES
private enum class Day(val display : String) {
    MON("Mon"),
    TUES("Tues"),
    WEDS("Weds"),
    THURS("Thurs"),
    FRI("Fri"),
    SAT("Sat"),
    SUN("Sun")
}

@Composable
fun navigateTeamCategory(navController : NavController, argument : String?) {
    when (argument) {
        "View" -> teamCategoryEditor(navController)
        "Add" -> addTeamCategory(navController)
        else -> editTeamCategory(navController, Json.decodeFromString<TeamCategory>(argument!!))
    }
}

@Composable
@Preview
fun teamCategoryEditor(navController: NavController) {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val state = viewModel.uiState.collectAsState()

    viewCommon(state.value, navController, "Team Categories", { createFloatingAction(navController, editor.addRoute()) }) {
            paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            val values = state.value.data!!
            items(
                items = values.sortedBy { it.name.uppercase().trim() },
                key = { teamCategory -> teamCategory.id }) { teamCategory ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        Row(
                            modifier = Modifier.weight(2F), content = {
                                Spacer(modifier = Modifier.size(16.dp))
                                ViewText(teamCategory.name)
                                Spacer(modifier = Modifier.size(16.dp))
                                ViewText(Day.entries[teamCategory.matchDay].display)
                            })

                        itemButtons(
                            editClick = {
                                navController.navigate(
                                    Editors.TEAM_CATEGORIES.name +
                                            "/${Json.encodeToString(teamCategory)}"
                                )
                            },
                            deleteClick = {
                                coroutineScope.launch {
                                    viewModel.delete(teamCategory)
                                }
                            })
                    })

            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addTeamCategory(navController: NavController) {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var matchDay by remember {mutableStateOf(5)}

    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {createTopBar(navController, "Add TeamCategory", "Return to teamCategories")},
           content = { paddingValues ->
            Row(
                modifier = Modifier.padding(paddingValues), content = {
                    ViewTextField(value = name, label = "Name:") { name = it }
                    Text("Match Day:")
                    DropdownList(itemList = Day.entries.map { it.display }, selectedIndex = matchDay) { matchDay = it }
                })
        }, bottomBar = {
            Button(onClick = {
                    coroutineScope.launch {
                        viewModel.insert(TeamCategory(name = name.trim(), matchDay = matchDay))
                        navController.popBackStack()
                    }
                },
                enabled = !name.isEmpty()) { ViewText(stringResource(Res.string.ok)) }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editTeamCategory(navController: NavController, editCategory: TeamCategory) {
    val viewModel: TeamCategoryViewModel = koinInject<TeamCategoryViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var teamCategory by remember { mutableStateOf(editCategory) }
    var name by remember { mutableStateOf(teamCategory.name) }
    var matchDay by remember {mutableStateOf(teamCategory.matchDay)}

    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            createTopBar(navController, "Edit TeamCategory", "Return to teamCategories")
        }, content = { paddingValues ->
            Row(
                modifier = Modifier.padding(paddingValues), content = {
                    ViewTextField(value = name, label = "Name:") { name = it }
                    Text("Match Day:")
                    DropdownList(itemList = Day.entries.map { it.display }, selectedIndex = matchDay) { matchDay = it }
                })
        }, bottomBar = {
            Button(onClick = {
                coroutineScope.launch {
                    viewModel.update(TeamCategory(teamCategory.id, name.trim(), matchDay))
                    navController.popBackStack()
                }
            },
                enabled = !name.isEmpty()) { ViewText(stringResource(Res.string.ok)) }
        })
    }
}
