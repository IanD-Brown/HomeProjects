package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

class CompetitionViewModel : BaseViewModel<CompetitionDao, Competition>() {
    override fun getDao(db: AppDatabase): CompetitionDao = db.getCompetitionDao()
}

class SeasonCompetitionViewModel: BaseViewModel<SeasonCompetitionDao, SeasonCompetition>() {
    override fun getDao(db: AppDatabase): SeasonCompetitionDao = db.getSeasonCompetitionDao()
}

private val editor = Editors.COMPETITIONS

@Composable
fun NavigateCompetitions(navController: NavController, argument: String?) {
    when (argument) {
        "View" -> CompetitionView(navController)
        "Add" -> AddCompetition(navController)
        else -> EditCompetition(navController, Json.decodeFromString<Competition>(argument!!))
    }
}

private enum class Types(val display : String) {
    LEAGUE("League"),
    KNOCK_OUT_CUP("Knockout cup")
}

@Composable
@Preview
private fun CompetitionView(navController: NavController) {
    val viewModel: CompetitionViewModel = koinInject<CompetitionViewModel>()
    val state = viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(state.value, navController, "Competitions",  { CreateFloatingAction(navController, editor.addRoute()) }, content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            val values = state.value.data!!
            items(
                items = values.sortedBy { it.name.uppercase().trim() },
                key = { competition -> competition.id }) { competition ->
                Row(modifier = Modifier.fillMaxWidth(), content = {
                    Row(
                        modifier = Modifier.weight(2F),
                        content = {
                            SpacedViewText(competition.name)
                            SpacedViewText(Types.entries[competition.type.toInt()].display)
                        })
                    ItemButtons(
                        { navController.navigate(editor.editRoute(competition)) },
                        { coroutineScope.launch { viewModel.delete(competition) } })
                })
            }
        })
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCompetition(navController: NavController) {
    val viewModel: CompetitionViewModel = koinInject<CompetitionViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var type by remember {mutableIntStateOf(0)}

    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {CreateTopBar(navController, "Add Competition", "Return to competitions")},
            content = { paddingValues ->
                Row(
                    modifier = Modifier.padding(paddingValues), content = {
                        ViewTextField(value = name, label = "Name:") { name = it }
                        Text("Type:")
                        DropdownList(itemList = Types.entries.map { it.display }, selectedIndex = type) { type = it }
                    })
            }, bottomBar = {
                Button(onClick = {
                    coroutineScope.launch {
                        viewModel.insert(Competition(name = name.trim(), type = type.toShort()))
                        navController.popBackStack()
                    }
                },
                    enabled = !name.isEmpty()) { ViewText(stringResource(Res.string.ok)) }
            })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCompetition(navController: NavController, editCategory: Competition) {
    val viewModel: CompetitionViewModel = koinInject<CompetitionViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var teamCategory by remember { mutableStateOf(editCategory) }
    var name by remember { mutableStateOf(teamCategory.name) }
    var type by remember {mutableStateOf(teamCategory.type)}

    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            CreateTopBar(navController, "Edit Competition", "Return to teamCategories")
        }, content = { paddingValues ->
            Row(
                modifier = Modifier.padding(paddingValues), content = {
                    ViewTextField(value = name, label = "Name:") { name = it }
                    Text("Type:")
                    DropdownList(itemList = Types.entries.map { it.display }, selectedIndex = type.toInt()) { type = it.toShort() }
                })
        }, bottomBar = {
            Button(onClick = {
                coroutineScope.launch {
                    viewModel.update(Competition(teamCategory.id, name.trim(), type))
                    navController.popBackStack()
                }
            },
                enabled = !name.isEmpty()) { ViewText(stringResource(Res.string.ok)) }

        })
    }
}
