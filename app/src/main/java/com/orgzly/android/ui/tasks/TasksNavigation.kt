package com.orgzly.android.ui.tasks

import android.content.Context
import android.content.Intent
import com.orgzly.android.AppIntent
import com.orgzly.android.ui.main.MainActivity

/**
 * The way out of a task row into Orgzly's full note editor, for anything the detail sheet
 * deliberately does not expose - properties, logbook, tags, priority, raw content.
 *
 * A plain intent rather than the shared compose Navigator: that routes through DisplayManager,
 * which swaps fragments into R.id.single_pane_container, and the destination here is a fresh
 * MainActivity rather than the current one.
 */
object TasksNavigation {

    /**
     * MainActivity is launchMode "standard" and reads these extras in setupDisplay(), but only
     * when savedInstanceState is null. Adding CLEAR_TOP/SINGLE_TOP would reuse the existing
     * instance and silently drop the extras, so the intent is deliberately plain.
     */
    fun openNoteInOrgzly(context: Context, bookId: Long, noteId: Long) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                putExtra(AppIntent.EXTRA_BOOK_ID, bookId)
                putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
            }
        )
    }
}
