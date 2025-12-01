package com.example.project_phoenix.ui.app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_phoenix.R
import com.example.project_phoenix.data.Task
import com.example.project_phoenix.data.TaskRepository
import com.example.project_phoenix.data.LevelRepository
import com.example.project_phoenix.viewm.TasksViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.project_phoenix.BuildConfig
import com.example.project_phoenix.data.TaskClassificationRepository
import com.google.ai.client.generativeai.GenerativeModel
import java.text.SimpleDateFormat
import java.util.*

class ChallengesFragment : Fragment() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var addTaskButton: MaterialButton
    private lateinit var adapter: TaskAdapter
    private val displayableItems = mutableListOf<DisplayableItem>()

    // Firebase & repo & viewmodel
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val uid by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private val repository by lazy { TaskRepository(db) }
    private val levelRepository by lazy { LevelRepository(db) }


    private val classifier by lazy {
        BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }?.let { key ->
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash-lite",
                apiKey = key
            )
            TaskClassificationRepository(model)
        }
    }
    private val viewModel by lazy {
        TasksViewModel(
            repo = repository,
            uid = uid,
            levelRepo = levelRepository,
            classifier = classifier   // 👈 pass it in
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_challenges, container, false)
        rootLayout = view.findViewById(R.id.root_layout)
        recyclerView = view.findViewById(R.id.tasksRecyclerView)
        addTaskButton = view.findViewById(R.id.addTaskButton)

        // ensure content sits above system bars
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom + 8)
            insets
        }

        // RecyclerView + adapter
        adapter = TaskAdapter(displayableItems,
            onToggle = { task ->
                val idx = displayableItems.indexOfFirst { it is DisplayableItem.TaskItem && it.task.id == task.id }
                if (idx >= 0) {
                    val item = displayableItems[idx] as DisplayableItem.TaskItem
                    item.task.completed = !item.task.completed
                    adapter.notifyItemChanged(idx)
                }
                viewModel.toggleTask(task.copy(completed = !task.completed))
            },
            onDelete = { task ->
                // optional deletion flow - prompt then delete via viewModel
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete task?")
                    .setMessage("Delete \"${task.title}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch { viewModel.deleteTask(task.id) }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // collect tasks from viewmodel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tasks.collectLatest { tasks ->
                val today = Calendar.getInstance()
                val todayTasks = mutableListOf<DisplayableItem.TaskItem>()
                val futureTasks = mutableListOf<DisplayableItem.TaskItem>()

                tasks.forEach { task ->
                    if (task.recurring || (task.dueDate != null && isSameDay(task.dueDate, today.time))) {
                        todayTasks.add(DisplayableItem.TaskItem(task))
                    } else {
                        futureTasks.add(DisplayableItem.TaskItem(task))
                    }
                }

                displayableItems.clear()
                if (todayTasks.isNotEmpty()) {
                    displayableItems.add(DisplayableItem.Header("Today"))
                    displayableItems.addAll(todayTasks)
                }
                if (futureTasks.isNotEmpty()) {
                    displayableItems.add(DisplayableItem.Header("Future"))
                    displayableItems.addAll(futureTasks)
                }
                adapter.notifyDataSetChanged()
            }
        }

        addTaskButton.setOnClickListener { showAddTaskDialog() }

        return view
    }

    private fun showAddTaskDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 12, 24, 12)
        }

        val titleInput = EditText(context).apply {
            hint = "Task title"
        }

        val radioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
            val oneTime = RadioButton(context).apply {
                id = View.generateViewId()
                text = "One-time (show only on selected day)"
            }
            val recurring = RadioButton(context).apply {
                id = View.generateViewId()
                text = "Recurring (daily)"
            }
            addView(oneTime)
            addView(recurring)
            oneTime.isChecked = true
        }

        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }

        val selectDateButton = Button(context).apply {
            text = "Select Date"
            setOnClickListener {
                DatePickerDialog(
                    context,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        container.addView(titleInput)
        container.addView(radioGroup)
        container.addView(selectDateButton)

        AlertDialog.Builder(context)
            .setTitle("Add Task")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                val isRecurring = (radioGroup.checkedRadioButtonId != -1)
                        && (radioGroup.findViewById<RadioButton>(radioGroup.checkedRadioButtonId).text.toString().contains("Recurring"))

                if (title.isNotBlank()) {
                    val dueDate = if (isRecurring) null else calendar.time
                    viewModel.addTask(title, isRecurring, dueDate)
                } else {
                    Toast.makeText(context, "Please enter a task title", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
