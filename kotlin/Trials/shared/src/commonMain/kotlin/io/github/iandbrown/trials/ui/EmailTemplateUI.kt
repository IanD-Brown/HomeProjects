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
import org.koin.compose.viewmodel.koinViewModel

internal class EmailTemplateViewModel(dao: EmailTemplateDao) :
    CRUDViewModel<EmailTemplateDao, EmailTemplate>(dao = dao)

@Composable
internal fun EmailTemplateView() {
    val viewModel: EmailTemplateViewModel = koinViewModel()
    val state by viewModel.getState().collectAsState()
    var editingTemplate by remember { mutableStateOf<EmailTemplate?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    if (isAdding || editingTemplate != null) {
        EmailTemplateEditor(
            template = editingTemplate,
            onSave = { template ->
                if (editingTemplate == null) {
                    viewModel.insert(template)
                } else {
                    viewModel.update(template)
                }
                editingTemplate = null
                isAdding = false
            },
            onCancel = {
                editingTemplate = null
                isAdding = false
            }
        )
    } else {
        ViewCommon(
            title = "Email Templates",
            states = persistentListOf(state),
            bottomBar = {
                BottomBarWithButtons(
                    addButtonSettings(onClick = { isAdding = true })
                )
            }
        ) { padding ->
            TrailingIconLazyVerticalGrid(padding, 2, 2) {
                viewTextItems(listOf("Name", "Body Template", "", ""))
                state.values().forEach { template ->
                    viewTextItems(listOf(template.name, template.bodyTemplate))
                    editButton { editingTemplate = template }
                    deleteButton { viewModel.delete(template) }
                }
            }
        }
    }
}

@Composable
internal fun EmailTemplateEditor(
    template: EmailTemplate?,
    onSave: (EmailTemplate) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var bodyTemplate by remember { mutableStateOf(template?.bodyTemplate ?: "") }
    val isEditing = template != null

    val isValid = name.isNotBlank()

    ViewCommon(
        title = if (isEditing) "Edit Email Template" else "Add Email Template",
        description = "Cancel editing",
        bottomBar = {
            BottomBarWithButton(enabled = isValid) {
                onSave(EmailTemplate(name = name, bodyTemplate = bodyTemplate))
            }
        },
        confirm = { name.isNotBlank() },
        confirmAction = { onSave(EmailTemplate(name = name, bodyTemplate = bodyTemplate)) }
    ) { padding ->
        TrailingIconLazyVerticalGrid(padding, 2, 0) {
            gridEntry(name, "Name", onValueChange = { if (!isEditing) name = it })
            gridEntry(bodyTemplate, "Body Template", onValueChange = { bodyTemplate = it })
        }
    }
}
