package com.orgzly.android.ui.tasks.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orgzly.R
import com.orgzly.android.ui.tasks.model.Task
import kotlinx.coroutines.launch

/**
 * A task row: swipe right to archive, swipe left to delete.
 *
 * Neither gesture asks for confirmation. Both are reported to the caller, which offers an
 * undo instead - cheaper to dismiss than a dialog, and it does not interrupt the gesture.
 */
@Composable
fun TaskRow(
    task: Task,
    onToggleDone: () -> Unit,
    onOpen: () -> Unit,
    onSetArchived: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Deliberately plain remember, not rememberSwipeToDismissBoxState: that saves the
    // dismissed offset, and a restored non-Settled value makes SwipeToDismissBox fire
    // onDismiss again on recomposition. For gestures that archive or delete, replaying the
    // action is data loss, so the swipe position is simply not worth persisting.
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val swipeState = remember(task.noteId) {
        SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold)
    }

    // SwipeToDismissBox invokes onDismiss on every recomposition while the box is held at a
    // dismissed offset -- and archiving recomposes this row, because the write comes straight
    // back through the query flow. Without a latch that is an unbounded loop: one swipe
    // measured 72 writes, toggling the ARCHIVE tag on and off. Re-arm once the row settles,
    // so a later, deliberate second swipe still works.
    var armed by remember(task.noteId) { mutableStateOf(true) }

    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeToDismissBoxValue.Settled) {
            armed = true
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        backgroundContent = { SwipeBackground(swipeState.dismissDirection) },
        onDismiss = { direction ->
            if (!armed) return@SwipeToDismissBox
            armed = false

            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSetArchived(true)
                    // The row leaves the list as soon as the tag lands, but settle the box
                    // back anyway: if the write fails the row stays, and it must not stay
                    // parked open showing only its background.
                    scope.launch { swipeState.reset() }
                }

                SwipeToDismissBoxValue.EndToStart -> onDelete()

                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
    ) {
        TaskRowContent(task, onToggleDone, onOpen)
    }

}

@Composable
private fun TaskRowContent(
    task: Task,
    onToggleDone: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            // Completed tasks stay exactly where they were, just receded. Same treatment
            // Orgzly's own list gives done and archived notes.
            .alpha(if (task.isDone) 0.5f else 1f)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TaskCircle(
            isDone = task.isDone,
            onClick = onToggleDone,
        )

        // Just the headline. The circle already says whether it is done, the section header
        // already says when it is due, and the notebook is in the detail sheet.
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = rememberNoteListTextSize(R.attr.item_head_title_text_size),
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Asana-style tap target: an open circle that fills in when the task is done. */
@Composable
private fun TaskCircle(
    isDone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (isDone) R.string.tasks_mark_not_done else R.string.tasks_mark_done
    )

    Box(
        modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (isDone) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle_outline),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Box(
                Modifier
                    .size(21.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
            )
        }
    }
}


/** Colour and icon revealed behind the row, matching whichever way it is being dragged. */
@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    if (direction == SwipeToDismissBoxValue.Settled) {
        Box(Modifier.fillMaxSize())
        return
    }

    val isArchiveGesture = direction == SwipeToDismissBoxValue.StartToEnd

    val background =
        if (isArchiveGesture) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.errorContainer

    val foreground =
        if (isArchiveGesture) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onErrorContainer

    val label = stringResource(
        if (isArchiveGesture) R.string.tasks_archive else R.string.tasks_delete
    )

    Row(
        Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 20.dp),
        horizontalArrangement =
            if (isArchiveGesture) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(
                    if (isArchiveGesture) R.drawable.ic_move_to_inbox else R.drawable.ic_delete
                ),
                contentDescription = label,
                tint = foreground,
                modifier = Modifier.size(22.dp),
            )
            Text(label, style = MaterialTheme.typography.labelLarge, color = foreground)
        }
    }
}
