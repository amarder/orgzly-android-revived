package com.orgzly.android.ui.tasks

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.ui.compose.base.ComposeFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.tasks.screen.NewTaskScreen
import com.orgzly.android.ui.tasks.screen.TasksScreen
import javax.inject.Inject

/**
 * Task-oriented view of every to-do note across all notebooks.
 *
 * A fragment inside MainActivity rather than its own activity, so that it is a peer of
 * Notebooks and the saved searches: same drawer, same back stack, no dead end.
 */
class TasksFragment : ComposeFragment(), DrawerItem {

    @Inject
    lateinit var dataRepository: DataRepository

    /** Only for the one bulk read of task order values; every other write goes through
     * DataRepository and the use cases. */
    @Inject
    lateinit var database: OrgzlyDatabase

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private lateinit var viewModel: TasksViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        App.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Built here rather than with compose's viewModel(): that lives in
        // lifecycle-viewmodel-compose, which this project does not depend on, and adding it
        // would mean another edit to build.gradle for no behavioural gain.
        viewModel = ViewModelProvider(
            this,
            TasksViewModelFactory.getInstance(
                dataRepository,
                database,
                requireContext().applicationContext,
            )
        )[TasksViewModel::class.java]

        sharedMainActivityViewModel = ViewModelProvider(requireActivity())[
            SharedMainActivityViewModel::class.java
        ]

        TasksFirstRun.run(requireContext())
    }

    override fun onStop() {
        super.onStop()

        // The undo window cannot outlive the screen: anything still pending is written now,
        // rather than silently surviving as a row the user believes they deleted.
        viewModel.commitPendingDeletes()
    }

    override fun onResume() {
        super.onResume()

        // Tells the drawer which item to check.
        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)
    }

    override fun getCurrentDrawerItemId(): String = drawerItemId

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val detail by viewModel.detail.collectAsStateWithLifecycle()
        val quickAdd by viewModel.quickAdd.collectAsStateWithLifecycle()
        val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
        val isComposingNewTask by viewModel.isComposingNewTask.collectAsStateWithLifecycle()

        if (isComposingNewTask) {
            NewTaskScreen(
                quickAdd = quickAdd,
                onCancel = viewModel::cancelNewTask,
                onCreate = viewModel::createTask,
                onSelectNotebook = viewModel::selectQuickAddNotebook,
            )
            return
        }

        TasksScreen(
            state = state,
            detail = detail,
            quickAdd = quickAdd,
            syncStatus = syncStatus,
            events = viewModel.events,
            onOpenDrawer = { sharedMainActivityViewModel.openDrawer() },
            onToggleDone = viewModel::toggleDone,
            onOpenTask = { viewModel.openDetail(it.noteId) },
            onCloseDetail = viewModel::closeDetail,
            onNewTask = viewModel::startNewTask,
            onPasteTasks = { ClipboardTasks.clipboardText(requireContext()) },
            onCreateMany = viewModel::createMany,
            onRename = viewModel::rename,
            onContentChange = viewModel::setContent,
            onSetArchived = viewModel::setArchived,
            onDelete = viewModel::requestDelete,
            onSetDate = viewModel::setScheduledDate,
            onClearDate = viewModel::clearScheduled,
            onUndoArchive = { viewModel.setArchived(it, false) },
            onUndoDelete = viewModel::undoDelete,
            onCommitDelete = viewModel::commitDelete,
            onReorder = viewModel::reorder,
            onSelectNotebook = viewModel::selectQuickAddNotebook,
        )
    }

    companion object {
        val drawerItemId: String = TasksFragment::class.java.name

        val FRAGMENT_TAG: String = TasksFragment::class.java.name
    }
}
