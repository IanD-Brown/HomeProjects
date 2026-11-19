package io.github.iandbrown.trials.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.iandbrown.trials.database.EmailTemplateDao
import io.github.iandbrown.trials.database.NominationDao
import io.github.iandbrown.trials.database.StateDao
import io.github.iandbrown.trials.database.StateWithNextStatesDao
import io.github.iandbrown.trials.database.TeamCategoryDao
import kotlinx.io.Buffer
import kotlinx.io.writeString
import org.jetbrains.kotlinx.dataframe.api.at
import org.jetbrains.kotlinx.dataframe.api.insert
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RouteScreen(onNavigate: (Route) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var exception by remember { mutableStateOf<Exception?>(null) }
    val setupExporter: SetupExporter = koinInject()

    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Trials") }, actions = {
                IconButton(onClick = AppState.switchThemeCallback) {
                    Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null)
                }
                IconButton(onClick = { onNavigate(Route.SettingView) }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                }
            })
        },
        bottomBar = {
            BottomBarWithButtons(
                exportButtonSettings(coroutineScope, "trialsConfig") { buffer ->
                    try {
                        setupExporter.export(buffer)
                    } catch (e: Exception) {
                        logException("HomeScreen", e, "Export failed:")
                        exception = e
                    }
                },
            )
        }
    ) { padding ->
        if (exception != null) {
            val message = exception?.message ?: exception?.javaClass?.simpleName ?: ""
            AlertDialog(
                onDismissRequest = { exception = null },
                confirmButton = { Button(onClick = { exception = null }) { Text("OK") } },
                title = { ViewText("Error") },
                text = { ViewText(message) }
            )
        } else {

            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = listOf(
                        "Nominations" to Route.Nominations,
                        "Email Templates" to Route.EmailTemplates,
                        "States" to Route.States,
                        "Team Categories" to Route.TeamCategories,
                        "Trials" to Route.TrialsList
                    )
                ) { (name, route) ->
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
}

private const val ENTITY = "entity"
private const val TEAM_CATEGORY = "TeamCategory"
private const val STATE = "State"
private const val NEXT_STATE = "NextState"
private const val EMAIL_TEMPLATE = "EmailTemplate"

internal data class SetupExporter(val teamCategoryDao: TeamCategoryDao,
                             val stateDao: StateDao,
                             val stateWithNextStatesDao: StateWithNextStatesDao,
                             val emailTemplateDao: EmailTemplateDao,
                             val nominationDao: NominationDao) {
    suspend fun export(destination: Buffer) {
        val teamCategories = teamCategoryDao.get()
        val states = stateDao.get()
        val nextStates = stateWithNextStatesDao.get()
        val emailTemplates = emailTemplateDao.get()
        val nominationsWithAdultDetails = nominationDao.getNominationsWithAdultDetails()

        val sb = StringBuilder()
        (0 until 1).toDataFrame {
            "teamCategories" from { toDataFrame(teamCategories).insert(ENTITY) { TEAM_CATEGORY }.at(0) }
            "states" from { toDataFrame(states).insert(ENTITY) { STATE }.at(0) }
            "nextStates" from { toDataFrame(nextStates).insert(ENTITY) {NEXT_STATE}.at(0) }
            "emailTemplates" from { toDataFrame(emailTemplates).insert(ENTITY) { EMAIL_TEMPLATE }.at(0) }
            "nominations" from { toDataFrame(nominationsWithAdultDetails).insert(ENTITY) { "Nomination" }.at(0) }
        }.writeJson(sb)
        destination.writeString(sb.toString())
    }
}
