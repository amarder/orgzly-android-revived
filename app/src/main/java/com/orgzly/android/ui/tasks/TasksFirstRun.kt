package com.orgzly.android.ui.tasks

import android.content.Context
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.usecase.BookImportGettingStarted
import com.orgzly.android.usecase.UseCaseWorker

/**
 * First-run work that MainActivity.performIntros() would normally do.
 *
 * Because the launcher now opens TasksActivity, performIntros() never runs on a fresh install
 * and the Getting Started notebook is never imported - leaving the task list permanently empty
 * with nothing explaining why.
 *
 * Only the import is replicated here. The version-code bump, the what's-new dialog and the
 * clipboard clear are deliberately left to MainActivity: they describe the classic UI, and
 * bumping the version code here would suppress the what's-new dialog the user never saw.
 * The import is safe to duplicate because it carries its own preference guard.
 */
object TasksFirstRun {

    fun run(context: Context) {
        if (!AppPreferences.isGettingStartedNotebookLoaded(context)) {
            UseCaseWorker.schedule(context, BookImportGettingStarted())
        }
    }
}
