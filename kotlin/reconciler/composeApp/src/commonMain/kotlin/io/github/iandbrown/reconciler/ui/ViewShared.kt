package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults.IconSize
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.iandbrown.reconciler.logic.DayDate
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import java.util.Locale

val fontSize = 16.sp
const val OK = "OK"

var appFileKitDialogSettings : FileKitDialogSettings? = null
lateinit var appNavController : NavController

internal enum class EditorState {CLEAN, VALID, DIRTY}

@Composable
fun ViewCommon(
    title: String,
    description: String = "Return to home screen",
    bottomBar: @Composable () -> Unit = {},
    confirm: () -> Boolean = { false },
    confirmAction: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { CreateTopBar(title, description, confirm, confirmAction) },
        bottomBar = bottomBar,
        content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTopBar(
    title: String,
    description: String,
    confirm: () -> Boolean,
    confirmAction: () -> Unit
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val navState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navState,
        isBackEnabled = confirm(),
        onBackCompleted = {}
    )

    TopAppBar(title = { Text(title) }, navigationIcon = {
        IconButton(onClick = {
            if (confirm()) {
                isDialogOpen = true
            } else {
                appNavController.navigateUp()
            }
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = description)
        }
    })
    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = {},
            dismissButton = {
                Button(onClick = { isDialogOpen = closeConfirmDialog(appNavController) {} }) {
                    Text("Discard changes")
                }
            },
            confirmButton = {
                Button(onClick = { isDialogOpen = closeConfirmDialog(appNavController, confirmAction) }) {
                    Text("Save")
                }
            },
            title = { Text("Data has been changed") },
        )
    }
}

private fun closeConfirmDialog(navController: NavController, confirmAction : () -> Unit) : Boolean {
    confirmAction()
    navController.navigateUp()
    return false
}

@Composable
fun ViewText(value : String, modifier: Modifier = Modifier) {
    Text(
        text = value,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun ReadonlyViewText(value : String, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        readOnly = true,
        onValueChange = {},
        singleLine = true,
        colors = textFieldColors(),
        textStyle = textStyle(),
        modifier = modifier
    )
}

@Composable
fun ViewTextField(
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    label: String? = null,
    onValueChange: (String) -> Unit
) {
    when (label) {
        null -> TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            singleLine = true,
            trailingIcon = trailingIcon,
            colors = textFieldColors(),
            textStyle = textStyle()
        )
        else -> TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            singleLine = true,
            label = { ViewText(label) },
            trailingIcon = trailingIcon,
            colors = textFieldColors(),
            textStyle = textStyle()
        )
    }
}

@Composable
fun textStyle(): TextStyle = TextStyle.Default.copy(fontSize = fontSize, color = MaterialTheme.colorScheme.onSurface)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun textFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Composable
fun DropdownList(
    itemList: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    isLocked: () -> Boolean = { false },
    label: String? = null,
    onItemClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(if (itemList.isNotEmpty()) itemList[selectedIndex] else "") }

    if (isLocked()) {
        ViewText(selectedText, modifier)
    } else {
        // Up Icon when expanded and down icon when collapsed
        val icon = if (expanded)
            Icons.Filled.KeyboardArrowUp
        else
            Icons.Filled.KeyboardArrowDown
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = modifier
                .fillMaxWidth().padding(0.dp)
                .clickable {
                    if (!isLocked()) {
                        expanded = !expanded
                    }
                },
        ) {
            ViewTextField(
                value = selectedText,
                label = label,
                trailingIcon = {
                    if (!isLocked()) {
                        Icon(
                            icon, "contentDescription",
                            Modifier.clickable { expanded = !expanded })
                    }
                }
            ) {}
            if (!isLocked() && expanded) {
                DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
                    itemList.forEach { label ->
                        DropdownMenuItem(
                            text = { ViewText(label) },
                            onClick = {
                                selectedText = label
                                onItemClick(itemList.indexOf(label))
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerView(current: Int, modifier : Modifier, isSelectable : (Long) -> Boolean, onItemClick: (Int) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    ViewTextField(
        value = DayDate(current).toString(),
        modifier = modifier,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = !showDatePicker }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date"
                )
            }
        }
    ) {}

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DayDate(current).asUtcMs(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = isSelectable(utcTimeMillis)
            })
        val onDismissRequest = { showDatePicker = false }

        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    enabled = (datePickerState.selectedDateMillis ?: 0) > 0L,
                    onClick = {
                        onItemClick(DayDate(datePickerState.selectedDateMillis!!).value())
                        onDismissRequest()
                    }) { ViewText("OK") }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) { ViewText("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun EditButton(navigateFun : (NavController) -> Unit) {
    Icon(Icons.Default.Edit, "edit", Modifier.clickable(onClick = { navigateFun(appNavController)}), Color.Green)
}

@Composable
fun DeleteButton(disabled: Boolean = false, onClick : () -> Unit) =
    if (disabled) {
        Icon(Icons.Default.Delete, "delete", tint = Color(0x99990000))
    } else {
        Icon(Icons.Default.Delete, "delete", Modifier.clickable(onClick = onClick), Color.Red)
    }

class WeightedIconGridCells(val iconCount: Int, vararg val weights: Int) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val columnCount = iconCount + weights.size
        val totalSpacing = spacing * (columnCount - 1)
        val iconSize = (IconSize + 16.dp).roundToPx()
        val remainingSpace = availableSize - totalSpacing - iconSize * iconCount
        val totalWeight = weights.sum()
        return mutableListOf<Int>().apply {
            repeat(weights.size) {
                add((remainingSpace / totalWeight) * weights[it])
            }
        }.apply {
            repeat(iconCount) {
                add(iconSize)
            }
        }
    }
}

@Composable
fun OutlinedTextButton(value: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(enabled = enabled,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(6.dp),
        onClick = onClick)
    { ViewText(value) }
}

data class ButtonSettings(val value : String = OK, val enabled: Boolean = true, val navigateFun : (NavController) -> Unit)

@Composable
fun BottomBarWithButton(value : String = OK, enabled: Boolean = true, navigateFun : (NavController) -> Unit) =
    BottomBarWithButtons(ButtonSettings(value, enabled, navigateFun))

@Composable
fun BottomBarWithButtons(vararg buttonSettings: ButtonSettings) {
    Row {
        ReadonlyViewText("", Modifier.weight(4f))
        for (buttonSetting in buttonSettings) {
            OutlinedTextButton(buttonSetting.value, Modifier.weight(1f), buttonSetting.enabled) {
                buttonSetting.navigateFun(appNavController)
            }
        }
    }
}

fun LazyGridScope.gridEntry(title : String, value : String, onValueChange: (String) -> Unit) {
    item { ViewText(title) }
    item { ViewTextField(value = value, onValueChange = onValueChange) }
}

fun LazyGridScope.gridEntry(title : String, value : Boolean, onValueChange: (Boolean) -> Unit) {
    item { ViewText(title) }
    item { Checkbox(value, onValueChange) }
}

fun LazyGridScope.formatedNumber(format : String, value : Double?) {
    item { ViewText(String.format(Locale.UK, format, value ?: 0.0)) }
}
