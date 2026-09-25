package com.orgzly.android.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.OrgzlyDatabase

class TasksViewModelFactory(
    private val dataRepository: DataRepository,
    private val database: OrgzlyDatabase,
    private val context: Context,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TasksViewModel(dataRepository, database, context) as T
    }

    companion object {
        fun getInstance(
            dataRepository: DataRepository,
            database: OrgzlyDatabase,
            context: Context,
        ): ViewModelProvider.Factory =
            TasksViewModelFactory(dataRepository, database, context)
    }
}
