package io.github.iandbrown.reconciler.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class Editors(val displayName: String, val showOnHome: Boolean = true) {
    RULES("Rules"),
    ALL_TRANSACTIONS("All Transactions"),
    SUMMARY_BY_CATEGORY("Summary By Category"),
    SPENDING_SUMMARY("Spending Summary"),
    IMPORT_DEFINITION("Import Definition");

    fun viewRoute() : String = "$name/View"
    fun addRoute() : String = "$name/Add"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { Text("Account reconcile") }, actions = {
            IconButton(onClick = AppState.switchThemeCallback) {
                Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
            }
        })
    }, content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            items(items = Editors.entries.filter { it.showOnHome }.toTypedArray(), key = { entry -> entry.ordinal }) { editor ->
                OutlinedTextButton(value = editor.displayName)
                { appNavController.navigate(editor.viewRoute()) }
            }
        })
    })
}
