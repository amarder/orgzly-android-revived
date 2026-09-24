package com.orgzly.android.ui.tasks.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orgzly.R
import com.orgzly.android.ui.tasks.model.DueKind
import com.orgzly.android.ui.tasks.model.Task
import com.orgzly.android.ui.tasks.model.TaskBucket

@Composable
fun TaskRow(
    task: Task,
    bucket: TaskBucket,
    onToggleDone: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            // Completed tasks stay exactly where they were, just receded. Same treatment
            // Orgzly's own list gives done and archived notes.
            .alpha(if (task.isDone) 0.5f else 1f)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TaskCircle(
            isDone = task.isDone,
            onClick = onToggleDone,
            modifier = Modifier.padding(top = 2.dp),
        )

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Only the headline: the state keyword is what the circle already communicates.
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                task.due?.let { due ->
                    DueChip(millis = due.millis, kind = due.kind, bucket = bucket)
                }

                Text(
                    text = task.bookName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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

@Composable
private fun DueChip(millis: Long, kind: DueKind, bucket: TaskBucket) {
    val context = LocalContext.current

    val isUrgent = bucket == TaskBucket.OVERDUE || bucket == TaskBucket.TODAY

    val background =
        if (isUrgent) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceVariant

    val foreground =
        if (isUrgent) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        Modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(
                if (kind == DueKind.DEADLINE) R.drawable.ic_alarm else R.drawable.ic_today
            ),
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = formatDueDate(context, millis),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
        )
    }
}
