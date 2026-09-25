package com.orgzly.android.ui.tasks.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orgzly.R
import com.orgzly.android.ui.tasks.ClipboardTasks
import com.orgzly.android.ui.tasks.QuickAddState

/**
 * Many tasks at once, one per line, reached by holding the + button.
 *
 * Pre-filled from the clipboard but fully editable, so the text can be tidied before it turns
 * into notes. Unlike the + button this is a considered action rather than a fast one, so it
 * stays a sheet and deliberately does not steal focus on open - nothing here has to race the
 * keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteTasksSheet(
    initialText: String,
    quickAdd: QuickAddState,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
    onSelectNotebook: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var text by remember { mutableStateOf(initialText) }

    val titles = remember(text) { ClipboardTasks.parseTaskLines(text) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.tasks_paste_title),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.tasks_paste_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp),
            )

            NotebookPicker(
                notebooks = quickAdd.notebooks,
                selected = quickAdd.selected,
                onSelect = onSelectNotebook,
            )

            Button(
                onClick = {
                    onAdd(titles)
                    onDismiss()
                },
                enabled = titles.isNotEmpty(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.tasks_paste_add, titles.size))
            }
        }
    }
}
