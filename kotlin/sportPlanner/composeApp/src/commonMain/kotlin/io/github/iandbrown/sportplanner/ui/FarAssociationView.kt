package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.FarAssociation
import io.github.iandbrown.sportplanner.database.FarAssociationDao
import io.github.iandbrown.sportplanner.database.FarAssociationView
import io.github.iandbrown.sportplanner.database.FarAssociationViewDao
import io.github.iandbrown.sportplanner.di.inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

class FarAssociationViewModel : BaseConfigCRUDViewModel<FarAssociationDao, FarAssociation>(inject<FarAssociationDao>().value)

class FarAssociationListViewModel : BaseReadViewModel<FarAssociationViewDao, FarAssociationView> (inject<FarAssociationViewDao>().value) {
    fun delete(entity : FarAssociationView) {
        viewModelScope.launch {
            dao.delete(entity.homeAssociationId, entity.awayAssociationId)
        }
    }
}


private val editor = Editors.FAR_ASSOCIATIONS

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
private fun FarAssociationEditor(viewModel: FarAssociationListViewModel = koinInject<FarAssociationListViewModel>()) {
    val state = viewModel.uiState.collectAsState()

    ViewCommon(
        editor.displayName,
        bottomBar = {
            BottomBarWithButtonN("+") {editor.addRoute()}
        }){ paddingValues ->
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            item { ViewText("Home Association") }
            item { ViewText("Away Association") }
            item {}
            item {}
            for (entity in state.value) {
                item { ViewText(entity.homeAssociationName) }
                item { ViewText(entity.awayAssociationName) }
                item { EditButton {editor.editRoute(entity) } }
                item { DeleteButton { viewModel.delete(entity) } }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun EditFarAssociation(farAssociation: FarAssociationView?,
                               viewModel: FarAssociationViewModel = koinInject(),
                               associationModel : AssociationViewModel = koinInject()) {
    val state = viewModel.uiState.collectAsState()
    val associationState = associationModel.uiState.collectAsState()
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
        confirmAction = {save(farAssociation, viewModel, homeAssociation, awayAssociation)}) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                val sortedAssociations = associationState.value.sortedBy { it.name }
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
                        val invalidAwayAssociations = state.value
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
        println("Update ${farAssociation.awayAssociationId} to $awayAssociation")
    }
    viewModel.insert(FarAssociation(homeAssociation, awayAssociation))
}
