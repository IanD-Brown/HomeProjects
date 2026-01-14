package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
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
        "Add" -> EditCompetition(navController, null)
        else -> EditCompetition(navController, Json.decodeFromString<Competition>(argument!!))
    }
}

enum class CompetitionTypes(val display : String) {
    LEAGUE("League"),
    KNOCK_OUT_CUP("Knockout cup")
}

@Composable
private fun CompetitionView(navController: NavController) {
    val viewModel: CompetitionViewModel = koinInject<CompetitionViewModel>()
    val state = viewModel.uiState.collectAsState()

    ViewCommon(state.value, navController, "Competitions",  { CreateFloatingAction(navController, editor.addRoute()) }, content = { paddingValues ->
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            item { ViewText("Name") }
            item { ViewText("Type") }
            item {}
            item {}
            for (competition in state.value.data?.sortedBy { it.name.uppercase().trim() }!!) {
                item { ViewText(competition.name) }
                item { ViewText(CompetitionTypes.entries[competition.type.toInt()].display) }
                item { EditButton {navController.navigate(editor.editRoute(competition)) } }
                item { DeleteButton { viewModel.delete(competition) } }
            }
        }
    })
}

@Composable
private fun EditCompetition(navController: NavController, editCompetition: Competition?) {
    val viewModel: CompetitionViewModel = koinInject<CompetitionViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(editCompetition?.name ?: "") }
    var type by remember {mutableStateOf(editCompetition?.type ?: 0.toShort())}
    val title = if (editCompetition == null) "Add Competition" else "Edit Competition"

    ViewCommon(SimpleState(), navController, title, {}, "Return to Competitions",
        {
            Button(onClick = {
                save(coroutineScope, viewModel, editCompetition, name, type)
                navController.popBackStack()
            },
            enabled = !name.isEmpty()) { ViewText(stringResource(Res.string.ok)) }
        },
        {
            (name.isNotEmpty() && (editCompetition == null || name != editCompetition.name)) || (editCompetition != null && type != editCompetition.type)
        },
        {save(coroutineScope, viewModel, editCompetition, name, type)}) { paddingValues ->
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(paddingValues)) {
            item { ReadonlyViewText(value = "Name") }
            item { ReadonlyViewText(value = "Type") }
            item { ViewTextField(value = name, onValueChange = { name = it }) }
            item { DropdownList(
                itemList = CompetitionTypes.entries.map { it.display },
                selectedIndex = type.toInt(),
            ) { type = it.toShort() } }
        }
    }
}

private fun save(coroutineScope: CoroutineScope, viewModel: CompetitionViewModel, editCompetition: Competition?, name: String, type: Short) {
    coroutineScope.launch {
        viewModel.insert(Competition(editCompetition?.id ?: 0.toShort(), name.trim(), type))
    }
}
