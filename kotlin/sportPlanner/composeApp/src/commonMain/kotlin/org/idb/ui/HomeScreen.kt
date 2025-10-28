package org.idb.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class Editors(val displayName: String) { ASSOCIATIONS("Associations") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun homeScreen(navController: NavController) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { Text("Season Planner") }, actions = {
            IconButton(onClick = AppState.switchThemeCallback) {
                Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
            }
        })
    }, content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues), content = {
            items(items = Editors.entries.toTypedArray(), key = { entry -> entry.ordinal }) { editor ->
                OutlinedButton( onClick = { navController.navigate(editor.name) },
                    modifier = Modifier.padding(6.dp),
                ) {
                    Text(
                        editor.displayName,
                    )
                }
            }
        })
    })
}