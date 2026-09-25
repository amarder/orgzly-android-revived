package com.orgzly.android.ui.tasks

import com.orgzly.android.sync.SyncState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The button has to say one thing while up to three are true at once, so the precedence is
 * the whole design. Running wins because it is the only transient one; error beats pending
 * because a notebook that cannot be written is worse news than one waiting to be.
 */
class SyncStatusTest {

    private fun state(type: SyncState.Type) =
        SyncState.getInstance(type)

    @Test
    fun `nothing wrong and nothing waiting is in sync`() {
        assertEquals(
            SyncStatus.SYNCED,
            syncStatusOf(state(SyncState.Type.FINISHED), hasError = false, hasPending = false),
        )
    }

    @Test
    fun `never having synced is in sync, not an error`() {
        assertEquals(
            SyncStatus.SYNCED,
            syncStatusOf(null, hasError = false, hasPending = false),
        )
    }

    @Test
    fun `local changes waiting show as pending`() {
        assertEquals(
            SyncStatus.PENDING,
            syncStatusOf(state(SyncState.Type.FINISHED), hasError = false, hasPending = true),
        )
    }

    @Test
    fun `a running sync outranks pending changes`() {
        assertEquals(
            SyncStatus.SYNCING,
            syncStatusOf(state(SyncState.Type.BOOK_STARTED), hasError = true, hasPending = true),
        )
    }

    @Test
    fun `a failed sync shows as failed`() {
        assertEquals(
            SyncStatus.FAILED,
            syncStatusOf(
                state(SyncState.Type.FAILED_NO_CONNECTION),
                hasError = false,
                hasPending = false,
            ),
        )
    }

    /**
     * SyncWorker reports FINISHED even when individual notebooks failed, which is why the
     * book-level error is a separate input rather than something read off the sync state.
     */
    @Test
    fun `a notebook error beats a finished sync and beats pending`() {
        assertEquals(
            SyncStatus.FAILED,
            syncStatusOf(state(SyncState.Type.FINISHED), hasError = true, hasPending = true),
        )
    }
}
