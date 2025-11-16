package com.example.project_phoenix.viewm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_phoenix.data.Task
import com.example.project_phoenix.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TasksViewModel(
    private val repo: TaskRepository,
    private val uid: String
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    init {
        viewModelScope.launch {
            repo.getTasks(uid).collect { list ->
                _tasks.value = list
            }
        }
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            repo.addTask(uid, title)
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(completed = !task.completed)
            repo.updateTask(uid, updated)
        }
    }
}
