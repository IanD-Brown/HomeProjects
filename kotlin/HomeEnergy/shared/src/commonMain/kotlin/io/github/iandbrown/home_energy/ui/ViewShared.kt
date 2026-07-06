package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults.IconSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val fontSize = 16.sp
const val OK = "OK"

internal enum class EditorState {CLEAN, VALID, DIRTY}

@Composable
fun ViewCommon(
    title: String,
    states: ImmutableList<ViewModelState<*>> = persistentListOf(),
    description: String = "Return to home screen",
    bottomBar: @Composable () -> Unit = {},
    confirm: () -> Boolean = { false },
    confirmAction: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val errors = states.filterIsInstance<ViewModelState.Error>()
    if (errors.isNotEmpty()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { CreateTopBar(title, description, confirm, confirmAction) },
        ) {paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                for (error in errors) {
                    ReadonlyViewText(error.message)
                }
            }
        }
    } else if (states.filter { it !is ViewModelState.Error }.any { it !is ViewModelState.Success }) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { CreateTopBar(title, description, confirm, confirmAction) },
            bottomBar = bottomBar,
            content = content
        )
    }
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

    fun closeConfirmDialog(confirmAction : () -> Unit) : Boolean {
        confirmAction()
        return false
    }


    TopAppBar(title = { Text(title) }, navigationIcon = {
        IconButton(onClick = {
            if (confirm()) {
                isDialogOpen = true
            }
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = description)
        }
    })
    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = {},
            dismissButton = {
                Button(onClick = { isDialogOpen = closeConfirmDialog {} }) {
                    Text("Discard changes")
                }
            },
            confirmButton = {
                Button(onClick = {isDialogOpen = closeConfirmDialog(confirmAction) }) {
                    Text("Save")
                }
            },
            title = { Text("Data has been changed") },
        )
    }
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
            label = {ReadonlyViewText(label) },
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
fun OutlinedTextButton(value: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(enabled = enabled,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(6.dp),
        onClick = onClick)
    { Text(value, fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

class TrailingIconGridCells(val dataColumnCount: Int, val trailingIconCount: Int) :
    GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        // Define the total available width after accounting for spacing
        val columnCount = dataColumnCount + trailingIconCount
        val totalSpacing = spacing * (columnCount - 1)
        val iconSize = (IconSize + 16.dp).roundToPx()
        val usableWidth = availableSize - (totalSpacing + trailingIconCount * iconSize)
        return mutableListOf<Int>().apply {
            repeat(dataColumnCount) {
                add(usableWidth / dataColumnCount)
            }
        }.apply {
            repeat(trailingIconCount) {
                add(iconSize)
            }
        }
    }
}

internal data class ButtonSettings(val value : String = OK, val enabled: Boolean = true, val imageVector: ImageVector? = null, val onClick : () -> Unit)

internal fun addButtonSettings(onClick : () -> Unit) : ButtonSettings =
    ButtonSettings(imageVector = Icons.Default.Add, onClick = onClick)

@Composable
fun BottomBarWithButton(value : String = OK, enabled: Boolean = true, onClick : () -> Unit) =
    BottomBarWithButtons(ButtonSettings(value, enabled, onClick = onClick))

@Composable
internal fun BottomBarWithButtons(vararg buttonSettings: ButtonSettings) {
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        for (buttonSetting in buttonSettings) {
            if (buttonSetting.imageVector == null) {
                OutlinedTextButton(buttonSetting.value, Modifier.wrapContentSize(), buttonSetting.enabled) {
                    buttonSetting.onClick()
                }
            } else {
                IconButton(onClick = { buttonSetting.onClick() }) {
                    Icon(buttonSetting.imageVector, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun ClickableIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Icon(imageVector, contentDescription, Modifier.clickable(onClick = { onClick() }), tint)
}

internal fun LazyGridScope.clickableIcon(imageVector: ImageVector,
                                         contentDescription: String?,
                                         color: Color,
                                         route : () -> Unit) {
    item { ClickableIcon(imageVector, contentDescription, color, route) }
}

internal fun LazyGridScope.editButton(route : () -> Unit) =
    clickableIcon(Icons.Default.Edit, "edit", Color.Green, route)

internal fun LazyGridScope.deleteButton(disabled: Boolean = false, onClick : () -> Unit) {
    item {
        if (disabled) {
            Icon(Icons.Default.Delete, "delete", tint = Color(0x99990000))
        } else {
            Icon(Icons.Default.Delete, "delete", Modifier.clickable(onClick = onClick), Color.Red)
        }
    }
}
internal fun LazyGridScope.viewTextItems(values: List<String>) {
    items(items = values) {
        ReadonlyViewText(it)
    }
}
