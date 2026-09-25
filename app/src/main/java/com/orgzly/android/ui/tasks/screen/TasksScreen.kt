package com.orgzly.android.ui.tasks.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.orgzly.R
import com.orgzly.android.sync.SyncRunner
import com.orgzly.android.ui.compose.modifiers.scaffoldPadding
import com.orgzly.android.ui.compose.widgets.OrgzlyTopAppBar
import com.orgzly.android.ui.tasks.QuickAddState
import com.orgzly.android.ui.tasks.SyncStatus
import com.orgzly.android.ui.tasks.TaskDetailState
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
    syncStatus: SyncStatus,
    events: Flow<TasksEvent>,
    onOpenDrawer: () -> Unit,
    onToggleDone: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onCloseDetail: () -> Unit,
    onNewTask: () -> Unit,
    onPasteTasks: () -> String,
    onCreateMany: (List<String>) -> Unit,
    onRename: (Task, String) -> Unit,
    onContentChange: (Task, String?) -> Unit,
    onSetArchived: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onSetDate: (Task, Int, Int, Int) -> Unit,
    onClearDate: (Task) -> Unit,
    onUndoArchive: (Task) -> Unit,
    onUndoDelete: (Task) -> Unit,
    onCommitDelete: (Task) -> Unit,
    onReorder: (Long, List<Task>) -> Unit,
    onSelectNotebook: (Long) -> Unit,
) {
    // Null until the + is held: the clipboard is read once, at that moment, so the sheet
    // cannot be showing text that was replaced while it was open.
    var pasteText by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val undoLabel = stringResource(R.string.tasks_undo)
    val archivedMessage = stringResource(R.string.tasks_snackbar_archived)
    val deletedMessage = stringResource(R.string.tasks_snackbar_deleted)
    // Resolved from resources rather than pluralStringResource: the count is only known
    // inside the collector, which is not a composable scope.
    val resources = LocalContext.current.resources

    // showSnackbar suspends until the bar goes away, and reports whether Undo was tapped.
    // Dismissed is the commit path: for a delete that is when the row is really removed.
    LaunchedEffect(events) {
        events.collect { event ->
            val message = when (event) {
                is TasksEvent.Archived -> archivedMessage
                is TasksEvent.Deleted -> deletedMessage
                is TasksEvent.Added ->
                    resources.getQuantityString(
                        R.plurals.tasks_snackbar_added, event.count, event.count
                    )
            }

            // Only the reversible events offer a way back; an add has nothing hidden to
            // restore, so it just reports itself.
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (event is TasksEvent.Added) null else undoLabel,
                duration = SnackbarDuration.Short,
            )

            when (event) {
                is TasksEvent.Archived ->
                    if (result == SnackbarResult.ActionPerformed) onUndoArchive(event.task)

                is TasksEvent.Deleted ->
                    if (result == SnackbarResult.ActionPerformed) onUndoDelete(event.task)
                    else onCommitDelete(event.task)

                is TasksEvent.Added -> Unit
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
                actions = {
                    SyncStatusButton(syncStatus) {
                        if (syncStatus == SyncStatus.SYNCING) {
                            SyncRunner.stopSync()
                        } else {
                            SyncRunner.startSync()
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            AddTaskButton(
                onClick = onNewTask,
                onLongClick = { pasteText = onPasteTasks() },
            )
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

                else -> DraggableTaskList(
                    tasks = state.tasks,
                    listState = listState,
                    onToggleDone = onToggleDone,
                    onOpenTask = onOpenTask,
                    onSetArchived = onSetArchived,
                    onDelete = onDelete,
                    onReorder = onReorder,
                )
            }
        }
    }

    pasteText?.let { text ->
        PasteTasksSheet(
            initialText = text,
            quickAdd = quickAdd,
            onDismiss = { pasteText = null },
            onAdd = onCreateMany,
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

/**
 * The + button: tap for one task, hold for a list pasted from the clipboard.
 *
 * Built from a Surface rather than a FloatingActionButton because that only takes onClick,
 * and layering combinedClickable over its own clickable leaves two of them competing for the
 * same press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddTaskButton(onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        shape = FloatingActionButtonDefaults.shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = stringResource(R.string.tasks_add_task),
                onLongClickLabel = stringResource(R.string.tasks_paste_title),
            ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.tasks_add_task),
            )
        }
    }
}
