package com.orgzly.android.ui.tasks

import com.orgzly.android.sync.SyncState

/**
 * What the sync button in the task list says about the data right now.
 *
 * Orgzly has no single "am I in sync?" answer, so this folds together the two it does have:
 * the state of the sync worker (process-wide, via SyncRunner) and the per-notebook flags the
 * notebook list and drawer already render.
 */
enum class SyncStatus {
    SYNCED,
    PENDING,
    SYNCING,
    FAILED,
}

/**
 * Pure so the precedence can be tested without WorkManager or a database.
 *
 * [hasPending] must come from [com.orgzly.android.db.entity.BookView.isOutOfSync], not
 * isModified: out-of-sync is "synced once and has drifted since", so a user with no repos
 * configured never sees a badge they can do nothing about.
 *
 * Error beats pending, matching BooksAdapter and DrawerNavigationView. A FINISHED sync does
 * not imply success - individual notebooks can fail inside one, which is why [hasError] is a
 * separate input rather than being read off [state].
 */
fun syncStatusOf(state: SyncState?, hasError: Boolean, hasPending: Boolean): SyncStatus = when {
    state != null && state.isRunning() -> SyncStatus.SYNCING
    hasError || (state != null && state.isFailure()) -> SyncStatus.FAILED
    hasPending -> SyncStatus.PENDING
    else -> SyncStatus.SYNCED
}
