package com.orgzly.android.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class TasksViewModelFactory(
    private val dataRepository: DataRepository,
    private val context: Context,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TasksViewModel(dataRepository, context) as T
    }

    companion object {
        fun getInstance(
            dataRepository: DataRepository,
            context: Context,
        ): ViewModelProvider.Factory = TasksViewModelFactory(dataRepository, context)
    }
}
