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
import com.example.project_phoenix.data.TaskCategory
import com.example.project_phoenix.data.TaskClassificationRepository
import com.example.project_phoenix.data.LevelRepository
import com.example.project_phoenix.data.POINTS_PER_TASK
import java.util.Date

class TasksViewModel(
    private val repo: TaskRepository,
    private val uid: String,
    private val levelRepo: LevelRepository? = null,
    private val classifier: TaskClassificationRepository? = null
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

    fun addTask(title: String, recurring: Boolean, dueDate: Date?) {
        viewModelScope.launch {
            val category = classifier?.runCatching { classify(title) }?.getOrElse { TaskCategory.PERSONAL_SELF_CARE }
                ?: TaskCategory.PERSONAL_SELF_CARE
            repo.addTask(uid, title, recurring, category, dueDate)
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val updatedCompletion = !task.completed
            val updated = task.copy(completed = updatedCompletion)
            repo.updateTask(uid, updated)

            val deltaPoints = when {
                !task.completed && updatedCompletion -> POINTS_PER_TASK
                task.completed && !updatedCompletion -> -POINTS_PER_TASK
                else -> 0
            }

            if (deltaPoints != 0) {
                levelRepo?.applyPointDelta(uid, deltaPoints)
            }
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
