package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.Association
import io.github.iandbrown.sportplanner.database.AssociationDao
import io.github.iandbrown.sportplanner.database.AssociationId
import io.github.iandbrown.sportplanner.database.FarAssociation
import io.github.iandbrown.sportplanner.database.FarAssociationDao
import io.github.iandbrown.sportplanner.database.FarAssociationView
import io.github.iandbrown.sportplanner.database.FarAssociationViewDao
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.viewmodel.koinViewModel
import org.koin.java.KoinJavaComponent.inject

class FarAssociationViewModel(dao: FarAssociationDao) :
    BaseCRUDViewModel<FarAssociationDao, FarAssociation>(dao, { it.get() }) {

    fun save(
        farAssociation: FarAssociationView?,
        homeAssociation: AssociationId,
        awayAssociation: AssociationId
    ) {
        if (farAssociation != null) {
            if (farAssociation.awayAssociationId == awayAssociation) {
                return
            }
            delete(FarAssociation(farAssociation.homeAssociationId, farAssociation.awayAssociationId))
        }
        insert(FarAssociation(homeAssociation, awayAssociation))
    }
}

class FarAssociationListViewModel(dao: FarAssociationViewDao) :
    BaseReadViewModel<FarAssociationViewDao, FarAssociationView>(dao, { it.get() }) {
    fun delete(entity: FarAssociationView) {
        viewModelScope.launch {
            dao.delete(entity.homeAssociationId, entity.awayAssociationId)
        }
    }
}

private const val HOME = "Home"
private const val AWAY = "Away"

@Composable
fun FarAssociationListScreen() {
    val viewModel: FarAssociationListViewModel = koinViewModel()
    val farAssociationViewModel: FarAssociationViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()
    val farAssociationState by farAssociationViewModel.getState().collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(viewModel) {
        viewModel.readAll()
        onPauseOrDispose { }
    }

    FarAssociationListContent(
        state = state,
        farAssociationState = farAssociationState,
        onExport = {
            exportButtonSettings(coroutineScope, "distantAwayGames") {
                toDataFrame(state.values()).writeJson(it)
            }
        },
        onImport = {
            importJsonButtonSettings(farAssociationViewModel) {
                toFarAssociation(it)
            }
        },
        onAdd = { appNavigator.navigate(Route.FarAssociationEdit(null)) },
        onEdit = { appNavigator.navigate(Route.FarAssociationEdit(it)) },
        onDelete = { viewModel.delete(it) }
    )
}

@Composable
private fun FarAssociationListContent(
    state: ViewModelState<FarAssociationView>,
    farAssociationState: ViewModelState<FarAssociation>,
    onExport: () -> ButtonSettings,
    onImport: () -> ButtonSettings,
    onAdd: () -> Unit,
    onEdit: (FarAssociationView) -> Unit,
    onDelete: (FarAssociationView) -> Unit
) {
    ViewCommon(
        "Distant Away Games",
        bottomBar = {
            BottomBarWithButtons(
                onExport(),
                onImport(),
                addButtonSettings { onAdd() }
            )
        },
        states = persistentListOf(state, farAssociationState)
    ) { paddingValues ->
        LazyVerticalGrid(columns = TrailingIconGridCells(2, 2), modifier = Modifier.padding(paddingValues)) {
            viewTextItems(listOf("Home Association", "Away Association"))
            item(span = { GridItemSpan(2) }) {}
            for (entity in state.values()) {
                viewTextItems(listOf(entity.homeAssociationName, entity.awayAssociationName))
                editButton { onEdit(entity) }
                deleteButton { onDelete(entity) }
            }
        }
    }
}

internal fun toDataFrame(records: List<FarAssociationView>): DataFrame<FarAssociationView> =
    records.toDataFrame {
        HOME from { it.homeAssociationName }
        AWAY from { it.awayAssociationName }
    }

internal suspend fun toFarAssociation(
    row: DataRow<Any?>,
    associationDao: AssociationDao = inject<AssociationDao>(AssociationDao::class.java).value
): FarAssociation =
    FarAssociation(
        associationDao.getByName(row[HOME] as String)!!,
        associationDao.getByName(row[AWAY] as String)!!
    )


@Composable
fun FarAssociationEditScreen(farAssociation: FarAssociationView?) {
    val viewModel: FarAssociationViewModel = koinViewModel()
    val associationModel: AssociationViewModel = koinViewModel()
    val state by viewModel.getState().collectAsStateWithLifecycle()
    val associationState by associationModel.getState().collectAsStateWithLifecycle()

    FarAssociationEditContent(
        farAssociation = farAssociation,
        state = state,
        associationState = associationState,
        onSave = { home, away ->
            viewModel.save(farAssociation, home, away)
            appNavigator.goBack()
        },
        onConfirmSave = { home, away ->
            viewModel.save(farAssociation, home, away)
        }
    )
}

@Composable
private fun FarAssociationEditContent(
    farAssociation: FarAssociationView?,
    state: ViewModelState<FarAssociation>,
    associationState: ViewModelState<Association>,
    onSave: (AssociationId, AssociationId) -> Unit,
    onConfirmSave: (AssociationId, AssociationId) -> Unit
) {
    var homeAssociation by remember { mutableStateOf(farAssociation?.homeAssociationId ?: 0) }
    var awayAssociation by remember { mutableStateOf(farAssociation?.awayAssociationId ?: 0) }

    ViewCommon(
        if (farAssociation == null) "Add Distant Away Game" else "Edit Distant Away Game",
        description = "Return Distant Away Games",
        bottomBar = {
            BottomBarWithButton(enabled = homeAssociation > 0 && awayAssociation > 0) {
                onSave(homeAssociation, awayAssociation)
            }
        },
        confirm = {
            homeAssociation > 0 && awayAssociation > 0 && (farAssociation == null || awayAssociation != farAssociation.awayAssociationId)
        },
        confirmAction = { onConfirmSave(homeAssociation, awayAssociation) },
        states = persistentListOf(state, associationState)
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            val sortedAssociations = associationState.values().sortedBy { it.name }
            Row {
                ReadonlyViewText("Home Association")
                if (farAssociation != null) {
                    SpacedViewText(farAssociation.homeAssociationName)
                } else {
                    val idList = sortedAssociations.map { it.id }
                    DropdownList(
                        sortedAssociations.map { it.name }.toImmutableList(),
                        idList.indexOf(homeAssociation)
                    ) {
                        homeAssociation = sortedAssociations[it].id
                    }
                }
            }
            Row {
                ReadonlyViewText("Away Association")
                if (homeAssociation > 0) {
                    // Logic to filter possible away associations
                    val associationsInState = state.values()
                    val invalidAwayAssociations = associationsInState
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
