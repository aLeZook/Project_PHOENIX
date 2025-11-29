package com.example.project_phoenix.viewm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.project_phoenix.data.Task
import com.example.project_phoenix.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TasksViewModel(
    private val repo: TaskRepository,
    private val uid: String
) : ViewModel() {

    // ACTIVE Tasks (not completed)
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    // COMPLETED Tasks (completed = true)
    private val _completedTasks = MutableLiveData<List<Task>>()
    val completedTasks: LiveData<List<Task>> get() = _completedTasks

    init {
        viewModelScope.launch {
            repo.getTasks(uid).collect { list ->
                // separate active and completed
                _tasks.value = list.filter { !it.completed }
                _completedTasks.value = list.filter { it.completed }
            }
        }
    }

    fun addTask(title: String, recurring: Boolean) {
        viewModelScope.launch {
            repo.addTask(uid, title, recurring)
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(completed = !task.completed)
            repo.updateTask(uid, updated)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repo.deleteTask(uid, taskId)
        }
    }

    /**
     * OPTIONAL:
     * If you ever want to manually force a daily reset of recurring tasks.
     * Not required for your current feature set.
     */
    fun resetRecurringNow() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            // No-op: repo listener handles daily reset automatically if implemented there.
            repo.getTasks(uid)
        }
    }
}
