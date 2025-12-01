package io.github.iandbrown.sportplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val fontSize = 16.sp
private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

@Composable
fun ViewCommon(
    baseUiState: BaseUiState,
    navController: NavController,
    title: String,
    floatingActionButton: @Composable () -> Unit,
    description: String = "Return to home screen",
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    if (baseUiState.loadingInProgress()) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp).align(Alignment.Center))
        }
    } else if (baseUiState.hasData()) {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            CreateTopBar(navController, title, description)
        }, floatingActionButton = floatingActionButton,
            bottomBar = bottomBar,
            content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTopBar(navController: NavController, title: String, description: String) {
    TopAppBar(title = { Text(title) }, navigationIcon = {
        IconButton(onClick = { navController.navigateUp() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = description)
        }
    })
}

@Composable
fun CreateFloatingAction(navController: NavController, route: String) {
    FloatingActionButton(onClick = {
        navController.navigate(route)
    }, content = {
        Icon(
            imageVector = Icons.Default.Add, contentDescription = "image", tint = Color.White
        )
    })
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
fun ViewTextField(value : String, modifier : Modifier, isError : () -> Boolean, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        colors = textFieldColors(),
        textStyle = textStyle(),
        isError = isError())
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
fun SpacedViewText(value : String, modifier: Modifier = Modifier) {
    Spacer(modifier = Modifier.size(16.dp))
    ViewText(value, modifier)
}

@Composable
fun SpacedIcon(imageVector: ImageVector,
               contentDescription: String?,
               tint: Color = MaterialTheme.colorScheme.onSurface,
               onClick : () -> Unit) {
    Spacer(modifier = Modifier.size(16.dp))
    Icon(imageVector, contentDescription, Modifier.clickable(onClick = onClick), tint)
}

@Composable
fun ItemButtons(editClick : () -> Unit, deleteClick : () -> Unit) {
    SpacedIcon(Icons.Default.Edit, "edit", Color.Green, editClick)
    SpacedIcon(Icons.Default.Delete, "delete", Color.Red, deleteClick)
}

// Creating a composable to display a drop-down menu
@Composable
fun DropdownList(
    itemList: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    isLocked: () -> Boolean = { false },
    onItemClick: (Int) -> Unit
) {
    // Declaring a boolean value to store
    // the expanded state of the Text Field
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(itemList[selectedIndex]) }

    // Up Icon when expanded and down icon when collapsed
    val icon = if (expanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth().padding(0.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                if (!isLocked()) {
                    expanded = !expanded
                }
            },
    ) {
        ViewTextField(
            value = selectedText,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (!isLocked()) {
                    Icon(
                        icon, "contentDescription",
                        Modifier.clickable { expanded = !expanded })
                }
            }
        ) {}
        if (!isLocked()  && expanded) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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


// Creating a composable to display a drop-down menu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerView(current: Long, modifier : Modifier, isSelectable : (Long) -> Boolean, onItemClick: (Long) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    ViewTextField(
        value = convertMillisToDate(current),
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
        val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = isSelectable(utcTimeMillis)
        })
        val onDismissRequest = { showDatePicker = false }

        datePickerState.selectedDateMillis = current

        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    enabled = (datePickerState.selectedDateMillis ?: 0) > 0L,
                    onClick = {
                        onItemClick(datePickerState.selectedDateMillis!!)
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

fun convertMillisToDate(millis: Long?): String =
    when (millis) {
        null -> ""
        0L -> "dd/mm/yyyy"
        else -> {
            dateFormatter.format(Date(millis))
        }
    }

fun isMondayIn(seasonCompetition : SeasonCompetition, utcMs : Long) : Boolean =
    isMondayIn(seasonCompetition.startDate, seasonCompetition.endDate, utcMs)

fun isMondayIn(startDate : Long, endDate: Long, utcMs : Long) : Boolean {
    if (utcMs in startDate..endDate) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = utcMs

        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
    }
    return false
}
