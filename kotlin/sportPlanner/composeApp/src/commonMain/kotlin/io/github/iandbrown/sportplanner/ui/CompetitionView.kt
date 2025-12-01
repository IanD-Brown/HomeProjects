package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.softartdev.theme.material.PreferableMaterialTheme
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.CompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.softartdev.theme_prefs.generated.resources.Res
import io.github.softartdev.theme_prefs.generated.resources.ok
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
                            SpacedViewText(CompetitionTypes.entries[competition.type.toInt()].display)
                        })
                    ItemButtons(
                        { navController.navigate(editor.editRoute(competition)) },
                        { coroutineScope.launch { viewModel.delete(competition) } })
                })
            }
        })
    })
}

@Composable
private fun EditCompetition(navController: NavController, editCategory: Competition?) {
    val viewModel: CompetitionViewModel = koinInject<CompetitionViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(editCategory?.name ?: "") }
    var type by remember {mutableStateOf(editCategory?.type ?: 0.toShort())}
    val title = if (editCategory == null) "Add Competition" else "Edit Competition"

    ViewCommon(SimpleState(), navController, title, {}, "Return to Competitions", {
        Button(onClick = {
            coroutineScope.launch {
                viewModel.insert(Competition(editCategory?.id ?: 0.toShort(), name.trim(), type))
                navController.popBackStack()
            }
        },
            enabled = !name.isEmpty()) { ViewText(stringResource(Res.string.ok)) }

    }) { paddingValues ->
        PreferableMaterialTheme {
            FlowRow(modifier = Modifier.padding(paddingValues).fillMaxWidth(), maxItemsInEachRow = 2) {
                ViewText("Name", modifier = Modifier.weight(1f))
                ViewText("Type", modifier = Modifier.weight(1f))
                ViewTextField(value = name, onValueChange = { name = it }, modifier = Modifier.weight(1f))
                DropdownList(
                    itemList = CompetitionTypes.entries.map { it.display },
                    selectedIndex = type.toInt(),
                    modifier = Modifier.weight(1f)
                ) { type = it.toShort() }
            }
        }
    }
}
