package com.example.project_phoenix.ui.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.project_phoenix.R
import com.example.project_phoenix.data.Task

class TaskAdapter(
    val tasks: MutableList<Task>,
    private val onToggle: (Task) -> Unit,
    private val onDelete: (Task) -> Unit = {}
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.taskCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(v)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.checkbox.text = task.title + if (task.recurring) " (daily)" else ""
        // avoid triggering listener during recycling
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = task.completed

        holder.checkbox.setOnCheckedChangeListener { _, _ ->
            // delegate update to the caller (fragment -> viewModel -> repo)
            onToggle(task)
        }

        // optional: long-press to delete
        holder.itemView.setOnLongClickListener {
            onDelete(task)
            true
        }
    }
}
