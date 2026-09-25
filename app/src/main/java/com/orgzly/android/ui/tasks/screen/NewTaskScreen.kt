package com.orgzly.android.ui.tasks.screen

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orgzly.R
import com.orgzly.android.ui.compose.modifiers.scaffoldPadding
import com.orgzly.android.ui.compose.widgets.OrgzlyTopAppBar
import com.orgzly.android.ui.tasks.QuickAddState
import com.orgzly.android.ui.tasks.model.TaskDate

/**
 * Everything a new task is, and nothing else: a title, a notebook, a date and some notes.
 *
 * Orgzly's own editor opened faster than any sheet, which is why the + used to go there, but
 * it also offers tags, priority, a deadline, state and arbitrary properties - none of which
 * belong in the way of writing down a thing to do. This keeps the speed, being a plain screen
 * in the same window so the keyboard rises with it, and drops the rest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskScreen(
    quickAdd: QuickAddState,
    onCancel: () -> Unit,
    onCreate: (title: String, content: String?, date: TaskDate?) -> Unit,
    onSelectNotebook: (Long) -> Unit,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<TaskDate?>(TaskDate.today()) }

    fun submit() {
        if (title.isNotBlank()) onCreate(title, notes, date)
    }

    BackHandler(onBack = onCancel)

    Scaffold(
        topBar = {
            OrgzlyTopAppBar(
                title = stringResource(R.string.tasks_new_task),
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.tasks_new_task_cancel),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = ::submit, enabled = title.isNotBlank()) {
                        Icon(
                            painterResource(R.drawable.ic_done),
                            contentDescription = stringResource(R.string.tasks_add),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .scaffoldPadding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.tasks_detail_title)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.tasks_detail_notes)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = {
                        val start = date ?: TaskDate.today()
                        DatePickerDialog(
                            context,
                            { _, year, month0, day -> date = TaskDate(year, month0, day) },
                            start.year,
                            start.month0,
                            start.day,
                        ).show()
                    },
                    label = {
                        Text(
                            date?.let { formatDueDate(context, it.toMillis()) }
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

                if (date != null) {
                    TextButton(onClick = { date = null }) {
                        Text(stringResource(R.string.tasks_detail_clear_date))
                    }
                }
            }

            if (quickAdd.notebooks.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NotebookPicker(
                        notebooks = quickAdd.notebooks,
                        selected = quickAdd.selected,
                        onSelect = onSelectNotebook,
                    )
                }
            }
        }
    }

    // A plain screen in the main window, so focus and the keyboard arrive together - the
    // desync that made the old bottom sheet feel slow is not possible here.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
