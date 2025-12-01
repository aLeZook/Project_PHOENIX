package com.example.project_phoenix.viewm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.project_phoenix.data.LevelRepository
import com.example.project_phoenix.data.TaskClassificationRepository
import com.example.project_phoenix.data.TaskRepository

class TasksViewModelFactory(
    private val repo: TaskRepository,
    private val uid: String,
    private val levelRepo: LevelRepository? = null,
    private val classifier: TaskClassificationRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(repo, uid, levelRepo, classifier) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}