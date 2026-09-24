package com.orgzly.android.ui.tasks.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import com.orgzly.R
import com.orgzly.android.ui.tasks.NotebookOption
import com.orgzly.android.ui.tasks.QuickAddState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    quickAdd: QuickAddState,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onSelectNotebook: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    var title by remember { mutableStateOf("") }

    fun submit() {
        if (title.isNotBlank()) {
            onAdd(title)
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.tasks_new_task)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            NotebookPicker(
                notebooks = quickAdd.notebooks,
                selected = quickAdd.selected,
                onSelect = onSelectNotebook,
            )

            Button(
                onClick = ::submit,
                enabled = title.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.tasks_add))
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/**
 * Chip showing which notebook the task will land in, opening a menu of the rest.
 *
 * Shown even with a single notebook, because "where is this going?" is worth answering
 * whether or not there is an alternative. Hidden only when no notebook exists yet, in which
 * case creating a task falls back to Orgzly's capture target and makes one.
 */
@Composable
private fun NotebookPicker(
    notebooks: List<NotebookOption>,
    selected: NotebookOption?,
    onSelect: (Long) -> Unit,
) {
    if (notebooks.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Text(selected?.name ?: stringResource(R.string.tasks_quick_add_choose_notebook))
            },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.ic_library_books),
                    contentDescription = stringResource(R.string.tasks_quick_add_notebook),
                    modifier = Modifier.size(18.dp),
                )
            },
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            notebooks.forEach { notebook ->
                DropdownMenuItem(
                    text = { Text(notebook.name) },
                    trailingIcon = {
                        if (notebook.id == selected?.id) {
                            Icon(
                                painterResource(R.drawable.ic_done),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(notebook.id)
                    },
                )
            }
        }
    }
}
