package com.orgzly.android.ui.tasks

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Preferences owned by the tasks UI.
 *
 * Deliberately kept out of AppPreferences: that file is upstream's and edits to it would be a
 * standing merge conflict. Keys are prefixed so they are obvious in a settings export.
 */
object TasksPrefs {

    private const val KEY_QUICK_ADD_BOOK_ID = "custom_ui_quick_add_book_id"

    private const val NO_BOOK = -1L

    /** Notebook last used for quick-add, or null if none has been chosen yet. */
    fun quickAddBookId(context: Context): Long? =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_QUICK_ADD_BOOK_ID, NO_BOOK)
            .takeIf { it != NO_BOOK }

    fun quickAddBookId(context: Context, bookId: Long) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putLong(KEY_QUICK_ADD_BOOK_ID, bookId)
            .apply()
    }
}
