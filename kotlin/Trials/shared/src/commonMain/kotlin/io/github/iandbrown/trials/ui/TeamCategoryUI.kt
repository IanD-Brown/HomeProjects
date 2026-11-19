package io.github.iandbrown.trials.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.iandbrown.trials.database.Gender
import io.github.iandbrown.trials.database.TeamCategory
import io.github.iandbrown.trials.database.TeamCategoryDao
import io.github.iandbrown.trials.database.YearGroup
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.koin.compose.viewmodel.koinViewModel

internal class TeamCategoryViewModel(dao: TeamCategoryDao) :
    CRUDViewModel<TeamCategoryDao, TeamCategory>(dao = dao)

@Composable
internal fun TeamCategoryView() {
    val viewModel: TeamCategoryViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()

    var editingCategory by remember { mutableStateOf<TeamCategory?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingCategory != null) {
        TeamCategoryEditor(
            category = editingCategory,
            onSave = { category ->
                if (editingCategory == null) {
                    viewModel.insert(category)
                } else {
                    viewModel.update(category)
                }
                editingCategory = null
                isAdding = false
            },
            onCancel = {
                editingCategory = null
                isAdding = false
            }
        )
    } else {
        ViewCommon(
            title = "Team Categories",
            states = persistentListOf(state),
            bottomBar = {
                BottomBarWithButtons(
                    addButtonSettings(onClick = { isAdding = true })
                )
            }
        ) { padding ->
            TrailingIconLazyVerticalGrid(padding, 3, 2) {
                viewTextItems(listOf("Name", "Year Group", "Gender", "", ""))
                state.values().forEach { category ->
                    viewTextItems(listOf(category.name, category.yearGroup.displayName, category.gender.displayName))
                    editButton { editingCategory = category }
                    deleteButton { viewModel.delete(category) }
                }
            }
        }
    }
}

@Composable
internal fun TeamCategoryEditor(
    category: TeamCategory?,
    onSave: (TeamCategory) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedYearGroup by remember { mutableStateOf(category?.yearGroup ?: YearGroup.FIVE) }
    var selectedGender by remember { mutableStateOf(category?.gender ?: Gender.MALE) }
    val isEditing = category != null

    val yearGroupOptions = YearGroup.entries.map { it.displayName }.toImmutableList()
    val selectedYearGroupIndex = YearGroup.entries.indexOf(selectedYearGroup)

    val genderOptions = Gender.entries.map { it.displayName }.toImmutableList()
    val selectedGenderIndex = Gender.entries.indexOf(selectedGender)

    val isValid = name.isNotBlank()

    ViewCommon(
        title = if (isEditing) "Edit Team Category" else "Add Team Category",
        description = "Cancel editing",
        bottomBar = {
            BottomBarWithButton(enabled = isValid) {
                onSave(TeamCategory(name = name, yearGroup = selectedYearGroup, gender = selectedGender))
            }
        },
        confirm = { name.isNotBlank() },
        confirmAction = { onSave(TeamCategory(name = name, yearGroup = selectedYearGroup, gender = selectedGender)) }
    ) { padding ->
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            gridEntry(name, "Name", onValueChange = { if (!isEditing) name = it })

            item { ViewText("Year Group") }
            item {
                DropdownList(
                    itemList = yearGroupOptions,
                    selectedIndex = selectedYearGroupIndex,
                    onItemClick = { idx ->
                        selectedYearGroup = YearGroup.entries[idx]
                    }
                )
            }

            item { ViewText("Gender") }
            item {
                DropdownList(
                    itemList = genderOptions,
                    selectedIndex = selectedGenderIndex,
                    onItemClick = { idx ->
                        selectedGender = Gender.entries[idx]
                    }
                )
            }
        }
    }
}

private const val NAME = "Name"
private const val YEAR_GROUP = "YearGroup"
private const val GENDER = "Gender"

internal fun toDataFrame(competitions: List<TeamCategory>): DataFrame<TeamCategory> =
    competitions.toDataFrame {
        NAME from { it.name }
        YEAR_GROUP from { it.yearGroup }
        GENDER from {it.gender}
    }
