package com.example.project_phoenix.ui.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_phoenix.R
import com.example.project_phoenix.data.Task
import com.example.project_phoenix.data.TaskRepository
import com.example.project_phoenix.viewm.TasksViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChallengesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addTaskButton: MaterialButton
    private lateinit var adapter: TaskAdapter
    private val tasksList = mutableListOf<Task>()

    // Firebase & Repository
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val repository = TaskRepository(db)

    // ViewModel
    private val viewModel by lazy { TasksViewModel(repository, uid) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_challenges, container, false)

        recyclerView = view.findViewById(R.id.tasksRecyclerView)
        addTaskButton = view.findViewById(R.id.addTaskButton)

        // Ensure button is above system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(addTaskButton) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemBars.bottom + 16
            )
            insets
        }

        // Setup RecyclerView
        adapter = TaskAdapter(tasksList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe tasks from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tasks.collectLatest { list ->
                tasksList.clear()
                tasksList.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }

        // Add task button click
        addTaskButton.setOnClickListener { showAddTaskDialog() }

        return view
    }

    private fun showAddTaskDialog() {
        val input = EditText(requireContext())
        input.hint = "Enter task title"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Task")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val title = input.text.toString()
                if (title.isNotBlank()) viewModel.addTask(title)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
