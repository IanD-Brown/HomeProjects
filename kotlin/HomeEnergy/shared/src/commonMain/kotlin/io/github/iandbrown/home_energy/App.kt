package io.github.iandbrown.home_energy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.softartdev.theme.material3.PreferableMaterialTheme
import io.github.iandbrown.home_energy.ui.AppState
import io.github.iandbrown.home_energy.ui.MeterActivity

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App() {
    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            val backStack = remember { mutableStateListOf<Recipe>() }
            backStack.add(Recipe("Root", { RouteScreen { backStack.add(it) } }))
            NavDisplay(
                modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Recipe> { it.activityFun() }
                }
            )

        }
    }
}

@Composable
private fun RouteScreen(backStack: (Recipe) -> Unit) {
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
                Recipe("Meters", { MeterActivity() }),
            )) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    modifier = Modifier.clickable {
                        backStack(item)
                        item.activityFun
                    }
                )
            }
        }
    }

}

internal class Recipe(val name: String, val activityFun: @Composable () -> Unit) {
    override fun toString() = name
}
