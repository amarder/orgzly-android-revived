package com.orgzly.android.ui.tasks

import androidx.fragment.app.FragmentManager
import com.orgzly.R

/**
 * Fragment plumbing for the tasks screen inside MainActivity.
 *
 * Mirrors DisplayManager.displayBooks() rather than calling it: DisplayManager.replaceFragment
 * is private, and keeping our transaction here means DisplayManager.java stays untouched.
 */
object TasksDisplay {

    /**
     * Broadcast action for the drawer item.
     *
     * Deliberately not added to AppIntent.java - that file is upstream's, and every fork entry
     * there is a standing merge conflict. The value follows the same namespace so it reads
     * consistently in logs.
     */
    const val ACTION_OPEN_TASKS = "com.orgzly.intent.action.OPEN_TASKS"

    /** Extra on MainActivity's launch intent, so the launcher can open straight to tasks. */
    const val EXTRA_OPEN_TASKS = "com.orgzly.intent.extra.OPEN_TASKS"

    fun display(fragmentManager: FragmentManager, addToBackStack: Boolean) {
        // Already showing: re-adding would push a duplicate onto the back stack.
        if (fragmentManager.findFragmentByTag(TasksFragment.FRAGMENT_TAG)?.isVisible == true) {
            return
        }

        val transaction = fragmentManager
            .beginTransaction()
            .replace(R.id.single_pane_container, TasksFragment(), TasksFragment.FRAGMENT_TAG)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }
}
