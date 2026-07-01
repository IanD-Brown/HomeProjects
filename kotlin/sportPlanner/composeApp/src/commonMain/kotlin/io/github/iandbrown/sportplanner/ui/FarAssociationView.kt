package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.AssociationDao
import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.FarAssociation
import io.github.iandbrown.sportplanner.database.FarAssociationDao
import io.github.iandbrown.sportplanner.database.FarAssociationView
import io.github.iandbrown.sportplanner.database.FarAssociationViewDao
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.java.KoinJavaComponent.inject

class FarAssociationViewModel(dao: FarAssociationDao) : BaseConfigCRUDViewModel<FarAssociationDao, FarAssociation>(dao)

class FarAssociationListViewModel(dao: FarAssociationViewDao) : BaseConfigReadViewModel<FarAssociationViewDao, FarAssociationView> (dao) {
    fun delete(entity : FarAssociationView) {
        viewModelScope.launch {
            dao.delete(entity.homeAssociationId, entity.awayAssociationId)
        }
    }
}

private val editor = Editors.FAR_ASSOCIATIONS
private const val HOME = "Home"
private const val AWAY = "Away"

@Composable
fun NavigateFarAssociation(argument: String?) {
    when (argument) {
        "View" -> FarAssociationEditor()
        "Add" -> EditFarAssociation(null)
        else -> EditFarAssociation(Json.decodeFromString<FarAssociationView>(argument!!))
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun FarAssociationEditor(viewModel: FarAssociationListViewModel = koinViewModel(),
                                 farAssociationViewModel: FarAssociationViewModel= koinViewModel()) {
    val state = viewModel.getState().collectAsState()
    val farAssociationState = farAssociationViewModel.getState().collectAsState()
    val coroutineScope = rememberCoroutineScope()

    ViewCommon(
        editor.displayName,
        bottomBar = {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope, "distantAwayGames") {
                    toDataFrame(state.values()).writeJson(it)
                },
                importJsonButtonSettings(farAssociationViewModel) {
                    toFarAssociation(it)
                },
                addButtonSettings { it.navigate(editor.addRoute()) }
            )
        },
        states = persistentListOf(state.value, farAssociationState.value)){ paddingValues ->
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Home Association", "Away Association"))
            item(span = { GridItemSpan(2) }) {}
            for (entity in state.values()) {
                viewTextItems(listOf(entity.homeAssociationName, entity.awayAssociationName))
                editButton {editor.editRoute(entity) }
                deleteButton { viewModel.delete(entity) }
            }
        }
    }
}

internal fun toDataFrame(records: List<FarAssociationView>): DataFrame<FarAssociationView> =
    records.toDataFrame {
        HOME from { it.homeAssociationName }
        AWAY from { it.awayAssociationName }
    }

internal suspend fun toFarAssociation(row: DataRow<Any?>,
                                      associationDao: AssociationDao = inject<AssociationDao>(AssociationDao::class.java).value): FarAssociation =
    FarAssociation(associationDao.getByName(row[HOME] as String)!!,
        associationDao.getByName(row[AWAY] as String)!!)


@Suppress("ParamsComparedByRef")
@Composable
private fun EditFarAssociation(farAssociation: FarAssociationView?,
                               viewModel: FarAssociationViewModel = koinInject(),
                               associationModel : AssociationViewModel = koinInject()) {
    val state = viewModel.getState().collectAsState()
    val associationState = associationModel.getState().collectAsState()
    var homeAssociation by remember { mutableStateOf(farAssociation?.homeAssociationId ?: 0) }
    var awayAssociation by remember { mutableStateOf(farAssociation?.awayAssociationId ?: 0) }
    val title = if (farAssociation == null) "Add Distant Away Game" else "Edit Distant Away Game"

    ViewCommon(
        title,
        description = "Return ${editor.displayName}",
        bottomBar = {
            BottomBarWithButton(enabled = homeAssociation > 0 && awayAssociation > 0) {
                save(farAssociation, viewModel, homeAssociation, awayAssociation)
                appNavController.popBackStack()
            }
        },
        confirm = {homeAssociation > 0 && awayAssociation > 0 && (farAssociation == null || awayAssociation != farAssociation.awayAssociationId)},
        confirmAction = {save(farAssociation, viewModel, homeAssociation, awayAssociation)},
        states = persistentListOf(state.value, associationState.value)) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                val sortedAssociations = associationState.values().sortedBy { it.name }
                Row {
                    ReadonlyViewText("Home Association")
                    if (farAssociation != null) {
                        SpacedViewText(farAssociation.homeAssociationName)
                    } else {
                        val idList = sortedAssociations.map {it.id}
                        DropdownList(sortedAssociations.map {it.name}.toImmutableList(), idList.indexOf(homeAssociation)) {
                            homeAssociation = sortedAssociations[it].id
                        }
                    }
                }
                Row {
                    ReadonlyViewText("Away Association")
                    if (homeAssociation > 0) {
                        val invalidAwayAssociations = state.values()
                            .filter { it.homeAssociation == homeAssociation }
                            .filter { it.awayAssociation != awayAssociation }
                            .map { it.awayAssociation }
                            .toSet()
                        val possibleAwayAssociations = sortedAssociations
                            .filter { it.id != homeAssociation }
                            .filter { !invalidAwayAssociations.contains(it.id) }
                        val idList = possibleAwayAssociations.map { it.id }
                        val nameList = possibleAwayAssociations.map { it.name }
                        DropdownList(nameList.toImmutableList(), idList.indexOf(awayAssociation)) {
                            awayAssociation = idList[it]
                        }
                    } else {
                        ViewText("")
                    }
                }
            }
        }
}

private fun save(farAssociation: FarAssociationView?, viewModel: FarAssociationViewModel, homeAssociation: AssociationId, awayAssociation: AssociationId) {
    if (farAssociation != null) {
        if (farAssociation.awayAssociationId == awayAssociation) {
            return
        }
        viewModel.delete(FarAssociation(farAssociation.homeAssociationId, farAssociation.awayAssociationId))
    }
    viewModel.insert(FarAssociation(homeAssociation, awayAssociation))
}
