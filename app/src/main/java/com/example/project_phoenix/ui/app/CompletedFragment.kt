package com.example.project_phoenix.ui.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_phoenix.R
import com.example.project_phoenix.data.TaskRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class CompletedFragment : Fragment() {

    private lateinit var completedRecyclerView: RecyclerView
    private lateinit var completedTitle: TextView
    private lateinit var clearCompletedButton: MaterialButton
    private lateinit var adapter: CompletedAdapter
    private lateinit var taskRepository: TaskRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_completed, container, false)

        completedTitle = view.findViewById(R.id.completedTitle)
        completedRecyclerView = view.findViewById(R.id.completedRecyclerView)
        clearCompletedButton = view.findViewById(R.id.clearCompletedButton)

        completedTitle.text = "Completed Tasks"

        adapter = CompletedAdapter()
        completedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        completedRecyclerView.adapter = adapter

        taskRepository = TaskRepository(FirebaseFirestore.getInstance())
        loadCompletedTasks()

        clearCompletedButton.setOnClickListener {
            clearAllCompletedTasks()
        }

        return view
    }

    private fun loadCompletedTasks() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            taskRepository.getCompletedTasks(uid).collect { tasks ->
                adapter.submitList(tasks)
            }
        }
    }

    private fun clearAllCompletedTasks() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Delete all completed tasks in Firestore
        taskRepository.clearCompletedTasks(uid)
        // Clear the RecyclerView immediately
        adapter.submitList(emptyList())
    }

}
