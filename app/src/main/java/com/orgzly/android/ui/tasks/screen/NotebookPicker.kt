package com.orgzly.android.ui.tasks.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orgzly.R
import com.orgzly.android.ui.tasks.NotebookOption

/**
 * Chip showing which notebook tasks will land in, opening a menu of the rest.
 *
 * Shown even with a single notebook, because "where is this going?" is worth answering
 * whether or not there is an alternative. Hidden only when no notebook exists yet, in which
 * case creating a task falls back to Orgzly's capture target and makes one.
 */
@Composable
fun NotebookPicker(
    notebooks: List<NotebookOption>,
    selected: NotebookOption?,
    onSelect: (Long) -> Unit,
) {
    if (notebooks.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { expanded = true },
            // Capped and clipped: the menu shows the full names, so the chip only has to say
            // enough to recognise the one in use.
            modifier = Modifier.widthIn(max = 220.dp),
            label = {
                Text(
                    text = selected?.name
                        ?: stringResource(R.string.tasks_quick_add_choose_notebook),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
