package com.example.project_phoenix.ui.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_phoenix.R
import com.example.project_phoenix.data.Task
import com.example.project_phoenix.data.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateText: TextView
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val taskRepository = TaskRepository(FirebaseFirestore.getInstance())
    private val allTasks = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendarView)
        selectedDateText = view.findViewById(R.id.selectedDateText)
        tasksRecyclerView = view.findViewById(R.id.calendar_tasks_recycler_view)

        taskAdapter = TaskAdapter(mutableListOf(), { task -> // onToggle
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                lifecycleScope.launch {
                    val updatedTask = task.copy(completed = !task.completed)
                    taskRepository.updateTask(uid, updatedTask)
                }
            } else {
                Toast.makeText(requireContext(), "You must be logged in to update tasks.", Toast.LENGTH_SHORT).show()
            }
        }, { task -> // onDelete
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                lifecycleScope.launch {
                    taskRepository.deleteTask(uid, task.id)
                }
            } else {
                Toast.makeText(requireContext(), "You must be logged in to delete tasks.", Toast.LENGTH_SHORT).show()
            }
        })
        tasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        tasksRecyclerView.adapter = taskAdapter

        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val initialDate = sdf.format(calendarView.date)
        selectedDateText.text = "Selected Date: $initialDate"

        loadAllTasks()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            val selectedDate = calendar.time
            val dateString = sdf.format(selectedDate)
            selectedDateText.text = "Selected Date: $dateString"
            filterTasksByDate(selectedDate)
        }
    }

    private fun loadAllTasks() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            val activeTasksFlow = taskRepository.getTasks(uid)
            val completedTasksFlow = taskRepository.getCompletedTasks(uid)

            activeTasksFlow.combine(completedTasksFlow) { active, completed ->
                allTasks.clear()
                allTasks.addAll(active)
                allTasks.addAll(completed)
                // Initially filter for the current date
                filterTasksByDate(Date(calendarView.date))
            }.collect {}
        }
    }

    private fun filterTasksByDate(selectedDate: Date) {
        val filteredTasks = allTasks.filter {
            it.dueDate != null && isSameDay(it.dueDate, selectedDate)
        }

        val notCompletedTasks = filteredTasks.filter { !it.completed }.map { DisplayableItem.TaskItem(it) }
        val completedTasks = filteredTasks.filter { it.completed }.map { DisplayableItem.TaskItem(it) }

        val displayableItems = mutableListOf<DisplayableItem>()
        if (notCompletedTasks.isNotEmpty()) {
            displayableItems.add(DisplayableItem.Header("Not Completed"))
            displayableItems.addAll(notCompletedTasks)
        }

        if (completedTasks.isNotEmpty()) {
            displayableItems.add(DisplayableItem.Header("Completed"))
            displayableItems.addAll(completedTasks)
        }

        taskAdapter.items.clear()
        taskAdapter.items.addAll(displayableItems)
        taskAdapter.notifyDataSetChanged()
    }
}

fun isSameDay(date1: Date, date2: Date): Boolean {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return sdf.format(date1) == sdf.format(date2)
}
