package io.github.iandbrown.trials.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.iandbrown.trials.database.EmailTemplate
import io.github.iandbrown.trials.database.EmailTemplateDao
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.koin.compose.viewmodel.koinViewModel

internal class EmailTemplateViewModel(dao: EmailTemplateDao) :
    CRUDViewModel<EmailTemplateDao, EmailTemplate>(dao = dao)

@Composable
internal fun EmailTemplateView(showEditor: (EmailTemplate?) -> Unit) {
    val viewModel: EmailTemplateViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()

    ViewCommon(
        title = "Email Templates",
        states = persistentListOf(state),
        bottomBar = {
            BottomBarWithButtons(
                addButtonSettings(onClick = { showEditor(null) })
            )
        }
    ) { padding ->
        TrailingIconLazyVerticalGrid(padding, 3, 2) {
            viewTextItems(listOf("Name", "To", "Subject", "", ""))
            state.values().forEach { template ->
                viewTextItems(listOf(template.name, template.toTemplate, template.subjectTemplate))
                editButton { showEditor(template) }
                deleteButton { viewModel.delete(template) }
            }
        }
    }
}

@Composable
internal fun EmailTemplateEditor(template: EmailTemplate?, done: () -> Unit) {
    val viewModel: EmailTemplateViewModel = koinViewModel()
    var name by remember { mutableStateOf(template?.name ?: "") }
    var toTemplate by remember { mutableStateOf(template?.toTemplate ?: "") }
    var copyTemplate by remember { mutableStateOf(template?.copyTemplate ?: "") }
    var subjectTemplate by remember { mutableStateOf(template?.subjectTemplate ?: "") }
    var bodyTemplate by remember { mutableStateOf(template?.bodyTemplate ?: "") }

    var editorState by remember { mutableStateOf(EditorState.CLEAN) }

    fun editorTemplate(): EmailTemplate =
        EmailTemplate(name, toTemplate, copyTemplate, subjectTemplate, bodyTemplate)

    fun allBlank(): Boolean =
        name.isBlank() && toTemplate.isBlank() && copyTemplate.isBlank()  && subjectTemplate.isBlank() && bodyTemplate.isBlank()

    fun setEditorState() {
        editorState = if ((template == null && allBlank()) || editorTemplate() == template) {
            EditorState.CLEAN
        } else if ((template == null && name.isBlank() && !allBlank())) {
            EditorState.DIRTY
        } else {
            EditorState.VALID
        }
    }

    fun save() {
        val editorTemplate = editorTemplate()
        if (template == null) {
            viewModel.insert(editorTemplate)
        } else if (editorTemplate != template) {
            viewModel.update(editorTemplate)
        }
    }

    ViewCommon(
        title = if (template != null) "Edit Email Template" else "Add Email Template",
        description = "Return to email templates",
        bottomBar = {
            BottomBarWithButton(enabled = editorState == EditorState.VALID) {
                save()
                done()
            }
        },
        confirm = { editorState == EditorState.VALID },
        confirmAction = { save ->
            if (save) {
                save()
            }
            done()
        }
    ) { padding ->
        WeightedIconLazyVerticalGrid(padding, 0, 1, 4) {
            gridEntry(name, "Name", readOnly = template != null) {
                name = it
                setEditorState()
            }
            gridEntry(toTemplate, "To") {
                toTemplate = it
                setEditorState()
            }
            gridEntry(copyTemplate, "Copy") {
                copyTemplate = it
                setEditorState()
            }
            gridEntry(subjectTemplate, "Subject") {
                subjectTemplate = it
                setEditorState()
            }
            gridEntry(bodyTemplate, "Body", lineCount = 10) {
                bodyTemplate = it
                setEditorState()
            }
        }
    }
}

internal fun toDataFrame(entities: List<EmailTemplate>): DataFrame<Any> =
    entities.toDataFrame {
        "Name" from { it.name }
        "To" from { it.toTemplate }
        "Copy" from {it.copyTemplate}
        "Subject" from {it.subjectTemplate}
        "Body" from {it.bodyTemplate}
    }
