package org.idb.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.idb.database.SeasonCompetition

val fontSize = 16.sp
private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

@Composable
fun viewCommon(
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
            createTopBar(navController, title, description)
        }, floatingActionButton = floatingActionButton,
            bottomBar = bottomBar,
            content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createTopBar(navController: NavController, title: String, description: String) {
    TopAppBar(title = { Text(title) }, navigationIcon = {
        IconButton(onClick = { navController.navigateUp() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = description)
        }
    })
}

@Composable
fun createFloatingAction(navController: NavController, route: String) {
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
fun textStyle(): TextStyle = TextStyle.Default.copy(fontSize = fontSize, color = MaterialTheme.colors.onSurface)

@Composable
fun textFieldColors(): TextFieldColors = TextFieldDefaults.textFieldColors(
    textColor = MaterialTheme.colors.onSurface,
    cursorColor = MaterialTheme.colors.onSurface,
    backgroundColor = MaterialTheme.colors.surface,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Composable
fun spacedViewText(value : String, modifier: Modifier = Modifier) {
    Spacer(modifier = Modifier.size(16.dp))
    ViewText(value, modifier)
}

@Composable
fun spacedIcon(imageVector: ImageVector,
               contentDescription: String?,
               tint: Color = MaterialTheme.colors.surface,
               onClick : () -> Unit) {
    Spacer(modifier = Modifier.size(16.dp))
    Icon(imageVector, contentDescription, Modifier.clickable(onClick = onClick), tint)
}

@Composable
fun itemButtons(editClick : () -> Unit, deleteClick : () -> Unit) {
    spacedIcon(Icons.Default.Edit, "edit", Color.Green, editClick)
    spacedIcon(Icons.Default.Delete, "delete", Color.Red, deleteClick)
}

// Creating a composable to display a drop-down menu
@Composable
fun DropdownList(
    itemList: List<String>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit
) {
    // Declaring a boolean value to store
    // the expanded state of the Text Field
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(itemList[selectedIndex]) }
    var textFieldSize by remember { mutableStateOf(Size.Zero)}

    // Up Icon when expanded and down icon when collapsed
    val icon = if (expanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown

    Column(Modifier.padding(20.dp)) {
        ViewTextField(
            value = selectedText,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    // This value is used to assign to
                    // the DropDown the same width
                    textFieldSize = coordinates.size.toSize()
                },
            trailingIcon = {
                Icon(icon,"contentDescription",
                    Modifier.clickable { expanded = !expanded })
            }
        ) {}

        // Create a drop-down menu with list of cities,
        // when clicked, set the Text Field text as the city selected
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current){textFieldSize.width.toDp()})
        ) {
            itemList.forEach { label ->
                DropdownMenuItem(
                    text = { Text(text = label) },
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


// Creating a composable to display a drop-down menu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun datePicker(current: Long, modifier : Modifier, isSelectable : (Long) -> Boolean, onItemClick: (Long) -> Unit) {
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

fun dateToMillis(dateString : String) : Long {
    val date = dateFormatter.parse(dateString)
    return date.toInstant().toEpochMilli()
}

fun isMondayIn(season : SeasonCompetition, utcMs : Long) : Boolean {
    if (utcMs >= season.startDate &&
        utcMs <= season.endDate) {
        val calendar = Calendar.getInstance()
        calendar.setTimeInMillis(utcMs)

        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
    }
    return false
}
