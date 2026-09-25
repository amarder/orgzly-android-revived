package com.orgzly.android.ui.tasks.screen

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.orgzly.R
import com.orgzly.android.ui.tasks.model.Task
import com.orgzly.android.ui.tasks.model.TaskListEntries
import com.orgzly.android.ui.tasks.model.TaskListEntry
import com.orgzly.android.ui.tasks.model.TaskOrdering
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

/**
 * The task list, with long-press-and-drag reordering inside a single day.
 *
 * Hand-rolled rather than pulling in a reordering library: the fork keeps its dependency list
 * identical to upstream's, and the constrained behaviour wanted here - a drag that cannot
 * leave its day - is most of what a library would have to be talked out of anyway.
 *
 * Long-press is the trigger because the rows already own both other gestures: a tap opens the
 * task and a horizontal swipe archives or deletes it. detectDragGesturesAfterLongPress only
 * claims the pointer once the press has been held, so the swipe still gets its chance first.
 */
@Composable
fun DraggableTaskList(
    tasks: List<Task>,
    listState: LazyListState,
    onToggleDone: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onSetArchived: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onReorder: (Long, List<Task>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    // Modifier.pointerInput keeps the lambda it was first given until its key changes, so
    // anything the gesture reads directly is frozen at the composition that created it. A row
    // added before the others would then work out its day from a list that no longer exists -
    // which is how a drag ended up greying the wrong rows, or refusing to start at all.
    val currentTasks by rememberUpdatedState(tasks)
    val currentOnReorder by rememberUpdatedState(onReorder)

    val dragState = remember { mutableStateOf<DragState?>(null) }
    var drag by dragState

    // The list re-emits from the database on every write, so the incoming order cannot be
    // trusted mid-gesture: a snapshot is held from the moment the drag starts until it ends,
    // otherwise the row being dragged jumps out from under the finger.
    val rows = drag?.tasks ?: tasks

    DragAutoScroll(dragState, listState)

    // Built once per recomposition rather than fed straight from itemsIndexed: once header
    // rows are interleaved, a LazyColumn item's own index counts headers too and no longer
    // matches a task's position in rows, which is what the drag math below is keyed on.
    val entries = remember(rows) { TaskListEntries.build(rows) }

    LazyColumn(modifier.fillMaxSize(), state = listState) {
        items(
            entries,
            key = { entry ->
                when (entry) {
                    is TaskListEntry.Header -> "header:${entry.dayBucket}"
                    is TaskListEntry.Row -> entry.task.noteId
                }
            },
        ) { entry ->
            when (entry) {
                is TaskListEntry.Header -> TaskDateHeader(entry.dayBucket)

                is TaskListEntry.Row -> {
                    val task = entry.task
                    val index = entry.taskIndex
                    val isDragging = drag?.draggedNoteId == task.noteId

                    // Read outside graphicsLayer so the lift follows every layout pass.
                    val dragTranslation =
                        if (isDragging) drag?.translation(listState) ?: 0f else 0f

                    // Rows the dragged task cannot reach are dimmed - without a cue a drag
                    // that refuses to go further just looks broken.
                    val outOfReach = drag?.let { index !in it.range } ?: false

                    TaskRow(
                        task = task,
                        onToggleDone = { onToggleDone(task) },
                        onOpen = { onOpenTask(task) },
                        onSetArchived = { onSetArchived(task, it) },
                        onDelete = { onDelete(task) },
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragTranslation else 0f
                                shadowElevation = if (isDragging) 8f else 0f
                            }
                            .alpha(if (outOfReach) 0.35f else 1f)
                            .pointerInput(task.noteId) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        DragState.start(currentTasks, task.noteId, listState)
                                            ?.let {
                                                haptics.performHapticFeedback(
                                                    HapticFeedbackType.LongPress
                                                )
                                                drag = it
                                            }
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        drag = drag?.dragBy(amount.y)
                                    },
                                    onDragEnd = {
                                        drag?.let {
                                            currentOnReorder(it.draggedNoteId, it.movedGroup())
                                        }
                                        drag = null
                                    },
                                    onDragCancel = { drag = null },
                                )
                            },
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

/** A day divider, echoing the built-in Agenda view's elevated date bar. */
@Composable
private fun TaskDateHeader(dayBucket: Long?) {
    val context = LocalContext.current
    val label = dayBucket?.let { formatDueDate(context, it) }
        ?: stringResource(R.string.tasks_date_none)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = rememberNoteListTextSize(R.attr.item_head_post_title_text_size),
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private const val AUTO_SCROLL_EDGE = 96f

private const val AUTO_SCROLL_STEP = 12f

/**
 * Scrolls the list while a dragged row is held near either edge, so a task can travel further
 * than one screenful without being let go of.
 *
 * Driven off a snapshotFlow rather than the drag callbacks because a finger held still at the
 * edge produces no further pointer events, and that is exactly when scrolling has to continue.
 */
@Composable
private fun DragAutoScroll(drag: MutableState<DragState?>, listState: LazyListState) {
    LaunchedEffect(listState) {
        snapshotFlow { drag.value?.edgeDistance(listState) }
            .distinctUntilChanged()
            .collect { distance ->
                if (distance == null) return@collect

                while (drag.value?.edgeDistance(listState) == distance) {
                    listState.scrollBy(distance)
                    drag.value = drag.value?.dragBy(distance)
                }
            }
    }
}

/**
 * Everything a drag in progress needs to know, kept in one immutable value so each pointer
 * event produces a new state rather than mutating shared bookkeeping.
 *
 * The order shown is *derived* from [original] on every event - the dragged task lifted out
 * and put back at [target] - rather than being edited in place. Editing in place meant each
 * event's correction was computed against the previous event's result, and pointer events
 * arrive faster than the list can be laid out again: on a device that drift was enough to
 * carry a task clean out of its own day and leave a second task displaced behind it.
 *
 * [band] is the pixel span of the days's rows and [range] their indices. The drag is clamped
 * to both, which is what stops a task being reordered onto a different date.
 */
private data class DragState(
    val original: List<Task>,
    val draggedNoteId: Long,
    val startIndex: Int,
    val startTop: Float,
    val height: Int,
    val range: IntRange,
    val band: ClosedFloatingPointRange<Float>,
    val offset: Float,
) {

    /** Where the top of the dragged row is now, never outside its day. */
    val top: Float
        get() = (startTop + offset).coerceIn(band.start, (band.endInclusive - height))

    val tasks: List<Task>
        get() = original.toMutableList().apply { add(target, removeAt(startIndex)) }

    /** The slot the row currently covers. */
    val target: Int
        get() {
            val travelled = top - startTop
            val slots = (travelled / height).roundToInt()

            return (startIndex + slots).coerceIn(range.first, range.last)
        }

    /** The dragged task's day, in its current order - what gets persisted on drop. */
    fun movedGroup(): List<Task> = tasks.slice(range)

    fun dragBy(delta: Float): DragState = copy(offset = offset + delta)

    /** How far the row would have to auto-scroll, or null when it is not near an edge. */
    fun edgeDistance(listState: LazyListState): Float? {
        val viewportTop = listState.layoutInfo.viewportStartOffset
        val viewportBottom = listState.layoutInfo.viewportEndOffset

        return when {
            top - viewportTop < AUTO_SCROLL_EDGE && listState.canScrollBackward ->
                -AUTO_SCROLL_STEP

            viewportBottom - (top + height) < AUTO_SCROLL_EDGE && listState.canScrollForward ->
                AUTO_SCROLL_STEP

            else -> null
        }
    }

    /**
     * How far to lift the row from where the list has laid it out.
     *
     * Measured against live layout rather than carried in state, so that a frame in which the
     * list has not caught up costs one frame of lag instead of a permanent drift.
     */
    fun translation(listState: LazyListState): Float {
        val laidOutAt = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == draggedNoteId }
            ?.offset
            ?: return 0f

        return top - laidOutAt
    }

    companion object {

        fun start(tasks: List<Task>, noteId: Long, listState: LazyListState): DragState? {
            val index = tasks.indexOfFirst { it.noteId == noteId }
            if (index < 0) return null

            val items = listState.layoutInfo.visibleItemsInfo
            val dragged = items.firstOrNull { it.key == noteId } ?: return null

            val range = TaskOrdering.dayRange(tasks, index)

            // Matched by key, not by LazyColumn item index: date-header rows share the same
            // item stream, so an item's index no longer equals its position in tasks.
            val firstNoteId = tasks[range.first].noteId
            val lastNoteId = tasks[range.last].noteId

            // A day taller than the screen has no visible first or last row. Falling back to
            // the dragged row itself would pin the drag to its own slot; the viewport is the
            // honest bound, and auto-scroll brings the rest of the day into reach.
            val layout = listState.layoutInfo
            val top = items.firstOrNull { it.key == firstNoteId }?.offset
                ?: layout.viewportStartOffset
            val bottom = items.firstOrNull { it.key == lastNoteId }?.let { it.offset + it.size }
                ?: layout.viewportEndOffset

            return DragState(
                original = tasks,
                draggedNoteId = noteId,
                startIndex = index,
                startTop = dragged.offset.toFloat(),
                height = dragged.size,
                range = range,
                band = top.toFloat()..bottom.toFloat(),
                offset = 0f,
            )
        }

    }
}
