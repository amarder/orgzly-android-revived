package com.orgzly.android.ui.tasks.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orgzly.R
import com.orgzly.android.ui.tasks.TaskDetailState
import com.orgzly.android.ui.tasks.TasksNavigation
import com.orgzly.android.ui.tasks.model.Task
import org.joda.time.DateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    detail: TaskDetailState,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onContentChange: (String?) -> Unit,
    onToggleDone: () -> Unit,
    onSetArchived: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSetDate: (year: Int, month0: Int, day: Int) -> Unit,
    onClearDate: () -> Unit,
) {
    val context = LocalContext.current
    val task = detail.task
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(task.noteId) { mutableStateOf(task.title) }
    var body by remember(task.noteId, detail.isLoadingContent) {
        mutableStateOf(detail.content.orEmpty())
    }
    // Title and body are committed on dismiss rather than per keystroke, so that a note is
    // written (and a sync triggered) once per edit instead of once per character.
    fun commitAndDismiss() {
        if (title.trim() != task.title) onRename(title)
        if (body.takeIf { it.isNotBlank() } != detail.content) onContentChange(body)
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = ::commitAndDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.tasks_detail_title)) },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.tasks_detail_notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = {
                        val start = task.due?.millis?.let { DateTime(it) } ?: DateTime.now()
                        DatePickerDialog(
                            context,
                            { _, year, month0, day -> onSetDate(year, month0, day) },
                            start.year,
                            start.monthOfYear - 1, // DatePickerDialog months are 0-based
                            start.dayOfMonth,
                        ).show()
                    },
                    label = {
                        Text(
                            task.due?.let { formatDueDate(context, it.millis) }
                                ?: stringResource(R.string.tasks_detail_set_date)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_today),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )

                if (task.due != null) {
                    TextButton(onClick = onClearDate) {
                        Text(stringResource(R.string.tasks_detail_clear_date))
                    }
                }
            }

            Text(
                text = stringResource(R.string.tasks_detail_notebook, task.bookName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SheetAction(
                iconRes = R.drawable.ic_check_circle_outline,
                labelRes = if (task.isDone) R.string.tasks_mark_not_done else R.string.tasks_mark_done,
                onClick = { onToggleDone(); onDismiss() },
            )

            SheetAction(
                iconRes = R.drawable.ic_move_to_inbox,
                labelRes = R.string.tasks_archive,
                onClick = { commitAndDismiss(); onSetArchived(true) },
            )

            SheetAction(
                iconRes = R.drawable.ic_library_books,
                labelRes = R.string.tasks_open_in_orgzly,
                onClick = {
                    commitAndDismiss()
                    TasksNavigation.openNoteInOrgzly(context, task.bookId, task.noteId)
                },
            )

            SheetAction(
                iconRes = R.drawable.ic_delete,
                labelRes = R.string.tasks_delete,
                destructive = true,
                onClick = { commitAndDismiss(); onDelete() },
            )
        }
    }

}

@Composable
private fun SheetAction(
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val tint =
        if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

