package io.github.iandbrown.home_energy

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.NavDisplay
import com.softartdev.theme.material3.PreferableMaterialTheme
import io.github.iandbrown.home_energy.ui.LocalBackstack
import io.github.iandbrown.home_energy.ui.Route
import org.koin.compose.navigation3.koinEntryProvider

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App() {
    PreferableMaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            val backStack = remember { mutableStateListOf<Any>(Route.Root) }
            CompositionLocalProvider(LocalBackstack provides backStack) {
                NavDisplay(
                    modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = koinEntryProvider()
                )
            }
        }
    }
}
