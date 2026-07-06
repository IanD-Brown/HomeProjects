package io.github.iandbrown.home_energy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RouteScreen(onNavigate: (Route) -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Home Energy") }, actions = {
                IconButton(onClick = AppState.switchThemeCallback) {
                    Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
                }
            })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = listOf(
                "Meters" to Route.Meters,
                "Usage" to Route.Usage
            )) { (name, route) ->
                ListItem(
                    headlineContent = { Text(name) },
                    modifier = Modifier.clickable {
                        onNavigate(route)
                    }
                )
            }
        }
    }
}
