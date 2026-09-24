package com.orgzly.android.ui.tasks.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orgzly.R
import com.orgzly.android.sync.SyncRunner
import com.orgzly.android.ui.compose.modifiers.scaffoldPadding
import com.orgzly.android.ui.compose.widgets.OrgzlyTopAppBar
import com.orgzly.android.ui.tasks.TaskDetailState
import com.orgzly.android.ui.tasks.QuickAddState
import com.orgzly.android.ui.tasks.TasksEvent
import com.orgzly.android.ui.tasks.TasksState
import kotlinx.coroutines.flow.Flow
import com.orgzly.android.ui.tasks.model.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    state: TasksState,
    detail: TaskDetailState?,
    quickAdd: QuickAddState,
    events: Flow<TasksEvent>,
    onOpenDrawer: () -> Unit,
    onToggleDone: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onCloseDetail: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Task, String) -> Unit,
    onContentChange: (Task, String?) -> Unit,
    onSetArchived: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onSetDate: (Task, Int, Int, Int) -> Unit,
    onClearDate: (Task) -> Unit,
    onUndoArchive: (Task) -> Unit,
    onUndoDelete: (Task) -> Unit,
    onCommitDelete: (Task) -> Unit,
    onSelectNotebook: (Long) -> Unit,
) {
    var quickAddVisible by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val undoLabel = stringResource(R.string.tasks_undo)
    val archivedMessage = stringResource(R.string.tasks_snackbar_archived)
    val deletedMessage = stringResource(R.string.tasks_snackbar_deleted)

    // showSnackbar suspends until the bar goes away, and reports whether Undo was tapped.
    // Dismissed is the commit path: for a delete that is when the row is really removed.
    LaunchedEffect(events) {
        events.collect { event ->
            val message = when (event) {
                is TasksEvent.Archived -> archivedMessage
                is TasksEvent.Deleted -> deletedMessage
            }

            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )

            when (event) {
                is TasksEvent.Archived ->
                    if (result == SnackbarResult.ActionPerformed) onUndoArchive(event.task)

                is TasksEvent.Deleted ->
                    if (result == SnackbarResult.ActionPerformed) onUndoDelete(event.task)
                    else onCommitDelete(event.task)
            }
        }
    }

    Scaffold(
        topBar = {
            OrgzlyTopAppBar(
                title = stringResource(R.string.tasks_title),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            painterResource(R.drawable.ic_menu),
                            contentDescription = stringResource(R.string.drawer_open),
                        )
                    }
                },
                actions = { TasksOverflowMenu() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { quickAddVisible = true }) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.tasks_add_task),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().scaffoldPadding(padding)) {
            when {
                state.isLoading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.isEmpty ->
                    Text(
                        text = stringResource(R.string.tasks_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )

                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    state.sections.forEach { section ->
                        stickyHeader(key = "header-${section.bucket.name}") {
                            SectionHeader(section.bucket.titleRes, section.tasks.size)
                        }

                        items(section.tasks, key = { it.noteId }) { task ->
                            TaskRow(
                                task = task,
                                bucket = section.bucket,
                                onToggleDone = { onToggleDone(task) },
                                onOpen = { onOpenTask(task) },
                                onSetArchived = { onSetArchived(task, it) },
                                onDelete = { onDelete(task) },
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (quickAddVisible) {
        QuickAddSheet(
            quickAdd = quickAdd,
            onDismiss = { quickAddVisible = false },
            onAdd = onCreate,
            onSelectNotebook = onSelectNotebook,
        )
    }

    if (detail != null) {
        TaskDetailSheet(
            detail = detail,
            onDismiss = onCloseDetail,
            onRename = { onRename(detail.task, it) },
            onContentChange = { onContentChange(detail.task, it) },
            onToggleDone = { onToggleDone(detail.task) },
            onSetArchived = { onSetArchived(detail.task, it) },
            onDelete = { onDelete(detail.task) },
            onSetDate = { y, m, d -> onSetDate(detail.task, y, m, d) },
            onClearDate = { onClearDate(detail.task) },
        )
    }
}

@Composable
private fun SectionHeader(titleRes: Int, count: Int) {
    Text(
        text = "${stringResource(titleRes)}  ·  $count",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun TasksOverflowMenu() {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            painterResource(R.drawable.ic_more_horiz),
            contentDescription = stringResource(R.string.tasks_more),
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.tasks_sync)) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_sync), contentDescription = null)
            },
            onClick = { expanded = false; SyncRunner.startSync() },
        )
    }
}
